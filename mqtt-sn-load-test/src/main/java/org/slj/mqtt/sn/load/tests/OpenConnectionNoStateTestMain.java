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

package org.slj.mqtt.sn.load.tests;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.client.impl.MqttsnClient;
import org.slj.mqtt.sn.client.impl.MqttsnClientRuntimeRegistry;
import org.slj.mqtt.sn.client.impl.MqttsnClientUdpOptions;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.spi.IMqttsnStorageService;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.wire.version1_2.Mqttsn_v1_2_MessageFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class OpenConnectionNoStateTestMain {

    public static void main(String[] args) throws Exception {

        int port = 2442;
        String host = "localhost";
        int localBindPort = 500;
        InetAddress address = InetAddress.getByName(host);
        boolean shouldSubscribe = true;

        for(int i = 0; i < 20000; i++){
            DatagramSocket socket =null;
            try {
                socket = new DatagramSocket(++localBindPort, null);
                byte[] connect = MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2.encode(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2.createMessageFactory().
                        createConnect("" + localBindPort, 3600, false, false, true, 0, 0, 0));
                DatagramPacket packet = new DatagramPacket(connect, connect.length, address, port);
                socket.send(packet);

                if(shouldSubscribe){

                    byte[] subscribeRandom = MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2.encode(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2.createMessageFactory().
                            createSubscribe(2, "/some/random/" + localBindPort));
                    packet = new DatagramPacket(subscribeRandom, subscribeRandom.length, address, port);
                    socket.send(packet);

                    byte[] subscribe = MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2.encode(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2.createMessageFactory().
                            createSubscribe(2, MqttsnConstants.TOPIC_TYPE.PREDEFINED, 1));
                    packet = new DatagramPacket(subscribe, subscribe.length, address, port);
                    socket.send(packet);
                }
            } catch(Exception e){
                e.printStackTrace();
            } finally {
                if(socket != null) socket.close();
            }
        }
    }

    protected static MqttsnClient createClient(IMqttsnStorageService storage, String host, int port)
            throws MqttsnException, UnknownHostException {

        MqttsnUdpOptions udpOptions = new MqttsnClientUdpOptions();
        MqttsnOptions options = new MqttsnOptions().
                withNetworkAddressEntry("gatewayId", NetworkAddress.from(port, host)).
                withContextId(UUID.randomUUID().toString()).
                withMinFlushTime(200).
                withMaxWait(20000).
                withPredefinedTopic("my/predefined/example/topic/1", 1);
        AbstractMqttsnRuntimeRegistry registry = MqttsnClientRuntimeRegistry.defaultConfiguration(storage, options).
                withTransport(new MqttsnUdpTransport(udpOptions)).
                withMetrics(null).
                withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2);

        MqttsnClient client = new MqttsnClient(false, false);
        client.start(registry);
        return client;
    }
}
