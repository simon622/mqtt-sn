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

package org.slj.mqtt.sn.gateway.impl;

import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.gateway.cli.MqttsnInteractiveGateway;
import org.slj.mqtt.sn.gateway.cli.MqttsnInteractiveGatewayLauncher;
import org.slj.mqtt.sn.gateway.impl.broker.MqttsnAggregatingBrokerService;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBrokerOptions;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.spi.IMqttsnTransport;

public class AggregatingGatewayInteractiveMain {
    public static void main(String[] args) throws Exception {
        MqttsnInteractiveGatewayLauncher.launch(new MqttsnInteractiveGateway() {
            protected AbstractMqttsnRuntimeRegistry createRuntimeRegistry(MqttsnOptions options, IMqttsnTransport transport) {

                MqttsnBrokerOptions brokerOptions = new MqttsnBrokerOptions().
                        withHost(hostName).
                        withPort(port).
                        withUsername(username).
                        withPassword(password);

                return MqttsnGatewayRuntimeRegistry.defaultConfiguration(options).
                        withBrokerConnectionFactory(new PahoMqttsnBrokerConnectionFactory()).
                        withBrokerService(new MqttsnAggregatingBrokerService(brokerOptions)).
                        withTransport(createTransport()).
                        withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2);
            }
        });
    }
}
