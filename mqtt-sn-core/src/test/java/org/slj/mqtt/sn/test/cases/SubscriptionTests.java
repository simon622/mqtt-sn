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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.spi.IMqttsnSubscriptionRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnIllegalFormatException;
import org.slj.mqtt.sn.test.MqttsnTestRuntime;
import org.slj.mqtt.sn.test.MqttsnTestRuntimeRegistry;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class SubscriptionTests {

    final static String TEST_TOPIC = "test/topic";
    final static String TEST_SINGLE_WILDCARD_TOPIC = "test/+/topic";
    final static String TEST_MULTI_WILDCARD_TOPIC = "test/#";

    private MqttsnTestRuntime runtime;

    @Before
    public void setup() throws MqttsnException {
        runtime = new MqttsnTestRuntime();
        MqttsnTestRuntimeRegistry registry =
                MqttsnTestRuntimeRegistry.defaultConfiguration(MqttsnTestRuntime.TEST_OPTIONS, false);
        runtime.start(registry);
    }

    @After
    public void tearDown() throws MqttsnException, IOException {
        try {
            runtime.stop();
        } finally {
            runtime.close();
        }
    }

    @Test
    public void testSimpleSubscription() throws MqttsnException, MqttsnIllegalFormatException {

        IMqttsnSession session = createConfirmedTestSession(MqttsnTestRuntime.TEST_CLIENT_ID, 1);

        IMqttsnSubscriptionRegistry subscriptionRegistry = runtime.getRegistry().getSubscriptionRegistry();
        Assert.assertTrue("new subscription should be added",
                subscriptionRegistry.subscribe(session, TEST_TOPIC, 1));

        Assert.assertTrue("subscription should exist",
                subscriptionRegistry.readSubscriptions(session).size() == 1);

        Assert.assertEquals("subscription should exist at QoS 1", 1,
                subscriptionRegistry.getQos(session, TEST_TOPIC));

        Assert.assertFalse("same subscription should not be added",
                subscriptionRegistry.subscribe(session, TEST_TOPIC, 2));

        Assert.assertEquals("subscription should exist at QoS 2", 2,
                subscriptionRegistry.getQos(session, TEST_TOPIC));

        Assert.assertEquals("subscription should exist at QoS 2", 1,
                subscriptionRegistry.matches(TEST_TOPIC).size());
    }

    @Test
    public void testMultiWildcardSubscriptionMatching() throws MqttsnException, MqttsnIllegalFormatException {

        IMqttsnSession session = createConfirmedTestSession(MqttsnTestRuntime.TEST_CLIENT_ID, 1);

        IMqttsnSubscriptionRegistry subscriptionRegistry = runtime.getRegistry().getSubscriptionRegistry();
        Assert.assertTrue("new subscription should be added",
                subscriptionRegistry.subscribe(session, TEST_MULTI_WILDCARD_TOPIC, 1));

        Assert.assertEquals("subscription should match from multi-wildcard", 1,
                subscriptionRegistry.matches(TEST_TOPIC).size());
    }

    @Test
    public void testConcurrentAccessSubscriptionManipulated() throws MqttsnException, MqttsnIllegalFormatException {

        int THREADS = 5;
        final CountDownLatch latch = new CountDownLatch(THREADS);
        final Object mon = new Object();

        for (int i = 0; i < THREADS; i++){

            System.err.println("starting thread " + (i + 1));

            Thread t = new Thread(() -> {
                String clientId = null;
                try {

                    clientId = MqttsnTestRuntime.TEST_CLIENT_ID +
                            ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);

                    System.err.println(clientId + " is starting");

                    IMqttsnSession session = createConfirmedTestSession(clientId, 1);
                    final IMqttsnSubscriptionRegistry subscriptionRegistry = runtime.getRegistry().getSubscriptionRegistry();

                    //-- add common subscription
                    Assert.assertTrue("new subscription should be added",
                            subscriptionRegistry.subscribe(session, TEST_TOPIC, 1));

                    String customTopicFilter = TEST_TOPIC + "/" + session.getContext().getId();

                    //-- add custom subscription
                    Assert.assertTrue("new subscription should be added",
                            subscriptionRegistry.subscribe(session, customTopicFilter, 1));

                    //-- search common subscription
                    Assert.assertTrue("subscription search for shared filter should match multiple",
                            subscriptionRegistry.matches(TEST_TOPIC).size() >= 1);

                    //-- search custom subscription
                    Assert.assertEquals("subscription search for custom filter should return 1", 1,
                            subscriptionRegistry.matches(customTopicFilter).size());

                    //-- remove custom subscription
                    Assert.assertTrue("subscription should have been removed",
                            subscriptionRegistry.unsubscribe(session, customTopicFilter));

                    //-- search custom subscription
                    Assert.assertEquals("subscription search for custom filter should return 0", 0,
                            subscriptionRegistry.matches(customTopicFilter).size());

                    subscriptionRegistry.readAllSubscribedTopicPaths();

                    latch.countDown();

                } catch(Exception e){
                    throw new RuntimeException(e);
                } finally {
                    try {
                        System.err.println("im done " + clientId);
                    } catch(Exception e){
                        Assert.fail("error closing runtime " + e.getMessage());
                    }
                }
            });
            t.start();
        }

        try {
            if(!latch.await(5, TimeUnit.SECONDS)){
                Assert.fail("error waiting on latch");
            }
        } catch(Exception e){
            Assert.fail("latch failure");
        }
    }

    @Test
    public void testSingleWildcardSubscriptionMatching() throws MqttsnException, MqttsnIllegalFormatException {

        IMqttsnSession session = createConfirmedTestSession(MqttsnTestRuntime.TEST_CLIENT_ID, 1);

        IMqttsnSubscriptionRegistry subscriptionRegistry = runtime.getRegistry().getSubscriptionRegistry();
        Assert.assertTrue("new subscription should be added",
                subscriptionRegistry.subscribe(session, "sport/tennis/+", 1));

        Assert.assertEquals("subscription should match from single-wildcard", 1,
                subscriptionRegistry.matches("sport/tennis/player1").size());
    }

    @Test
    public void testNormativeValidTopicRules() throws MqttsnException {

        //valid topics
        String[] topics = new String[] {
                "#", "sport/tennis/#", "+", "+/tennis/#", "sport/+/player1", "+/+", "/+", "/"
        };

        IMqttsnSession session = createConfirmedTestSession(MqttsnTestRuntime.TEST_CLIENT_ID, 1);
        IMqttsnSubscriptionRegistry subscriptionRegistry = runtime.getRegistry().getSubscriptionRegistry();

        for (int i=0;i<topics.length;i++){
            try {
                subscriptionRegistry.subscribe(session, topics[i], 1);
            } catch(MqttsnIllegalFormatException e){
                Assert.fail(String.format("topic '%s' should not have caused subscription to fail", topics[i]));
            }
        }
    }

    @Test
    public void testNormativeInvalidTopicRules() throws MqttsnException {

        //invalid topics
        String[] invalidTopics = new String[] {
                "sport/tennis#", "sport/tennis/#/ranking", "sport+", "", "unicode/null/" + MqttsnConstants.UNICODE_ZERO + "/is/illegal"
        };

        IMqttsnSession session = createConfirmedTestSession(MqttsnTestRuntime.TEST_CLIENT_ID, 1);
        IMqttsnSubscriptionRegistry subscriptionRegistry = runtime.getRegistry().getSubscriptionRegistry();

        for (int i=0;i<invalidTopics.length;i++){
            try {
                subscriptionRegistry.subscribe(session, invalidTopics[i], 1);
                Assert.fail(String.format("invalid topic '%s' should have caused subscription to fail", invalidTopics[i]));
            } catch(MqttsnIllegalFormatException e){
                //this is expected
            }
        }
    }

    @Test
    public void testValidMultiWildcardSubscription() throws MqttsnIllegalFormatException, MqttsnException {

        IMqttsnSession session = createConfirmedTestSession(MqttsnTestRuntime.TEST_CLIENT_ID, 1);
        IMqttsnSubscriptionRegistry subscriptionRegistry = runtime.getRegistry().getSubscriptionRegistry();
        subscriptionRegistry.subscribe(session, TEST_MULTI_WILDCARD_TOPIC, 1);
    }

    @Test
    public void testValidSingleWildcardSubscription() throws MqttsnIllegalFormatException, MqttsnException {

        IMqttsnSession session = createConfirmedTestSession(MqttsnTestRuntime.TEST_CLIENT_ID, 1);
        IMqttsnSubscriptionRegistry subscriptionRegistry = runtime.getRegistry().getSubscriptionRegistry();
        subscriptionRegistry.subscribe(session, TEST_SINGLE_WILDCARD_TOPIC, 1);
    }

    public IMqttsnSession createConfirmedTestSession(String clientId, int protocolVersion)
            throws MqttsnException {

        IMqttsnContext context = runtime.getRegistry().getContextFactory().createInitialApplicationContext(
                createUnconfirmedTestContext(), clientId, protocolVersion);
        IMqttsnSession session = runtime.getRegistry().getSessionRegistry().getSession(context, true);
        return session;
    }

    public INetworkContext createUnconfirmedTestContext()
            throws MqttsnException {

        return runtime.getRegistry().getContextFactory().
                createInitialNetworkContext(MqttsnTestRuntime.TEST_ADDRESS);
    }


//    @Test
//    public void testManySubscriptions() throws MqttsnException, MqttsnIllegalFormatException {
//
//        IMqttsnSession session = createConfirmedTestSession(MqttsnTestRuntime.TEST_CLIENT_ID, 1);
//        IMqttsnSubscriptionRegistry subscriptionRegistry = runtime.getRegistry().getSubscriptionRegistry();
//
//        Set<Integer> randoms = new HashSet<>();
//        for(int i = 0; i < 100000; i++){
//           int random = ThreadLocalRandom.current().nextInt();
//           randoms.add(random);
//           subscriptionRegistry.subscribe(session,
//                   getTopic(random), 1);
//       }
//
//        randoms.stream().forEach(i -> {
//            try {
//                Assert.assertTrue("subscription should match at least 1",
//                        subscriptionRegistry.matches(getTopic(i)).size() > 0);
//            } catch(Exception e){
//                e.printStackTrace();
//            }
//        });
//
//
//    }

//    private static String getTopic(int i){
//        return "test/subscribe/" + i + "/leaf";
//    }
}
