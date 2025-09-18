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

package org.slj.mqtt.sn.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.console.impl.MqttsnConsoleService;
import org.slj.mqtt.sn.gateway.impl.MqttsnGateway;
import org.slj.mqtt.sn.gateway.impl.MqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.gateway.impl.connector.LoopbackMqttsnConnector;
import org.slj.mqtt.sn.gateway.impl.gateway.type.MqttsnAggregatingGateway;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.impl.MqttsnFilesystemStorageService;
import org.slj.mqtt.sn.impl.MqttsnSearchableSessionRegistry;
import org.slj.mqtt.sn.impl.MqttsnVMObjectReaderWriter;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.spi.IMqttsnSessionRegistry;

public class MqttsnGatewayMain {

    protected static final Logger logger =
            LoggerFactory.getLogger(MqttsnGatewayMain.class);

    public static void main(String[] args) throws Exception {
        if(args.length < 2)
            throw new IllegalArgumentException("you must specify 2 arguments; <localPort>, <gatewayId>");

        //-- the local port on which to listen
        int localPort = Integer.valueOf(args[0].trim());

        //-- the clientId of the MQTT broker you are connecting to
        String gatewayId = args[1].trim();

        MqttsnFilesystemStorageService filesystemStorageService = new MqttsnFilesystemStorageService(
                new MqttsnVMObjectReaderWriter(), "mqtt-sn-gateway");

        //-- configure your gateway runtime
        MqttsnGatewayOptions gatewayOptions = new MqttsnGatewayOptions();
        gatewayOptions.withGatewayId(1).
                withMaxClientSessions(25_000).
                withMaxMessagesInQueue(1000).
                withContextId(gatewayId);
        gatewayOptions.withPredefinedTopic("/my/example/topic/1", 1);

        MqttsnConsoleOptions console = new MqttsnConsoleOptions().
                withConsoleEnabled(true);

        if(System.getProperty("consolePort") != null){
            try {
                console.withConsolePort(Integer.valueOf(System.getProperty("consolePort")));
            } catch (NumberFormatException e){}
        }

        logger.info("Starting gateway with console port {}", console.getConsolePort());

        IMqttsnCodec codec = MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2;
        if(System.getProperty("protocolVersion") != null){
            if(System.getProperty("protocolVersion").equals("2")){
                codec = MqttsnCodecs.MQTTSN_CODEC_VERSION_2_0;
            }
        }
        logger.info("Starting gateway with protocol version {} from {}", codec.getProtocolDescriptor().getName(), System.getProperty("protocolVersion"));

        //-- construct the registry of controllers and config
        AbstractMqttsnRuntimeRegistry registry = MqttsnGatewayRuntimeRegistry.defaultConfiguration(filesystemStorageService, gatewayOptions).
                withConnector(new LoopbackMqttsnConnector(LoopbackMqttsnConnector.DESCRIPTOR, null)).
                withBackendService(new MqttsnAggregatingGateway()).
                withServiceReplaceIfExists(IMqttsnSessionRegistry.class, new MqttsnSearchableSessionRegistry()).
                withService(new MqttsnConsoleService(console)).
                withTransport(new MqttsnUdpTransport(new MqttsnUdpOptions().withPort(localPort))).
                withCodec(codec);

        MqttsnGateway gateway = new MqttsnGateway();

        //-- start the gateway and specify if you wish to join the main gateway thread (blocking) or
        //-- specify false to run async if you are embedding
        gateway.start(registry, true);
    }
}
