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

package org.slj.mqtt.sn.gateway.connector.thingstream;

import org.slj.mqtt.sn.cloud.MqttsnConnectorDescriptor;
import org.slj.mqtt.sn.gateway.connector.paho.PahoMqttsnBrokerConnection;
import org.slj.mqtt.sn.gateway.impl.connector.AbstractMqttsnConnector;
import org.slj.mqtt.sn.gateway.spi.connector.IMqttsnConnector;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorException;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorOptions;

public class ThingstreamConnector extends AbstractMqttsnConnector<PahoMqttsnBrokerConnection>
        implements IMqttsnConnector<PahoMqttsnBrokerConnection> {

    public static final MqttsnConnectorDescriptor DESCRIPTOR = new MqttsnConnectorDescriptor();
    static {
        DESCRIPTOR.setClassName(ThingstreamConnector.class.getName());
        DESCRIPTOR.setCompanyName("ublox");
        DESCRIPTOR.setProtocol("MQTT");
        DESCRIPTOR.setImageUrl("https://thingstream.io/thingstream-logo.svg");
        DESCRIPTOR.setDescription("Connects into the Thingstream cloud platform using an MQTT Now connection.");
        DESCRIPTOR.setName("u-blox Thingstream Connector");
        DESCRIPTOR.setDeveloper("Thingstream ublox");
    }

    public ThingstreamConnector(MqttsnConnectorDescriptor descriptor, MqttsnConnectorOptions options) {
        super(descriptor, options);
    }

    public ThingstreamConnector(MqttsnConnectorOptions options) {
        super(DESCRIPTOR, options);
    }

    @Override
    public PahoMqttsnBrokerConnection createConnection(MqttsnConnectorOptions options, String clientId) throws MqttsnConnectorException {
        try {
            PahoMqttsnBrokerConnection connection = new PahoMqttsnBrokerConnection(options);
            connection.connect();
            return connection;
        } catch(Exception e){
            throw new MqttsnConnectorException("error creating connection;", e);
        }
    }
}