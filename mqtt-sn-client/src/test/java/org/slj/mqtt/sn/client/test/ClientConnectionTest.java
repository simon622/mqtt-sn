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

package org.slj.mqtt.sn.client.test;

import org.junit.Assert;
import org.junit.Test;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.client.impl.MqttsnClient;
import org.slj.mqtt.sn.client.impl.MqttsnClientRuntimeRegistry;
import org.slj.mqtt.sn.client.impl.MqttsnClientUdpOptions;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.impl.MqttsnFilesystemStorageService;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.model.ClientState;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ClientConnectionTest {

    static final String TOPIC = "t/%s";
    static final int CONNECT_TIMEOUT = 1000;
    static final int MUTLI_CLIENT_LATCH_TIMEOUT = 240;
    static final byte[] PAYLOAD = new byte[]{0x01,0x02,0x03};

    protected MqttsnClientRuntimeRegistry createClientRuntimeRegistry(String clientId){

        MqttsnFilesystemStorageService storageService = new MqttsnFilesystemStorageService();
        MqttsnUdpOptions udpOptions = new MqttsnClientUdpOptions();
        MqttsnOptions options = new MqttsnOptions().
                withNetworkAddressEntry("gatewayId",
                        NetworkAddress.localhost(MqttsnUdpOptions.DEFAULT_LOCAL_PORT)).
                withContextId(clientId + "-" + ThreadLocalRandom.current().nextLong()).
                withMaxWait(60000).
                withPredefinedTopic("my/example/topic/1", 1);

        return (MqttsnClientRuntimeRegistry) MqttsnClientRuntimeRegistry.defaultConfiguration(storageService, options).
                withTransport(new MqttsnUdpTransport(udpOptions)).
                withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2);
    }

    @Test
    public void testClientConnection() throws IOException, MqttsnException, MqttsnClientConnectException {
        try (MqttsnClient client = new MqttsnClient()) {
            client.start(createClientRuntimeRegistry("testClientId"));
            client.connect(CONNECT_TIMEOUT, true);
            assertClientSessionState(client, ClientState.ACTIVE);
        }
    }

    @Test
    public void testClientDoubleConnection() throws IOException, MqttsnException, MqttsnClientConnectException {
        try (MqttsnClient client = new MqttsnClient()) {
            client.start(createClientRuntimeRegistry("testClientId"));
            client.connect(CONNECT_TIMEOUT, true);
            assertClientSessionState(client, ClientState.ACTIVE);
            client.connect(CONNECT_TIMEOUT, true);
            assertClientSessionState(client, ClientState.ACTIVE);
        }
    }

    @Test
    public void testClientDisconnectedAfterClose() throws IOException, MqttsnException, MqttsnClientConnectException {
        try (MqttsnClient client = new MqttsnClient()) {
            client.start(createClientRuntimeRegistry("testClientId"));
            client.connect(CONNECT_TIMEOUT, true);
            assertClientSessionState(client, ClientState.ACTIVE);
            client.disconnect();
            assertClientSessionState(client, ClientState.DISCONNECTED);
        }
    }

    @Test
    public void testClientSleep() throws IOException, MqttsnException, MqttsnClientConnectException {
        try (MqttsnClient client = new MqttsnClient()) {
            client.start(createClientRuntimeRegistry("testClientId"));
            client.connect(CONNECT_TIMEOUT, true);
            assertClientSessionState(client, ClientState.ACTIVE);
            client.sleep(CONNECT_TIMEOUT);
            assertClientSessionState(client, ClientState.ASLEEP);
        }
    }

    @Test
    public void testClientWake() throws IOException, MqttsnException, MqttsnClientConnectException {
        try (MqttsnClient client = new MqttsnClient()) {
            client.start(createClientRuntimeRegistry("testClientId"));
            client.connect(CONNECT_TIMEOUT, true);
            assertClientSessionState(client, ClientState.ACTIVE);
            client.sleep(CONNECT_TIMEOUT);
            assertClientSessionState(client, ClientState.ASLEEP);
            client.wake(15000);
            assertClientSessionState(client, ClientState.ASLEEP);
        }
    }

    @Test
    public void testClientSleepConnect() throws IOException, MqttsnException, MqttsnClientConnectException {
        try (MqttsnClient client = new MqttsnClient()) {
            client.start(createClientRuntimeRegistry("testClientId"));
            client.connect(CONNECT_TIMEOUT, true);
            assertClientSessionState(client, ClientState.ACTIVE);
            client.sleep(CONNECT_TIMEOUT);
            assertClientSessionState(client, ClientState.ASLEEP);
            client.connect(CONNECT_TIMEOUT, false);
            assertClientSessionState(client, ClientState.ACTIVE);
        }
    }

    @Test
    public void testMultipleClientConnections() throws Exception, MqttsnClientConnectException {

        final int concurrent = 5;
        final CountDownLatch latch = new CountDownLatch(concurrent);
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    final CountDownLatch localLatch = new CountDownLatch(1);
                    final byte[] payload = "hello".getBytes();
                    try (MqttsnClient client = new MqttsnClient()) {
                        client.start(createClientRuntimeRegistry("testClientId"));
                        client.registerPublishReceivedListener((context, topicName, qos, retained, data, message) -> {
                            if(Objects.deepEquals(data, payload)){
                                latch.countDown();
                                localLatch.countDown();
                            }
                        });
                        client.connect(CONNECT_TIMEOUT, true);
                        final String publishTopic = String.format(TOPIC, client.getClientId());
                        client.subscribe(publishTopic, 2);
                        assertClientSessionState(client, ClientState.ACTIVE);
                        client.publish(publishTopic,2, false,
                                payload);
                        localLatch.await(MUTLI_CLIENT_LATCH_TIMEOUT, TimeUnit.SECONDS);
                    }
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        };
        for (int i = 0; i < concurrent; i++){
            Thread t = new Thread(r);
            t.start();
        }

        Assert.assertTrue("timedout waiting for all clients", latch.await(MUTLI_CLIENT_LATCH_TIMEOUT, TimeUnit.SECONDS));
    }

    protected void assertClientSessionState(MqttsnClient client, ClientState state){
        ISession s = client.getSessionState();
        Assert.assertNotNull("session state should not be null", s);
        ClientState sessionClientState = s.getClientState();
        if(state != null){
            Assert.assertNotNull("client state should not be null", sessionClientState);
            Assert.assertEquals("client state should match", state, sessionClientState);
        } else {
            Assert.assertNull("client state should be null", s.getClientState());
        }
    }
}