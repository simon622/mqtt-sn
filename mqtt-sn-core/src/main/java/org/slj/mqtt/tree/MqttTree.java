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

package org.slj.mqtt.tree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TriesTree implementation designed to add members at each level of the tree and normalise the storage
 * retaining the separators as tree nodes per the specification.
 *
 * The tree should operate per the normative and non-normative specification statements. The one exception
 * to this is the behaviour of $SYS topics which I considered to be a function of the implementation as to
 * whether these are supported.
 */
public class MqttTree<T> implements IMqttTree<T> {

    //define static exceptions for use in high traffic
    static MqttTreeLimitExceededException TREE_MEMBER_LIMIT_EXCEEDED
            = new MqttTreeLimitExceededException("mqtt tree member limit exceeded");

    static MqttTreeLimitExceededException TREE_PATH_LIMIT_EXCEEDED
            = new MqttTreeLimitExceededException("mqtt tree path limit exceeded");

    static MqttTreeLimitExceededException TREE_PATH_LENGTH_EXCEEDED
            = new MqttTreeLimitExceededException("mqtt tree path length exceeded");

    static MqttTreeException TREE_STATE_ERROR
            = new MqttTreeException("mqtt tree state");

    static MqttTreeInputException TREE_INPUT_ERROR
            = new MqttTreeInputException("mqtt tree exception, error in input");

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    public static final char DEFAULT_SPLIT = MqttTreeConstants.PATH_SEP;
    public static final String DEFAULT_WILDCARD = MqttTreeConstants.MULTI_LEVEL_WILDCARD;
    public static final String DEFAULT_WILDPATH = MqttTreeConstants.SINGLE_LEVEL_WILDCARD;
    private static final int DEFAULT_MAX_PATH_SIZE = MqttTreeConstants.MAX_TOPIC_LENGTH;
    private static final int DEFAULT_MAX_PATH_SEGMENTS = 1024;
    private static final int DEFAULT_MAX_MEMBERS_AT_LEVEL = 1024 * 10;
    private static final int DEFAULT_ROOT_INITIALIZATION_SIZE = 256;
    private static final int DEFAULT_INITIALIZATION_SIZE = 4;
    private final boolean selfPruningTree;
    private final char split;
    private final String splitStr;
    private final TrieNode<T> root;
    private int maxPathSize = DEFAULT_MAX_PATH_SIZE;
    private int maxPathSegments = DEFAULT_MAX_PATH_SEGMENTS;
    private int maxMembersAtLevel = DEFAULT_MAX_MEMBERS_AT_LEVEL;
    private int defaultChildCountInitializationSize = DEFAULT_INITIALIZATION_SIZE;
    private int defaultRootChildCountInitializationSize = DEFAULT_ROOT_INITIALIZATION_SIZE;
    private String wildCard = DEFAULT_WILDCARD;
    private String wildPath = DEFAULT_WILDPATH;

    /**
     * An MQTT subscription tree is a tries based thread-safe tree, designed for storage of arbitrary membership data at any given level
     * in the tree.
     *
     * The tree allows you to pass in configuration according to your requirements, but traditionally this would be constructed using '/'
     * as the path seperater. Wildcard and wildpath tokens should also be supplied.
     *
     * @param selfPruningTree - when removing members, when a leaf is determined to be empty subsequent to the removal operation, should the
     *                        tree at that level be pruned (where it is the last level of the tree)
     */
    public MqttTree(final char splitChar, final boolean selfPruningTree){
        this.split = splitChar;
        this.splitStr = split + "";
        this.selfPruningTree = selfPruningTree;
        this.root = new TrieNode<T>( null, null);
    }

    public long getMaxPathSize() {
        return maxPathSize;
    }

    /**
     * Configure the max allowed path size
     * @param maxPathSize - The max. allowed path size (including separator characters) in the tree.
     * @return this
     */
    public MqttTree withMaxPathSize(int maxPathSize) {
        this.maxPathSize = maxPathSize;
        return this;
    }

    /**
     * Configure the size to initialize child map with for the '/' node
     * @param defaultRootChildCountInitializationSize
     * @return this
     */
    public MqttTree withDefaultRootInitializationSize(int defaultRootChildCountInitializationSize) {
        this.defaultRootChildCountInitializationSize = defaultRootChildCountInitializationSize;
        return this;
    }

