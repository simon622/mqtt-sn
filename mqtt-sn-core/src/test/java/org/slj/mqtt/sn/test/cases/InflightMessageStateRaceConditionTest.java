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
import org.junit.Test;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.impl.MqttsnFilesystemStorageService;
import org.slj.mqtt.sn.impl.ram.MqttsnInMemoryMessageStateService;
import org.slj.mqtt.sn.model.ClientIdentifierContext;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.InflightMessage;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.model.MqttsnWaitToken;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnOriginatingMessageSource;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.test.MqttsnTestRuntime;
import org.slj.mqtt.sn.test.MqttsnTestRuntimeRegistry;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for the race condition in MqttsnInMemoryMessageStateService where the daemon
 * thread's doWork() removes an empty context entry from the inflightMessages map,
 * causing a subsequent addInflightMessage() to write to an orphaned map that is no
 * longer reachable through inflightMessages.
 *
 * This reproduces the CI failure in SnClientIdentifierIT where:
 * 1. clearState() creates an empty entry in inflightMessages for the GW context
 * 2. The daemon's doWork() finds the empty entry and removes it via itr.remove()
 * 3. addInflightMessage() calls getInflightMessages() which returns MapA
 *    (obtained BEFORE the daemon removed the entry, or a freshly created map)
 * 4. Between getInflightMessages() returning MapA and map.put(), the daemon
 *    removes the context entry, orphaning MapA
 * 5. map.put() adds the CONNECT to the now-orphaned MapA
 * 6. The CONNACK handler calls getInflightMessages() which creates a NEW empty MapB
 * 7. MapB.containsKey(WEAK_ATTACH_ID) returns false — message lost
 *
 * @see <a href="https://github.com/simon622/mqtt-sn/issues/69">Related deadlock fix</a>
 */
public class InflightMessageStateRaceConditionTest {

    /**
     * Same constant used internally by AbstractMqttsnMessageStateService for messages
     * that don't require a packet identifier (CONNECT, DISCONNECT, PINGREQ, etc.)
     */
    static final Integer WEAK_ATTACH_ID = MqttsnConstants.UNSIGNED_MAX_16 + 1;

    private MqttsnTestRuntime runtime;

    private MqttsnTestRuntime createRuntime(String workspaceName, MqttsnOptions options) throws MqttsnException {
        MqttsnFilesystemStorageService storageService =
                new MqttsnFilesystemStorageService(workspaceName);
        MqttsnTestRuntimeRegistry registry =
                MqttsnTestRuntimeRegistry.defaultConfiguration(storageService, options, true);
        MqttsnTestRuntime rt = new MqttsnTestRuntime();
        rt.start(registry);
        return rt;
    }

    @After
    public void tearDown() {
        if (runtime != null) {
            try {
                runtime.stop();
            } catch (Exception e) {
                // best-effort
            } finally {
                runtime.close();
            }
        }
    }

    /**
     * Verifies that the inflight map reference remains stable across calls to
     * getInflightMessages() for the same context.
     *
     * Before the fix, doWork() would remove empty context entries, causing
     * getInflightMessages() to return a new (orphaned) map. The fix ensures
     * doWork() no longer removes context entries, so the map reference obtained
     * by addInflightMessage() remains the same one returned to the CONNACK handler.
     *
     * This test simulates the exact sequence from MqttsnClient.connect():
     * 1. clearInflight() creates an empty entry (like clearState)
     * 2. getInflightMessages() obtains a reference to the LOCAL map (like addInflightMessage)
     * 3. A message is added via map.put() (like addInflightMessage does)
     * 4. A fresh getInflightMessages() call (like the CONNACK handler) returns the same map
     * 5. The message is found — no orphaning occurred
     */
    @Test
    public void testMapReferenceStaysStableAcrossCalls() throws MqttsnException, MqttsnCodecException {

        runtime = createRuntime("mqtt-sn-race-test-deterministic", MqttsnTestRuntime.TEST_OPTIONS);

        IClientIdentifierContext context = new ClientIdentifierContext("testClient");
        MqttsnInMemoryMessageStateService stateService =
                (MqttsnInMemoryMessageStateService) runtime.getRegistry().getMessageStateService();

        // Step 1: clearInflight creates an empty entry (like clearState in MqttsnClient.connect)
        stateService.clearInflight(context);

        // Step 2: Obtain a reference to the LOCAL inflight map.
        // This is what addInflightMessage() does internally before map.put().
        Map<Integer, InflightMessage> mapRef =
                stateService.getInflightMessages(context, IMqttsnOriginatingMessageSource.LOCAL);

        // Step 3: Add a message to the map reference.
        // This is what addInflightMessage() does: map.put(messageId, message)
        IMqttsnMessage connectMsg = runtime.getRegistry().getCodec().createMessageFactory()
                .createConnect("testClient", 60, false, false, true, 0, 0, 0);
        InflightMessage inflight = new InflightMessage(
                connectMsg, IMqttsnOriginatingMessageSource.LOCAL, MqttsnWaitToken.from(connectMsg));
        mapRef.put(WEAK_ATTACH_ID, inflight);

        // Step 4: Look up the message through a fresh getInflightMessages() call.
        // This is what the CONNACK handler does in notifyMessageReceived().
        Map<Integer, InflightMessage> lookupMap =
                stateService.getInflightMessages(context, IMqttsnOriginatingMessageSource.LOCAL);

        // Step 5: Verify the map reference is the same object — no orphaning.
        Assert.assertSame(
                "getInflightMessages() should return the same map object for the same context",
                mapRef, lookupMap);

        Assert.assertTrue(
                "Message added via the map reference should be visible through a fresh lookup",
                lookupMap.containsKey(WEAK_ATTACH_ID));
    }

