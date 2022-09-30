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

import org.slj.mqtt.sn.client.impl.MqttsnClient;
import org.slj.mqtt.sn.client.impl.MqttsnClientRuntimeRegistry;
import org.slj.mqtt.sn.client.impl.MqttsnClientUdpOptions;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.net.UnknownHostException;
import java.util.UUID;

public class OpenConnectionTestMain {

    public static void main(String[] args) {

        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tc] %4$s %2$s - %5$s %6$s%n");
        int port = 2442;
//        String host = "34.248.60.25";
        String host = "localhost";
        for(int i = 0; i < 1500; i++){
            try {
                MqttsnClient client = createClient(host, port);
                client.connect(2400, true);
                client.subscribe("foo", 2);
                Thread.sleep(10);
                client.stop();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    protected static MqttsnClient createClient(String host, int port)
            throws MqttsnException, UnknownHostException {

        MqttsnUdpOptions udpOptions = new MqttsnClientUdpOptions();
        MqttsnOptions options = new MqttsnOptions().
                withNetworkAddressEntry("gatewayId", NetworkAddress.from(port, host)).
                withContextId(UUID.randomUUID().toString()).
                withMinFlushTime(200).
                withMaxWait(20000).
                withPredefinedTopic("my/predefined/example/topic/1", 1);
        AbstractMqttsnRuntimeRegistry registry = MqttsnClientRuntimeRegistry.defaultConfiguration(options).
                withTransport(new MqttsnUdpTransport(udpOptions)).
                withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2);

        MqttsnClient client = new MqttsnClient(false, false);
        client.start(registry);
        return client;
    }
}