    /**
     * Configure the size to initialize child map with all nodes bar '/'
     * @param defaultChildCountInitializationSize
     * @return this
     */
    public MqttTree withDefaultInitializationSize(int defaultChildCountInitializationSize) {
        this.defaultChildCountInitializationSize = defaultChildCountInitializationSize;
        return this;
    }

    /**
     * Configure the max allowed path segments
     * @param maxPathSegments - The max. allowed path segment count in tree. This represents how deep the tree is allowed to get. Setting a limit and then brreaching that limit will result in an exception
     *                        during use.
     * @return this
     */
    public MqttTree withMaxPathSegments(int maxPathSegments) {
        this.maxPathSegments = maxPathSegments;
        return this;
    }

    public int getMaxPathSegments() {
        return maxPathSegments;
    }

    public long getMaxMembersAtLevel() {
        return maxMembersAtLevel;
    }

    public MqttTree withMaxMembersAtLevel(int maxMembersAtLevel) {
        this.maxMembersAtLevel = maxMembersAtLevel;
        return this;
    }

    public MqttTreeNode<T> subscribe(final String path, final T... members)
            throws MqttTreeException, MqttTreeLimitExceededException {

        if(!MqttTreeUtils.isValidSubscriptionTopic(path, maxPathSize)){
            throw TREE_INPUT_ERROR;
        }

        String[] segments = split(path);

        if(segments.length > maxPathSegments)
            throw TREE_PATH_LIMIT_EXCEEDED;

        if(members != null && members.length > maxMembersAtLevel)
            throw TREE_MEMBER_LIMIT_EXCEEDED;

        TrieNode<T> node = root;
        for (int i=0; i<segments.length; i++){
            if(node == null){
                throw TREE_STATE_ERROR;
            }
            if(i == segments.length - 1){
                node = node.addChild(segments[i], members);
            } else {
                node = node.addChild(segments[i]);
            }
        }
        return node;
    }

    public boolean unsubscribe(final String path, T member) throws MqttTreeException {
        if(path == null || path.length() == 0) throw new MqttTreeException("invalid subscription path");
        TrieNode<T> node = getNodeIfExists(path);
        if(node != null){
            //if the leaf now contains no members AND its not a branch, cut the leaf off the tree
            boolean removed = node.removeMember(member);
            if(removed && selfPruningTree &&
                    node.getMembers().isEmpty() && !node.hasChildren()){
                node.getParent().removeChild(node);
            }
            return removed;
        }
        return false;
    }

    public Set<T> search(final String path)
            throws MqttTreeException {
        if(root == null || !root.hasChildren())
            return Collections.emptySet();

        return searchInternal(path);

//        return getNodeIfExists(path).getMembers();
//        String[] segments = split(path);
//        return searchTreeForMembers(root, segments);
    }

    public boolean hasMembers(final String path){
        TrieNode<T> node = getNodeIfExists(path);
        return node != null && node.hasMembers();
    }

    public boolean hasPath(String path){
        return getNodeIfExists(path) != null;
    }

    protected final TrieNode<T> getNodeIfExists(final String path){

        String[] segments = split(path);
        TrieNode node = root;
        for (int i=0; i < segments.length; i++){
            node = node.getChild(segments[i]);
            if(node == null) {
                return null;
            }
        }
        return node;
    }

    protected final Set<T> searchInternal(final String path){

        Set<T> members = new HashSet<>();
        String[] segments = split(path);
        searchChildren(root, segments, members);
        return members;
    }

    protected void searchChildren(TrieNode<T> node, String[] segments, Set<T> members){

        //root wildpath logic
        if(node.isRoot()){
            TrieNode<T> wildpath = node.getChild(DEFAULT_WILDPATH);
            if(wildpath != null){
                if(segments.length <= 1){
                    if(wildpath != null && wildpath.isLeaf()){
                        copyMembersNullSafe(members, wildpath);
                    }
                }
                //a + at root is equal to nothing at root
                searchChildren(wildpath, segments, members);

            }

        }

        //wildpath leaf logic
        if(node.isLeaf() &&
                node.isWildpath() && segments.length <= 1){
            copyMembersNullSafe(members, node);
        }

        TrieNode<T> last = null;
        for (int i=0; i < segments.length; i++){
            String currentSegment = segments[i];

            //check for wildpath segment
            if(node.isSeparator() || node.isRoot()) {
                TrieNode<T> wildpath = node.getChild(DEFAULT_WILDPATH);
                if(wildpath != null){
                    String[] from =
                            Arrays.copyOfRange(segments, i+1, segments.length);
                    if(from.length > 0){
                        searchChildren(wildpath, from, members);
                    } else {
                        //weve run out of path segments BUT we have a wildpath so we need to ensure we dont have a sub forward
                        readWildpathAtNextLevel(wildpath, members, true);
                    }
                }
            }

            //check for wildcard
            if(node.isSeparator() || node.isRoot()) {
                TrieNode<T> wildcard = node.getChild(DEFAULT_WILDCARD);
                if(wildcard != null){
                    copyMembersNullSafe(members, wildcard);
                }
            }

            node = node.getChild(currentSegment);
            if(node == null) {
                break;
            }

            last = node;
        }

        if(last != null){
            //check for wildcard at parent level
            readWildcardAtNextLevel(last, members);
        }

        //this is the direct match
        if(node != null){
            copyMembersNullSafe(members, node);
        }
    }


