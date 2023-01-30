/*
 * Copyright (c) 2021 Simon Johnson <simon622 AT gmail DOT com>
 *
 * Find me on GitHub:
 * https://github.com/simon622
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.slj.mqtt.sn.utils.tree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple TriesTree implementation designed to add members at each level of the tree and normalise the storage
 */
public class MqttSubscriptionTree<T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final char split;
    private static final int DEFAULT_MAX_PATH_SIZE = 1024;
    private static final int DEFAULT_MAX_PATH_SEGMENTS = 1024;
    private static final int DEFAULT_MAX_MEMBERS_AT_LEVEL = 1024 * 10;
    private Set<String> wildcards = new HashSet<>(4);
    private Set<String> wildpaths = new HashSet<>(4);
    private boolean selfPruningTree;
    private final TrieNode<T> root;
    private long maxPathSize = DEFAULT_MAX_PATH_SIZE;
    private long maxPathSegments = DEFAULT_MAX_PATH_SEGMENTS;
    private long maxMembersAtLevel = DEFAULT_MAX_MEMBERS_AT_LEVEL;

    /**
     * Construct your tree taking the immutable configuration for the rest of the life of the tree. Using this constructor
     * will ensure the tree is simply single level (NB this is not the most efficient use of the tree - you would be better using
     * a traditional radix tree)
     *
     * @param selfPruningTree - when removing members, when a leaf is determined to be empty subsequent to the removal operation, should the
     *                        tree at that level be pruned (where it is the last level of the tree)
     */
    public MqttSubscriptionTree(final char splitChar, final boolean selfPruningTree){
        this.split = splitChar;
        this.selfPruningTree = selfPruningTree;
        this.root = new TrieNode<T>( null, null);
    }

    public long getMaxPathSize() {
        return maxPathSize;
    }

    public void setMaxPathSize(long maxPathSize) {
        this.maxPathSize = maxPathSize;
    }

    public long getMaxPathSegments() {
        return maxPathSegments;
    }

    public void setMaxPathSegments(long maxPathSegments) {
        this.maxPathSegments = maxPathSegments;
    }

    public long getMaxMembersAtLevel() {
        return maxMembersAtLevel;
    }

    public void setMaxMembersAtLevel(long maxMembersAtLevel) {
        this.maxMembersAtLevel = maxMembersAtLevel;
    }

    public void addWildcard(String wildcard){
        if(wildcard == null) throw new NullPointerException("wild card cannot be <null>");
        wildcards.add(wildcard);
    }

    public void addWildpath(String wildpath){
        if(wildpaths == null) throw new NullPointerException("wild path cannot be <null>");
        wildpaths.add(wildpath);
    }

    public void addPath(final String path, final T... members) throws MqttSubscriptionTreeLimitExceededException {

        if(path == null) throw new NullPointerException("unable to add <null> path to tree");

        if(path.length() > maxPathSize)
            throw new MqttSubscriptionTreeLimitExceededException("cannot add paths lengths exceeding the configured max '"+maxPathSize+"' - ("+path.length()+")");

        String[] segments = split(path);

        if(segments.length > maxPathSegments)
            throw new MqttSubscriptionTreeLimitExceededException("cannot add paths exceeding the configured max segments '"+maxPathSegments+"' - ("+segments.length+")");

        if(members != null && members.length > maxMembersAtLevel)
            throw new MqttSubscriptionTreeLimitExceededException("cannot add paths with the number of members exceeding max '"+maxMembersAtLevel+"'");

        TrieNode<T> node = root;
        for (int i=0; i<segments.length; i++){
            if(i == segments.length - 1){
                node = node.addChild(segments[i], members);
            } else {
                node = node.addChild(segments[i]);
            }
        }
    }

    public void removeMemberFromPath(final String path, T member){
        String[] segments = split(path);
        TrieNode<T> node = root;
        for (int i=0; i<segments.length; i++){
            node = node.getChild(segments[i]);
        }
        if(node != null){
            //if the leaf now contains no members, cut the leaf off the tree
            if(node.removeMember(member) && selfPruningTree &&
                    node.getMembers().isEmpty() && !node.hasChildren()){
                node.getParent().removeChild(node);
            }
        }
    }

    public Set<T> searchMembers(final String path){
        String[] segments = split(path);
        return searchTreeForMembers(root, segments);
    }

    public boolean hasMembers(final String path){
        TrieNode<T> node = getNodeIfExists(path);
        return node == null ? false: node.hasMembers();
    }

    protected TrieNode<T> getNodeIfExists(final String path){

        String[] segments = split(path);
        MqttSubscriptionTree.TrieNode node = root;
        for (int i=0; i < segments.length; i++){
            node = node.getChild(segments[i]);
            if(node == null) {
                return null;
            }
        }
        return node;
    }

    public boolean hasPath(String path){
        String[] segments = split(path);
        MqttSubscriptionTree.TrieNode node = root;
        boolean pathExists = true;
        for (int i=0; i < segments.length; i++){
            node = node.getChild(segments[i]);
            if(node == null) {
                pathExists = false;
                break;
            }
        }
        return pathExists;
    }

    public static void visitChildren(MqttSubscriptionTree.TrieNode node, Visitor visitor) {
        if (node != null) {
            Set<String> children = node.getChildPaths();
            Iterator<String> itr = children.iterator();
            while (itr.hasNext()) {
                String path = itr.next();
                MqttSubscriptionTree.TrieNode child = node.getChild(path);
                if (child == null) {
                    throw new RuntimeException("encountered invalid tree state");
                } else {
                    visitChildren(child, visitor);
                    visitor.visit(child);
                }
            }
        }
    }

    protected Set<T> searchTreeForMembers(TrieNode<T> node, String[] segments){

        if(node == null) throw new NullPointerException("cannot search a null node");

        Set<T> wildcardMembers = null;
        Set<T> wildSegmentMembers = null;
        Set<T> fullMembers = null;

        for (int i=0; i < segments.length; i++){
            if(!wildcards.isEmpty()){
                for (String wildcard: wildcards) {
                    TrieNode<T> wild = node.getChild(wildcard);
                    if(wild != null){
                        Set<T> wildcardMembersAtLevel = wild.getMembers();
                        if(wildcardMembersAtLevel != null && !wildcardMembersAtLevel.isEmpty()){
                            if(wildcardMembers == null) wildcardMembers = new HashSet<>();
                            wildcardMembers.addAll(wildcardMembersAtLevel);
                        }
                    }
                }
            }

            if(!wildpaths.isEmpty()){
                for (String wildpath: wildpaths) {
                    TrieNode<T> wild = node.getChild(wildpath);
                    if(wild != null){
                        String[] remainingSegments =
                                Arrays.copyOfRange(segments, i + 1, segments.length);
                        //recurse point
                        Set<T> wildSegmentMembersAtLevel = searchTreeForMembers(wild, remainingSegments);
                        if(wildSegmentMembersAtLevel != null && !wildSegmentMembersAtLevel.isEmpty()){
                            if(wildSegmentMembers == null) wildSegmentMembers = new HashSet<>();
                            wildSegmentMembers.addAll(wildSegmentMembersAtLevel);
                        }
                    }
                }
            }
            node = node.getChild(segments[i]);
            if(node == null) break;
        }

        if(node != null){
            fullMembers = node.getMembers();
        }

        //avoiding having to resize
        int size = wildcardMembers != null ? wildcardMembers.size() : 0;
        size += (wildSegmentMembers != null ? wildSegmentMembers.size() : 0);
        size += (fullMembers != null ? fullMembers.size() : 0);
        Set<T> merged = new HashSet<>(size);

        if(wildcardMembers != null &&
                !wildcardMembers.isEmpty()){
            merged.addAll(wildcardMembers);
        }

        if(wildSegmentMembers != null &&
                !wildSegmentMembers.isEmpty()){
            merged.addAll(wildSegmentMembers);
        }

        if(fullMembers != null &&
                !fullMembers.isEmpty()){
            merged.addAll(fullMembers);
        }

        return merged;
    }

    public String toTree(String lineSep){
        StringBuilder sb = new StringBuilder();
        visitChildren(root, n -> {
            if(!n.hasChildren()){
                if(sb.length() > 0){
                    sb.append(lineSep);
                }
                sb.append(n.toPath(true));
                sb.append(" (");
                sb.append(n.getMembers().size());
                sb.append(")");
            }
        });
        return sb.toString();
    }

    /**
     * Return a set of paths which are either entirely distinct or have been made distinct with memberships.
     * @param considerMembership - true if you wish to include sub paths with memberships as distinct from
     *                           the child paths thereafter.
     *                           For example, '/this/path/here' and '/this/path' with membership included at '/this/path' would be included once without
     *                           considering membership just returning '/this/path/here' or 2 dintinct entries if memberships were considered
     * @return the distinct paths
     */
    public Set<String> getDistinctPaths(boolean considerMembership){
        return getDistinctPathsFromNode(root, considerMembership);
    }

    public int countDistinctPaths(boolean considerMembership){
        return countDistinctPathsFromNode(root, considerMembership);
    }

    public int getBranchCount(){
        return root.getChildPaths().size();
    }

    protected Set<String> getDistinctPathsFromNode(MqttSubscriptionTree.TrieNode node, boolean considerMembership){
        Set<String> paths = new HashSet<>();
        visitChildren(node, n -> {
            //-- either its a leaf node or a node with children but also members
            if(n.isLeaf() || (considerMembership && n.hasMembers())){
                paths.add(n.toPath(true));
            }
        });
        return paths;
    }

    protected int countDistinctPathsFromNode(MqttSubscriptionTree.TrieNode node, boolean considerMembership){
        final AtomicInteger i = new AtomicInteger();
        visitChildren(node, n -> {
            if(n.isLeaf() || (considerMembership && n.hasMembers())){
                i.incrementAndGet();
            }
        });
        return i.get();
    }

    protected String[] split(final String path){
        return MqttSubscriptionTreeUtils.splitPathRetainingSplitChar(path, split);
    }

    class TrieNode<T> {
        private volatile Map<String, TrieNode<T>> children;
        private String pathSegment;
        private volatile Set<T> members;
        private TrieNode parent;
        private final boolean isRoot;
        private final Object memberMutex = new Object();
        private final Object childrenMutex = new Object();
        private AtomicInteger memberCount = new AtomicInteger(0);

        protected TrieNode(final TrieNode parent, final String pathSegment){
            this.parent = parent;
            this.pathSegment = pathSegment;
            this.isRoot = parent == null;
            if(!isRoot){
                if(pathSegment == null)
                    throw new IllegalArgumentException("unable to add null element child to " + parent.pathSegment);
            }
        }

        public TrieNode addChild(final String pathSegment, final T... membersIn) throws MqttSubscriptionTreeLimitExceededException {
            if(pathSegment == null) throw new IllegalArgumentException("unable to mount <null> leaf to tree");
            TrieNode child;
            if(children == null) {
                synchronized (childrenMutex) {
                    if (children == null) {
                        children = new HashMap(4);
                    }
                }
            }
            if(!children.containsKey(pathSegment)){
                synchronized (childrenMutex){
                    if(!children.containsKey(pathSegment)){
                        child = new TrieNode<>(this, pathSegment);
                        children.put(pathSegment, child);
                    } else {
                        child = getChild(pathSegment);
                    }
                }
            } else {
                child = getChild(pathSegment);
            }
            if(membersIn.length > 0) child.addMembers(membersIn);
            return child;
        }

        public TrieNode getChild(final String path){
            return children == null ? null : children.get(path);
        }

        public boolean hasChild(String path){
            return children != null && children.containsKey(path);
        }

        public boolean hasChildren(){
            return children != null && !children.isEmpty();
        }

        public boolean hasMembers(){
            return members != null && !members.isEmpty();
        }

        public TrieNode getParent(){
            return parent;
        }

        public boolean isRoot(){
            return isRoot;
        }

        public String getPathSegment(){
            return pathSegment;
        }

        public void removeChild(TrieNode node){
            if(children != null && children.containsKey(node.pathSegment)){
                synchronized (childrenMutex){
                    TrieNode removed = children.remove(node.pathSegment);
                    if(removed != node){
                        throw new RuntimeException("node removal inconsistency");
                    } else {
                        removed.clear();
                    }
                }
                node.parent = null;
            }
        }

        public  void  addMembers(T... membersIn) throws MqttSubscriptionTreeLimitExceededException {

            if(members == null && membersIn != null && membersIn.length > 0) {
                synchronized (memberMutex) {
                    if(members == null){
                        members = ConcurrentHashMap.newKeySet();
                    }
                }
            }

            if(membersIn != null && membersIn.length > 0){
                if(members.size() + membersIn.length > MqttSubscriptionTree.this.getMaxMembersAtLevel()){
                    throw new MqttSubscriptionTreeLimitExceededException("member limit exceeded at level");
                }
                for(T m : membersIn){
                    if(members.add(m)){
                        memberCount.incrementAndGet();
                    }
                }
            }
        }

        public void clear(){
            if(members != null){
                members.clear();
                members = null;
                memberCount.set(0);
            }
        }
        public boolean removeMember(T member){
            return members.remove(member);
        }

        public Set<T> getMembers(){
            if(members == null){
                return Collections.emptySet();
            } else {
                long start = System.currentTimeMillis();
                Set<T> t = Collections.unmodifiableSet(members);
                if(System.currentTimeMillis() - start > 50){
                    logger.warn("member copy operation took {} for {} members",
                            System.currentTimeMillis() - start, t.size());
                }
                return t;
            }
        }

        public Set<String> getChildPaths(){
            if(children == null){
                return Collections.emptySet();
            } else {
                synchronized (children){
                    return Collections.unmodifiableSet(children.keySet());
                }
            }
        }

        public boolean isLeaf(){
            return !hasChildren();
        }

        @Override
        public String toString() {
            return "TrieNode{" +
                    "pathSegment='" + pathSegment + '\'' +
                    ", parent=" + parent +
                    ", isRoot=" + isRoot +
                    '}';
        }


        public String toPath(boolean climb){
            if(climb){
                List<String> l = new ArrayList<>();
                TrieNode<T> leaf = this;
                while(leaf != null){
                    if(leaf.isRoot()) break;
                    l.add(leaf.pathSegment);
                    leaf = leaf.getParent();
                }
                StringBuilder sb = new StringBuilder();
                for (int i = l.size(); i-- > 0; ) {
                    sb.append(l.get(i));
                }
                return sb.toString();
            } else {
                return pathSegment;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TrieNode<?> trieNode = (TrieNode<?>) o;
            if (!pathSegment.equals(trieNode.pathSegment)) return false;
            return parent != null ? parent.equals(trieNode.parent) : trieNode.parent == null;
        }

        @Override
        public int hashCode() {
            int result = pathSegment.hashCode();
            result = 31 * result + (parent != null ? parent.hashCode() : 0);
            return result;
        }
    }
}

interface Visitor {

    void visit(MqttSubscriptionTree.TrieNode node);
}

