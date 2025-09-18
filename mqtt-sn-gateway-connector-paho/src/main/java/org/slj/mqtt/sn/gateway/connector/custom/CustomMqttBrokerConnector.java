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

package org.slj.mqtt.sn.gateway.connector.custom;

import org.slj.mqtt.sn.cloud.DescriptorProperty;
import org.slj.mqtt.sn.cloud.MqttsnConnectorDescriptor;
import org.slj.mqtt.sn.gateway.connector.paho.PahoMqttsnBrokerConnection;
import org.slj.mqtt.sn.gateway.impl.connector.AbstractMqttsnConnector;
import org.slj.mqtt.sn.gateway.spi.connector.IMqttsnConnector;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorException;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CustomMqttBrokerConnector extends AbstractMqttsnConnector<PahoMqttsnBrokerConnection>
        implements IMqttsnConnector<PahoMqttsnBrokerConnection> {

    public static final MqttsnConnectorDescriptor DESCRIPTOR = new MqttsnConnectorDescriptor();
    static {
        DESCRIPTOR.setClassName(CustomMqttBrokerConnector.class.getName());
        DESCRIPTOR.setCompanyName("Eclipse");
        DESCRIPTOR.setProtocol("MQTT");
        DESCRIPTOR.setDescription("Connect your MQTT-SN devices and gateway to a custom MQTT installation.");
        DESCRIPTOR.setName("Aggregating Gateway MQTT Connector");
        DESCRIPTOR.setDeveloper("PAHO");
        DESCRIPTOR.setImageUrl("https://mqtt.org/assets/downloads/mqtt-ver.png");

        List<DescriptorProperty> properties = new ArrayList<>();

        DescriptorProperty hostName = new DescriptorProperty();
        hostName.setName("hostName");
        hostName.setType("text");
        hostName.setRequired(false);
        properties.add(hostName);

        DescriptorProperty clientId = new DescriptorProperty();
        clientId.setName("clientId");
        clientId.setType("text");
        clientId.setRequired(true);
        clientId.setDefaultValue("client-" + ThreadLocalRandom.current().nextInt(1000000));
        properties.add(clientId);

        DescriptorProperty username = new DescriptorProperty();
        username.setName("username");
        username.setType("text");
        username.setRequired(false);
        properties.add(username);

        DescriptorProperty password = new DescriptorProperty();
        password.setName("password");
        password.setType("password");
        password.setRequired(false);
        properties.add(password);

        DescriptorProperty port = new DescriptorProperty();
        port.setName("port");
        port.setType("int");
        port.setRequired(true);
        port.setDefaultValue("3306");
        properties.add(port);

        DESCRIPTOR.setProperties(properties);
    }

    public CustomMqttBrokerConnector(MqttsnConnectorDescriptor descriptor, MqttsnConnectorOptions options) {
        super(descriptor, options);
    }

    public CustomMqttBrokerConnector(MqttsnConnectorOptions options) {
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

    @Override
    public String getConnectionString() {
        return String.format("%s@%s:%s",
                options.getClientId(),
                options.getHostName(),
                options.getPort());
    }
}
