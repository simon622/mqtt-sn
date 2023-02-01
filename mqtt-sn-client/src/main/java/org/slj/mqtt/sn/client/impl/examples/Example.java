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

package org.slj.mqtt.sn.client.impl.examples;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.client.impl.MqttsnClient;
import org.slj.mqtt.sn.client.impl.MqttsnClientRuntimeRegistry;
import org.slj.mqtt.sn.client.impl.MqttsnClientUdpOptions;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.impl.MqttsnFilesystemStorageService;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Example {
    public static void main(String[] args) throws Exception {

        MqttsnFilesystemStorageService filesystemStorageService =
                new MqttsnFilesystemStorageService("mqtt-sn-client-example");

        //-- use the client transport options, which will use random unallocated local ports
        MqttsnUdpOptions udpOptions = new MqttsnClientUdpOptions();

        //-- runtimes options can be used to tune the behaviour of the client
        MqttsnOptions options = new MqttsnOptions().
                //-- specify the address of any static gateway nominating a context id for it
                withNetworkAddressEntry("gatewayId", NetworkAddress.localhost(MqttsnUdpOptions.DEFAULT_LOCAL_PORT)).
                //-- configure your clientId
                withContextId("clientId1").
                //-- specify and predefined topic Ids that the gateway will know about
                withPredefinedTopic("my/predefined/example/topic/1", 1);

        //-- using a default configuration for the controllers will just work out of the box, alternatively
        //-- you can supply your own implementations to change underlying storage or business logic as is required
        AbstractMqttsnRuntimeRegistry registry = MqttsnClientRuntimeRegistry.defaultConfiguration(filesystemStorageService, options).
                withTransport(new MqttsnUdpTransport(udpOptions)).
                //-- select the codec you wish to use, support for SN 1.2 is standard or you can nominate your own
                withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2);

        AtomicInteger receiveCounter = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);

        //-- the client is Closeable and so use a try with resource
        try (MqttsnClient client = new MqttsnClient()) {

            //-- the client needs to be started using the configuration you constructed above
            client.start(registry);

            //-- register any publish receive listeners you require
            client.registerPublishReceivedListener((IClientIdentifierContext context, TopicPath topic, int qos, boolean retained, byte[] data, IMqttsnMessage message) -> {
                receiveCounter.incrementAndGet();
                System.err.println(String.format("received message [%s] [%s]",
                        receiveCounter.get(), new String(data, MqttsnConstants.CHARSET)));
                latch.countDown();
            });

            //-- register any publish sent listeners you require
            client.registerPublishSentListener((IClientIdentifierContext context, TopicPath topic, int qos, boolean retained, byte[] data, IMqttsnMessage message) -> {
                System.err.println(String.format("sent message [%s]",
                        new String(data, MqttsnConstants.CHARSET)));
            });



            //-- issue a connect command - the method will block until completion
            client.connect(360, true);

            //-- issue a subscribe command - the method will block until completion
            client.subscribe("my/example/topic/1", 2);

            //-- issue a publish command - the method will queue the message for sending and return immediately
            client.publish("my/example/topic/1", 1,  false, "hello world".getBytes());

            //-- wait for the sent message to be looped back before closing
            latch.await(30, TimeUnit.SECONDS);

            //-- issue a disconnect command - the method will block until completion
            client.disconnect();
        }
    }
}
