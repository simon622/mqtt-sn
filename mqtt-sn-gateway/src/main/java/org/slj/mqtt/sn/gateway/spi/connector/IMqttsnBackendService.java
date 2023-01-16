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

package org.slj.mqtt.sn.gateway.spi.connector;

import org.slj.mqtt.sn.cloud.MqttsnConnectorDescriptor;
import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.IMqttsnService;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.List;

/**
 * The backend service determines the TYPE of behaviour of the gateway (ie. AGGREGATING, TRANSPARENT, FORWARDING), and is responsible
 * for passing the traffic on in the associated pattern to the installed and running connector
 */
public interface IMqttsnBackendService extends IMqttsnService  {

    IMqttsnGatewayRuntimeRegistry getRegistry();

    boolean isConnected(IClientIdentifierContext context) throws MqttsnConnectorException;

    ConnectResult connect(IClientIdentifierContext context, IMqttsnMessage message) throws MqttsnConnectorException;

    DisconnectResult disconnect(IClientIdentifierContext context, IMqttsnMessage message) throws MqttsnConnectorException;

    PublishResult publish(IClientIdentifierContext context, TopicPath topic, int qos, boolean retained, byte[] payload, IMqttsnMessage message) throws MqttsnConnectorException;

    SubscribeResult subscribe(IClientIdentifierContext context, TopicPath topic, IMqttsnMessage message) throws MqttsnConnectorException;

    UnsubscribeResult unsubscribe(IClientIdentifierContext context, TopicPath topic, IMqttsnMessage message) throws MqttsnConnectorException;

    void receive(String topicPath, int qos, boolean retained, byte[] payload) throws MqttsnException;

    void reinit() throws MqttsnConnectorException;

    int getQueuedCount();

    void pokeQueue() throws MqttsnConnectorException;

    void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException;

    void stop() throws MqttsnException;

    boolean running();

    boolean connectorAvailable(MqttsnConnectorDescriptor descriptor);

    boolean matchesRunningConnector(MqttsnConnectorDescriptor descriptor);

    boolean initializeConnector(MqttsnConnectorDescriptor descriptor, MqttsnConnectorOptions options) throws MqttsnException ;

    MqttsnConnectorDescriptor getDescriptorById(List<MqttsnConnectorDescriptor> descriptors, String connectorId);

    MqttsnConnectorDescriptor getInstalledDescriptor(List<MqttsnConnectorDescriptor> descriptors);
}
