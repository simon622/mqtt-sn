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

package org.slj.mqtt.sn.load.impl;

import org.slj.mqtt.sn.client.impl.MqttsnClient;
import org.slj.mqtt.sn.client.impl.MqttsnClientRuntimeRegistry;
import org.slj.mqtt.sn.client.impl.MqttsnClientUdpOptions;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.load.*;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.TopicPath;

import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class MqttsnClientProfile extends AbstractExecutionProfile {

    protected volatile MqttsnClient client;
    protected String clientId;
    protected String host;
    protected int port;

    public MqttsnClientProfile() {
        clientId = UUID.randomUUID().toString();
    }

    @Override
    public String getProfileName() {
        return clientId;
    }

    protected MqttsnClient createOrGetClient()
            throws MqttsnException, UnknownHostException {

        if(client == null){
            synchronized (this){
                if(client == null){
                    MqttsnUdpOptions udpOptions = new MqttsnClientUdpOptions();
                    MqttsnOptions options = new MqttsnOptions().
                            withNetworkAddressEntry("gatewayId", NetworkAddress.from(port, host)).
                            withContextId(clientId).
                            withMaxMessagesInQueue(10000).
                            withMaxWait(20000).
                            withPredefinedTopic("my/predefined/example/topic/1", 1);
                    AbstractMqttsnRuntimeRegistry registry = MqttsnClientRuntimeRegistry.defaultConfiguration(options).
                            withTransport(new MqttsnUdpTransport(udpOptions)).
                            withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2);

                    client = new MqttsnClient(false, false);
                    client.start(registry);
                }
            }
        }

        return client;
    }

    @Override
    public ExecutionProgress initializeProfile(ExecutionInput input) {

        this.host = ((ClientInput)input).host;
        this.port = ((ClientInput)input).port;

        ExecutionProgress progress = new ExecutionProgress(input);
        return progress;
    }

    @Override
    public void shutdownProfile(){

        super.shutdownProfile();
        try {
            if(client != null){
                if(client.isConnected())
                    client.disconnect();
            }
        } catch (MqttsnException e) {
        } finally {
            try {
                if(client != null){
                    try {
                        client.close();
                    } catch(Exception e){}
                }
            } finally {
                client = null;
            }
        }
    }

    protected void bindReceiveLatch() throws UnknownHostException, MqttsnException {
        createOrGetClient().registerPublishReceivedListener((IMqttsnContext context, TopicPath topicPath, int qos, boolean retained, byte[] data, IMqttsnMessage message) -> {
            getProgress().incrementProgress(1);
        });
    }

    protected void bindSendLatch() throws UnknownHostException, MqttsnException {
        createOrGetClient().registerPublishSentListener((IMqttsnContext context, UUID messageId, TopicPath topicPath, int qos, boolean retained, byte[] data, IMqttsnMessage message) -> {
            getProgress().incrementProgress(1);
        });
    }

    protected void bindFailedLatch() throws UnknownHostException, MqttsnException {
        createOrGetClient().registerPublishFailedListener((IMqttsnContext context, UUID messageId, TopicPath topicPath, int qos, boolean retained, byte[] data, IMqttsnMessage message, int retry) -> {
            getProgress().incrementProgress(1);
        });
    }

    public ClientInput getClientInput(){
        return (ClientInput) getProgress().getInput();
    }

    public static class ClientInput extends AbstractExecutionInput {

        public String host;
        public int port;

        public String topic;
        public int qos;
        public int messageCount;

        public ClientInput(long maxWait, TimeUnit maxWaitUnit) {
            super(maxWait, maxWaitUnit);
        }
    }
}