    protected void readWildcardAtNextLevel(TrieNode<T> node, Set<T> members){
        if(node != null) {
            if(!node.isSeparator()){
                TrieNode<T> sp = node.getChild(splitStr);
                if(sp != null && sp.isSeparator()){
                    sp = sp.getChild(DEFAULT_WILDCARD);
                    if(sp != null && sp.isLeaf()){
                        copyMembersNullSafe(members, sp);
                    }
                }
            }
        }
    }

    protected void readWildpathAtNextLevel(TrieNode<T> node, Set<T> members, boolean allowTraversal){
        if(node != null) {
            if(node.isSeparator()){
                TrieNode<T> sp = node.getChild(DEFAULT_WILDPATH);
                if(sp != null && sp.isLeaf()){
                    copyMembersNullSafe(members, sp);
                }
            } else {
                if(allowTraversal){
                    TrieNode<T> sp = node.getChild(splitStr);
                    readWildpathAtNextLevel(sp, members, false);
                }
            }
        }
    }



    protected Set<T> copyMembersNullSafe(Set<T> copyTo, MqttTreeNode<T> copyFrom){
        Set<T> members = null;
        if(copyFrom != null) {
            members = copyFrom.getMembers();
        }
        return copyMembersNullSafe(copyTo, members);
    }

    protected Set<T> copyMembersNullSafe(Set<T> copyTo, Set<T> copyFrom){
        if(copyFrom != null &&
                !copyFrom.isEmpty()){
            if (copyTo == null) copyTo = new HashSet<>();
            copyTo.addAll(copyFrom);
        }
        return copyTo;
    }

    private static void visitChildren(MqttTreeNode node, MqttTreeNodeVisitor visitor) {
        if (node != null) {
            Set<String> children = node.getChildPaths();
            Iterator<String> itr = children.iterator();
            while (itr.hasNext()) {
                String path = itr.next();
                MqttTreeNode child = node.getChild(path);
                if (child == null) {
                    throw TREE_STATE_ERROR;
                } else {
                    visitChildren(child, visitor);
                    visitor.visit(child);
                }
            }
        }
    }

    public MqttTreeNode<T> getRootNode(){
        return root;
    }

