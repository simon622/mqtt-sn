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

package org.slj.mqtt.sn.gateway.impl.connector;

import org.slj.mqtt.sn.gateway.cli.MqttsnInteractiveGateway;
import org.slj.mqtt.sn.gateway.cli.MqttsnInteractiveGatewayLauncher;
import org.slj.mqtt.sn.gateway.impl.MqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.gateway.impl.gateway.type.MqttsnAggregatingGateway;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBackendOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayPerformanceProfile;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.spi.IMqttsnTransport;

public class LoopbackGatewayInteractiveMain {
    public static void main(String[] args) throws Exception {
        MqttsnInteractiveGatewayLauncher.launch(new MqttsnInteractiveGateway() {
            protected AbstractMqttsnRuntimeRegistry createRuntimeRegistry(MqttsnOptions options, IMqttsnTransport transport) {
                MqttsnBackendOptions brokerOptions = new MqttsnBackendOptions(){
                    @Override
                    public boolean validConnectionDetails() {
                        return true;
                    }
                };
                brokerOptions.withHost(hostName).
                        withPort(port).
                        withUsername(username).
                        withPassword(password);

                ((MqttsnGatewayOptions)options).withPerformanceProfile(
                        MqttsnGatewayPerformanceProfile.EGRESS_CLOUD);
                ((MqttsnGatewayOptions)options).withMaxConnectedClients(10000);

                return MqttsnGatewayRuntimeRegistry.defaultConfiguration(options).
                        withBrokerConnectionFactory(new LoopbackMqttsnBrokerConnectionFactory()).
                        withBrokerService(new MqttsnAggregatingGateway(brokerOptions)).
                        withTransport(createTransport());
            }
        }, false, "Welcome to the loopback gateway. This version does NOT use a backend broker, instead brokering MQTT messages itself as a loopback to connected devices.");
    }
}