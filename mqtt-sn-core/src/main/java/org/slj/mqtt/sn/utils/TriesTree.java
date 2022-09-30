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

package org.slj.mqtt.sn.utils;

import java.util.*;

/**
 * Simple TriesTree implementation designed to add members at each level of the tree and normalise the storage
 * at each level
 */
public class TriesTree<T> {

    private final String pathSplitRegex;
    private Set<String> wildcards = new HashSet<>();
    private Set<String> wildpaths = new HashSet<>();
    private TrieNode<T> root = new TrieNode<T>(null, null);

    public TriesTree(final String pathSplitRegex){
        this.pathSplitRegex = pathSplitRegex;
    }

    public void addWildcard(String wildcard){
        if(wildcard == null) throw new NullPointerException("wild card cannot be <null>");
        wildcards.add(wildcard);
    }

    public void addWildpath(String wildpath){
        if(wildpaths == null) throw new NullPointerException("wild path cannot be <null>");
        wildpaths.add(wildpath);
    }

    public void addPath(final String path, final T... members){
        String[] segments = split(path);
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
            node.getMembers().remove(member);
            if(node.getMembers().isEmpty() && !node.hasChildren()){
                node.getParent().removeChild(node);
            }
        }
    }

    public Set<T> search(final String path){
        String[] segments = split(path);
        return searchTree(root, segments);
    }

    protected Set<T> searchTree(TrieNode<T> node, String[] segments){

        if(node == null) throw new NullPointerException("cannot search a null node");
        Set<T> merged = new HashSet<>();

        for (int i=0; i < segments.length; i++){
            if(!wildcards.isEmpty()){
                for (String wildcard: wildcards) {
                    TrieNode<T> wild = node.getChild(wildcard);
                    if(wild != null){
                        Set<T> members = wild.getMembers();
                        if(members != null && !members.isEmpty()){
                            merged.addAll(members);
                        }
                    }
                }
            }

            if(!wildpaths.isEmpty()){
                for (String wildpath: wildpaths) {
                    TrieNode<T> wild = node.getChild(wildpath);
                    if(wild != null){
                        String[] remainingSegments =
                                Arrays.copyOfRange(segments, i, segments.length - 1);
                        //recurse point
                        Set<T> wildMembers = searchTree(wild, remainingSegments);
                        if(wildMembers != null && !wildMembers.isEmpty()){
                            merged.addAll(wildMembers);
                        }
                    }
                }
            }
            node = node.getChild(segments[i]);
            if(node == null) break;
        }

        if(node != null){
            Set<T> members = node.getMembers();
            if(!members.isEmpty()){
                merged.addAll(members);
            }
        }
        return merged;
    }

    protected String[] split(final String path){
        return path.split(pathSplitRegex);
    }
}

class TrieNode<T> {

    private Map<String, TrieNode<T>> children = new HashMap<>();
    private String pathSegment;
    private volatile Set<T> members;
    private TrieNode parent;

    public TrieNode(final TrieNode parent, final String pathSegment){
        this.parent = parent;
        this.pathSegment = pathSegment;
    }

    public TrieNode addChild(final String pathSegment, final T... membersIn){
        if(pathSegment == null) throw new IllegalArgumentException("unable to mount <null> leaf to tree");
        TrieNode child = null;
        if(!children.containsKey(pathSegment)){
            synchronized (this){
                if(!children.containsKey(pathSegment)){
                    child = new TrieNode<>(this, pathSegment);
                    children.put(pathSegment, child);
                }
            }
        } else {
            child = getChild(pathSegment);
        }
        child.addMembers(membersIn);
        return child;
    }

    public TrieNode getChild(final String path){
        return children.get(path);
    }

    public boolean hasChild(String path){
        return children.containsKey(path);
    }

    public boolean hasChildren(){
        return !children.isEmpty();
    }

    public TrieNode getParent(){
        return parent;
    }

    public void removeChild(TrieNode node){
        if(children.containsKey(node.pathSegment)){
            synchronized (children){
                TrieNode removed = children.remove(node.pathSegment);
                if(removed != node){
                    throw new RuntimeException("node removal inconsistency");
                }
            }
            node.parent = null;
        }
    }

    public void addMembers(T... membersIn){
        if(members == null && membersIn != null) {
            synchronized (this) {
                if(members == null)
                    members = new HashSet<>();
            }
        }

        if(membersIn != null)
            members.addAll(Arrays.asList(membersIn));
    }

    public Set<T> getMembers(){
        synchronized (this){
            return new HashSet<>(members);
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