    public void visit(MqttTreeNodeVisitor visitor) {
        visit(root, visitor);
    }
    private static void visit(MqttTreeNode node, MqttTreeNodeVisitor visitor) {
        if (node != null) {
            visitor.visit(node);
            Set<String> children = node.getChildPaths();
            Iterator<String> itr = children.iterator();
            while (itr.hasNext()) {
                String path = itr.next();
                MqttTreeNode child = node.getChild(path);
                if (child == null) {
                    throw TREE_STATE_ERROR;
                } else {
                    visit(child, visitor);
                }
            }
        }
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

    protected Set<String> getDistinctPathsFromNode(TrieNode node, boolean considerMembership){
        Set<String> paths = new HashSet<>();
        visitChildren(node, n -> {
            //-- either its a leaf node or a node with children but also members
            if(n.isLeaf() || (considerMembership && n.hasMembers())){
                paths.add(n.toPath(true));
            }
        });
        return paths;
    }

    protected int countDistinctPathsFromNode(TrieNode node, boolean considerMembership){
        final AtomicInteger i = new AtomicInteger();
        visitChildren(node, n -> {
            if(n.isLeaf() || (considerMembership && n.hasMembers())){
                i.incrementAndGet();
            }
        });
        return i.get();
    }

    protected String[] split(final String path){
        return MqttTreeUtils.splitPathRetainingSplitChar(path, split, splitStr);
    }

    public class TrieNode<T> implements MqttTreeNode{
        private volatile Map<String, TrieNode<T>> children;
        private volatile Set<T> members;
        private final String pathSegment;
        private TrieNode parent;
        private final boolean isRoot;
        private final boolean isSeparator;
        private final boolean isWildcard;
        private final boolean isWildpath;
        private final Object memberMutex = new Object();
        private final Object childrenMutex = new Object();
        private final AtomicInteger memberCount = new AtomicInteger(0);

        private final int hashCode;

        protected TrieNode(final TrieNode parent, final String pathSegment){
            this.parent = parent;
            this.pathSegment = pathSegment;
            this.isRoot = parent == null;
            this.isSeparator = splitStr.equals(pathSegment);
            this.isWildcard = wildCard.equals(pathSegment);
            this.isWildpath = wildPath.equals(pathSegment);
            if(!isRoot){
                if(pathSegment == null){
                    throw TREE_INPUT_ERROR;
                }
            }

            //hash
            if(pathSegment != null){
                int result = pathSegment.hashCode();
                result = 31 * result + (parent != null ? parent.hashCode() : 0);
                hashCode = result;
            } else {
                hashCode = 0;
            }

        }

        public TrieNode addChild(final String pathSegment, final T... membersIn)
                throws MqttTreeLimitExceededException {
            if(pathSegment == null) {
                throw TREE_STATE_ERROR;
            }
            TrieNode<T> child;
            if(children == null) {
                synchronized (childrenMutex) {
                    if (children == null) {
                        //size the child map according to being '/'
                        //root has invariable 1 child '/' and '/' has many children
                        children = new HashMap<>((parent != null && parent.isRoot() && isSeparator())
                                ? defaultRootChildCountInitializationSize : defaultChildCountInitializationSize);
                    }
                }
            }

//            child = children.get(pathSegment);
//            if(child == null){
//                synchronized (childrenMutex){
//                    child = new TrieNode<>(this, pathSegment);
//                    children.put(pathSegment, child);
//                }
//            }

            synchronized (childrenMutex){
                if(!children.containsKey(pathSegment)){
                    child = new TrieNode<>(this, pathSegment);
                    children.put(pathSegment, child);
                } else {
                    child = children.get(pathSegment);
                }
            }

            if(membersIn.length > 0) child.addMembers(membersIn);
            return child;
        }

        public boolean isSeparator(){
            return isSeparator;
        }

        public boolean isWildcard() {
            return isWildcard;
        }

        public boolean isWildpath() {
            return isWildpath;
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

        public int getMemberCount(){
            return memberCount.get();
        }

        public void removeChild(TrieNode node){
            if(children != null &&
                    children.containsKey(node.pathSegment)){
                synchronized (childrenMutex){
                    TrieNode removed = children.remove(node.pathSegment);
                    if(removed == node){
                        removed.clear();
                    } else {
                        //only add clear the node if the removed node matched (it may have been recreated
                        //under high load)
                    }
                }
                node.parent = null;
            }
        }

        public void addMembers(T... membersIn) throws MqttTreeLimitExceededException {

            if(members == null && membersIn != null && membersIn.length > 0) {
                synchronized (memberMutex) {
                    if(members == null){
                        members = ConcurrentHashMap.newKeySet();
                    }
                }
            }

            if(membersIn != null && membersIn.length > 0){
                if(members.size() + membersIn.length > MqttTree.this.getMaxMembersAtLevel()){
                    throw TREE_MEMBER_LIMIT_EXCEEDED;
                }
                for(T m : membersIn){
                    if(m != null){
                        if(members.add(m)){
                            memberCount.incrementAndGet();
                        }
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
            if(members != null){
                boolean removed = members.remove(member);
                if(removed) memberCount.decrementAndGet();
                return removed;
            }
            return false;
        }

        public Set<T> getMembers(){
            if(members == null || parent == null){
                return Collections.emptySet();
            } else {
                long start = System.currentTimeMillis();
                Set<T> t = Collections.unmodifiableSet(members);
                if(System.currentTimeMillis() - start > 50){
                    logger.warn("member copy operation took {} for {} members",
                            System.currentTimeMillis() - start, t.size());
                }
                return members;
            }
        }

        public Set<String> getChildPaths(){
            if(children == null){
                return Collections.emptySet();
            } else {
                //TODO fix me
                synchronized (childrenMutex){
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
                    ", isLeaf=" + isLeaf() +
                    ", isRoot=" + isRoot +
                    ", isSeparator=" + isSeparator +
                    ", memberCount=" + memberCount +
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
            return Objects.equals(parent, trieNode.parent);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