    /**
     * Concurrent test that exercises the race between the daemon's doWork() thread
     * (which removes empty entries) and the connect flow (which adds inflight messages).
     *
     * Uses a very short stateLoopTimeout to maximize the chance of the daemon running
     * during the critical window between getInflightMessages() and map.put() in
     * addInflightMessage().
     */
    @Test
    public void testConcurrentDaemonCleanupAndInflightAdd() throws Exception {

        MqttsnOptions aggressiveOptions = new MqttsnOptions().withStateLoopTimeout(1);
        runtime = createRuntime("mqtt-sn-race-test-concurrent", aggressiveOptions);

        MqttsnInMemoryMessageStateService stateService =
                (MqttsnInMemoryMessageStateService) runtime.getRegistry().getMessageStateService();

        IClientIdentifierContext context = new ClientIdentifierContext("testClient");
        AtomicInteger failureCount = new AtomicInteger(0);
        int iterations = 2000;

        for (int i = 0; i < iterations; i++) {
            // Simulate clearState() in MqttsnClient.connect()
            stateService.clearInflight(context);

            // Simulate addInflightMessage(): get map reference, then put.
            // The daemon may remove the context entry between these two operations.
            Map<Integer, InflightMessage> mapRef =
                    stateService.getInflightMessages(context, IMqttsnOriginatingMessageSource.LOCAL);

            // Yield to give the daemon a chance to run doWork() and remove the empty entry
            Thread.yield();

            IMqttsnMessage connectMsg = runtime.getRegistry().getCodec().createMessageFactory()
                    .createConnect("testClient", 60, false, false, true, 0, 0, 0);
            InflightMessage inflight = new InflightMessage(
                    connectMsg, IMqttsnOriginatingMessageSource.LOCAL, MqttsnWaitToken.from(connectMsg));
            mapRef.put(WEAK_ATTACH_ID, inflight);

            // Simulate CONNACK lookup in notifyMessageReceived()
            Map<Integer, InflightMessage> lookupMap =
                    stateService.getInflightMessages(context, IMqttsnOriginatingMessageSource.LOCAL);

            if (!lookupMap.containsKey(WEAK_ATTACH_ID)) {
                failureCount.incrementAndGet();
            }

            // Clean up for next iteration
            lookupMap.remove(WEAK_ATTACH_ID);
        }

        Assert.assertEquals(
                "Inflight message should always be findable after being added, but was lost " +
                        failureCount.get() + "/" + iterations +
                        " times due to daemon removing the context entry between " +
                        "getInflightMessages() and map.put()",
                0, failureCount.get());
    }

    /**
     * Concurrent test with separate threads for the add and lookup operations,
     * modeling the actual thread topology: main thread adds the CONNECT to inflight,
     * and the UDP ingress thread looks it up when the CONNACK arrives.
     */
    @Test
    public void testConcurrentInflightAddAndLookupFromSeparateThreads() throws Exception {

        MqttsnOptions aggressiveOptions = new MqttsnOptions().withStateLoopTimeout(1);
        runtime = createRuntime("mqtt-sn-race-test-threads", aggressiveOptions);

        MqttsnInMemoryMessageStateService stateService =
                (MqttsnInMemoryMessageStateService) runtime.getRegistry().getMessageStateService();

        IClientIdentifierContext context = new ClientIdentifierContext("testClient");
        AtomicInteger failureCount = new AtomicInteger(0);
        int iterations = 500;

        for (int i = 0; i < iterations; i++) {
            // Step 1: clearInflight (like clearState in MqttsnClient.connect)
            stateService.clearInflight(context);

            // Step 2: Add inflight message (simulating addInflightMessage's two-step operation)
            Map<Integer, InflightMessage> mapRef =
                    stateService.getInflightMessages(context, IMqttsnOriginatingMessageSource.LOCAL);

            Thread.yield();

            IMqttsnMessage connectMsg = runtime.getRegistry().getCodec().createMessageFactory()
                    .createConnect("testClient", 60, false, false, true, 0, 0, 0);
            InflightMessage inflight = new InflightMessage(
                    connectMsg, IMqttsnOriginatingMessageSource.LOCAL, MqttsnWaitToken.from(connectMsg));
            mapRef.put(WEAK_ATTACH_ID, inflight);

            // Step 3: Look up from a separate thread (simulating CONNACK on UDP ingress thread)
            CountDownLatch lookupDone = new CountDownLatch(1);

            Thread lookupThread = new Thread(() -> {
                try {
                    Map<Integer, InflightMessage> lookupMap =
                            stateService.getInflightMessages(context, IMqttsnOriginatingMessageSource.LOCAL);
                    if (!lookupMap.containsKey(WEAK_ATTACH_ID)) {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    lookupDone.countDown();
                }
            });
            lookupThread.start();

            if (!lookupDone.await(2, TimeUnit.SECONDS)) {
                Assert.fail("Lookup thread did not complete within timeout");
            }

            // Clean up for next iteration
            stateService.getInflightMessages(context, IMqttsnOriginatingMessageSource.LOCAL)
                    .remove(WEAK_ATTACH_ID);
        }

        Assert.assertEquals(
                "Inflight message should always be findable from the lookup thread, but was lost " +
                        failureCount.get() + "/" + iterations + " times",
                0, failureCount.get());
    }
}
