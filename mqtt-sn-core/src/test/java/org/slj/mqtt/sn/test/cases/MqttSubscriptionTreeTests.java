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

package org.slj.mqtt.sn.test.cases;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.utils.tree.MqttSubscriptionTreeLimitExceededException;
import org.slj.mqtt.sn.utils.tree.MqttSubscriptionTree;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class MqttSubscriptionTreeTests {

    @Before
    public void setup(){
    }

    @Test
    public void testReadAllFromTree() throws MqttSubscriptionTreeLimitExceededException {
        MqttSubscriptionTree<String> tree = createTreeDefaultConfig();
        int BRANCHES = 50;
        Set<String> added = new HashSet<>();
        for (int i = 0; i < BRANCHES; i++){
            int depth = ThreadLocalRandom.current().nextInt(1, 10);
            String topic = generateRandomTopic(depth);
            tree.addPath(topic, "foo");
            added.add(topic);
        }
        Set<String> all = tree.getDistinctPaths(true);
        Assert.assertEquals("all distinct topics should be returned by match", added.size(), all.size());
        all.removeAll(added);
        Assert.assertEquals("all topics should exist in both sets", 0, all.size());
    }

    @Test(expected = MqttSubscriptionTreeLimitExceededException.class)
    public void testLargeTopicExceedsMaxSegments() throws MqttSubscriptionTreeLimitExceededException {
        MqttSubscriptionTree<String> tree = createTreeDefaultConfig();
        String topic = generateRandomTopic((int) tree.getMaxPathSegments() + 1);
        tree.addPath(topic, "foo");
    }

    @Test(expected = MqttSubscriptionTreeLimitExceededException.class)
    public void testLargeTopicExceedsMaxLength() throws MqttSubscriptionTreeLimitExceededException {
        MqttSubscriptionTree<String> tree = createTreeDefaultConfig();
        String topic = generateTopicMaxLength((int) tree.getMaxPathSize() + 1);
        tree.addPath(topic, "foo");
    }

    @Test(expected = MqttSubscriptionTreeLimitExceededException.class)
    public void testLargeTopicExceedsMaxMembers() throws MqttSubscriptionTreeLimitExceededException {
        MqttSubscriptionTree<String> tree = createTreeDefaultConfig();
        String topic = generateRandomTopic(10);
        String[] members = new String[(int) tree.getMaxMembersAtLevel() + 1];
        Arrays.fill(members, UUID.randomUUID().toString());
        tree.addPath(topic, members);
    }

//    @Test
    public void testConcurrency() throws Exception {

        MqttSubscriptionTree<String> tree = new MqttSubscriptionTree<>(MqttsnConstants.PATH_SEP, true);
        tree.addWildcard("#");
        tree.addWildpath("+");
        tree.setMaxMembersAtLevel(1000000);

        int loops = 100;
        int threads = 50;
        CountDownLatch latch = new CountDownLatch(loops * threads);
        final long start = System.currentTimeMillis();
        AtomicInteger c = new AtomicInteger();
        AtomicInteger subCount = new AtomicInteger();

        for(int t = 0;  t < threads; t++){
            Thread tt = new Thread(() -> {
                for (int i = 0; i < loops; i++){
                    try {
                        String subscriberId = ""+c.incrementAndGet();
                        if(i % 2 == 0){
                            tree.addPath("some/topic/1",subscriberId);
                            tree.addPath("some/topic/2",subscriberId);
                            tree.addPath("some/topic/3",subscriberId);
                            tree.addPath("some/+/2",""+c.incrementAndGet());
                            tree.addPath("#",""+c.incrementAndGet());
                            subCount.getAndAdd(5);
                        } else {
                            for(int r = 0; r < 1000; r++){
                                long start1 = System.currentTimeMillis();
                                Set<?> s = tree.searchMembers("some/topic/1");
                                if(System.currentTimeMillis() - start1 > 100){
                                    System.err.println("read took " + (System.currentTimeMillis() - start1) + "ms for " + s.size());
                                }
                            }
                        }

                    } catch(Exception e){
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                        long cl = subCount.get();
                        if(cl % 100 == 0){
                            System.out.println("added " + cl + " subscribers in total in " + (System.currentTimeMillis() - start) + "ms");
                        }
                    }
                }
            });
            tt.start();
        }

        latch.await();

        System.err.println("write took " + (System.currentTimeMillis() - start) + "ms");

        long quickstart = System.currentTimeMillis();
        Set<String> s = tree.searchMembers("some/topic/2");
        System.err.println("path had " + s.size() + " subscribers in " + (System.currentTimeMillis() - quickstart));
        Assert.assertEquals("member output should match",7500, s.size());
    }

    @Test
    public void testTopLevelTokenMatch() throws MqttSubscriptionTreeLimitExceededException {

        MqttSubscriptionTree<String> tree = createTreeDefaultConfig();
        tree.addPath("/", "foo");
        Assert.assertEquals("first level is a token", 1, tree.searchMembers("/").size());
    }

    @Test
    public void testTopLevelPrefixTokenMatchDistinct() throws MqttSubscriptionTreeLimitExceededException {

        MqttSubscriptionTree<String> tree = createTreeDefaultConfig();
        tree.addPath("/foo", "foo"); //different things
        tree.addPath("foo", "bar");
        System.err.println(tree.toTree(System.lineSeparator()));
        Assert.assertEquals("should be 2 distinct branches", 2, tree.getBranchCount());
        Assert.assertEquals("top level path sep is distinct from none", 1, tree.searchMembers("/foo").size());
        Assert.assertEquals("top level path sep is distinct from none", 1, tree.searchMembers("foo").size());
    }

    @Test
    public void testTopLevelSuffixTokenMatchDistinct() throws MqttSubscriptionTreeLimitExceededException {

        MqttSubscriptionTree<String> tree = createTreeDefaultConfig();
        tree.addPath("foo/", "foo"); //different things
        tree.addPath("foo", "bar");
        System.err.println(tree.toTree(System.lineSeparator()));
        Assert.assertEquals("should be 2 distinct branches", 1, tree.getBranchCount());
        Assert.assertEquals("top level path sep is distinct from none", 1, tree.searchMembers("foo/").size());
        Assert.assertEquals("top level path sep is distinct from none", 1, tree.searchMembers("foo").size());
        Assert.assertEquals("top level path sep is distinct from none", 0, tree.searchMembers("/foo").size());
    }

    @Test
    public void testWildcard() throws MqttSubscriptionTreeLimitExceededException {

        MqttSubscriptionTree<String> tree = createTreeDefaultConfig();
        tree.addPath("foo/bar/#", "foo");
        tree.addPath("foo/#", "bar");
        tree.addPath("/#", "foo1");
        tree.addPath("#", "root");
        System.err.println(tree.toTree(System.lineSeparator()));
        Assert.assertEquals("should be 1 distinct branches", 3, tree.getBranchCount());
        Assert.assertEquals("wildcard should match", 3, tree.searchMembers("foo/bar/is/me").size());
        Assert.assertEquals("wildcard should match", 2, tree.searchMembers("/foo/bar/is/you").size());
        Assert.assertEquals("wildcard should match", 2, tree.searchMembers("foo/bar").size());
        Assert.assertEquals("wildcard should match", 1, tree.searchMembers("moo/bar").size());
    }

    @Test
    public void testWildpath() throws MqttSubscriptionTreeLimitExceededException {

        MqttSubscriptionTree<String> tree = createTreeDefaultConfig();
        tree.addPath("foo/+/is/good", "foo");
        System.err.println(tree.toTree(System.lineSeparator()));
        Assert.assertEquals("should be 1 distinct branches", 1, tree.getBranchCount());
        Assert.assertEquals("wildcard should match", 0, tree.searchMembers("foo/bar").size());
        Assert.assertEquals("wildcard should match", 1, tree.searchMembers("foo/mar/is/good").size());
        Assert.assertEquals("wildcard should match", 1, tree.searchMembers("foo/bar/is/good").size());
        Assert.assertEquals("wildcard should match", 1, tree.searchMembers("foo/ /is/good").size());
        Assert.assertEquals("wildcard should match", 0, tree.searchMembers("foo/bar/is/good/or/bad").size());
        Assert.assertEquals("wildcard should match", 0, tree.searchMembers("foo/bar/is/bad").size());
        Assert.assertEquals("wildcard should match", 0, tree.searchMembers("/foo/bar/is/good").size());
        Assert.assertEquals("wildcard should match", 0, tree.searchMembers("foo/bar/is/bad").size());
    }

    @Test
    public void testWildcardsGetRolledUp() throws MqttSubscriptionTreeLimitExceededException {

        MqttSubscriptionTree<String> tree = createTreeDefaultConfig();

        tree.addPath("foo/#", "foo");
        tree.addPath("foo/bar", "foo1");
        tree.addPath("foo/bar/foo", "foo2");
        tree.addPath("foo/bar/zoo", "foo3");
        tree.addPath("foo/bar/#", "foo4");
        System.err.println(tree.toTree(System.lineSeparator()));

        Assert.assertEquals("should be 1 distinct branches", 1, tree.getBranchCount());
        Assert.assertEquals("wildcard should match", 3, tree.searchMembers("foo/bar/zoo").size());
    }

    @Test
    public void testPathExistenceInBigTree() throws MqttSubscriptionTreeLimitExceededException, InterruptedException {

        MqttSubscriptionTree<Integer> tree = new MqttSubscriptionTree<>(MqttsnConstants.PATH_SEP, true);
        String search = "some/member";
        String searchNoMem = "/some/member";

        //-- rememeber the / is a token
        tree.setMaxPathSegments(tree.getMaxPathSegments() * 2 + 1);
        tree.setMaxPathSize(1024 * 4);

        for (int i = 0; i < 200_000_0; i++){
            String topic = generateRandomTopic(5);
            if(i % 2 == 0){
                tree.addPath(topic,
                        ThreadLocalRandom.current().nextInt(0, 1000));
            } else {
                tree.addPath(topic);
            }
        }

        tree.addPath(search,
                ThreadLocalRandom.current().nextInt(0, 1000));
        tree.addPath(searchNoMem);

//        System.err.println(tree.toTree(System.lineSeparator()));

        Assert.assertTrue("this path should exist", tree.hasPath(search));
        Assert.assertTrue("this path should exist", tree.hasPath(searchNoMem));

        Assert.assertTrue("this path should have members", tree.hasMembers(search));
        Assert.assertFalse("this path should not have members", tree.hasMembers(searchNoMem));

        Assert.assertFalse("this path should not not exist", tree.hasPath("/doesnt/exits"));
        Assert.assertFalse("this path should not not exist nor have members", tree.hasMembers("/doesnt/exits"));

        Assert.assertEquals("path count should match", 200_000_2, tree.countDistinctPaths(false));
    }

    public static String generateRandomTopic(int segments){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments; i++){
            if(i == 0) sb.append("/");
            int r = ThreadLocalRandom.current().nextInt(0, 1000);
            sb.append(r);
            sb.append("/");
        }
        return sb.toString();
    }

    public static String generateTopicMaxLength(int length){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++){
            sb.append("a");
        }
        return sb.toString();
    }

    protected static MqttSubscriptionTree<String> createTreeDefaultConfig(){
        MqttSubscriptionTree<String> tree = new MqttSubscriptionTree<>(MqttsnConstants.PATH_SEP, true);
        tree.addWildcard("#");
        tree.addWildpath("+");
        tree.setMaxMembersAtLevel(1000000);
        return tree;
    }
}
