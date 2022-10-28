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

import org.slj.mqtt.sn.cloud.MqttsnConnectorDescriptor;
import org.slj.mqtt.sn.gateway.spi.connector.IMqttsnConnector;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorException;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorOptions;

public class LoopbackMqttsnConnector
        extends AbstractMqttsnConnector<LoopbackMqttsnConnectorConnection>
            implements IMqttsnConnector<LoopbackMqttsnConnectorConnection> {

    static final MqttsnConnectorDescriptor DESCRIPTOR = new MqttsnConnectorDescriptor();
    static {
        DESCRIPTOR.setClassName(LoopbackMqttsnConnector.class.getName());
        DESCRIPTOR.setCompanyName("SLJ");
        DESCRIPTOR.setProtocol("Native");
        DESCRIPTOR.setDescription("The loopback connector puts the gateway into standalone mode. It needs no remote MQTT broker to connect to, instead it will service the MQTT-SN clients directly, acting as a broker itself, using the MQTT-SN protocol.");
        DESCRIPTOR.setName("Loopback Gateway Connector");
        DESCRIPTOR.setDeveloper("Simon Johnson");
    }

    public LoopbackMqttsnConnector(MqttsnConnectorDescriptor descriptor, MqttsnConnectorOptions options) {
        super(descriptor, options);
    }

    @Override
    public LoopbackMqttsnConnectorConnection createConnection(MqttsnConnectorOptions options, String clientId) throws MqttsnConnectorException {
        try {
            LoopbackMqttsnConnectorConnection connection = new LoopbackMqttsnConnectorConnection(options, clientId);
            connection.connect();
            return connection;
        } catch(Exception e){
            throw new MqttsnConnectorException("error creating connection;", e);
        }
    }

    @Override
    public String getConnectionString() {
        return "java@localhost";
    }
}

