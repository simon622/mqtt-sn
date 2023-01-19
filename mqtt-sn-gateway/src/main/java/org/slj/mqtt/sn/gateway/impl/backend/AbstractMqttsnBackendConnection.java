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

package org.slj.mqtt.sn.gateway.impl.backend;

import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.connector.IMqttsnConnectorConnection;
import org.slj.mqtt.sn.gateway.spi.connector.IMqttsnBackendService;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorException;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.TopicPath;

/**
 * Allow the connection to be bootstrapped so implementers can simply extend this to provide their callbacks
 */
public abstract class AbstractMqttsnBackendConnection implements IMqttsnConnectorConnection {

    protected IMqttsnBackendService backendService;

    public void setBrokerService(IMqttsnBackendService backendService){
        this.backendService = backendService;
    }

    public void receive(String topicPath, int qos, boolean retained, byte[] payload) throws MqttsnException {
        if(backendService == null){
            throw new MqttsnException("backendService not available to connection, receive will fail");
        }
        backendService.receive(topicPath, qos, retained, payload);
    }

    @Override
    public boolean canAccept(IClientIdentifierContext context, TopicPath topicPath, byte[] payload, IMqttsnMessage message) {
        return true;
    }

    @Override
    public DisconnectResult disconnect(IClientIdentifierContext context, IMqttsnMessage message) throws MqttsnConnectorException {
        return new DisconnectResult(Result.STATUS.NOOP);
    }

    @Override
    public ConnectResult connect(IClientIdentifierContext context, IMqttsnMessage message) throws MqttsnConnectorException {
        return new ConnectResult(Result.STATUS.NOOP);
    }

    @Override
    public SubscribeResult subscribe(IClientIdentifierContext context, TopicPath topicPath, IMqttsnMessage message) throws MqttsnConnectorException {
        return new SubscribeResult(Result.STATUS.NOOP);
    }

    @Override
    public UnsubscribeResult unsubscribe(IClientIdentifierContext context, TopicPath topicPath, IMqttsnMessage message) throws MqttsnConnectorException {
        return new UnsubscribeResult(Result.STATUS.NOOP);
    }

    @Override
    public PublishResult publish(IClientIdentifierContext context, TopicPath topicPath, int qos, boolean retained, byte[] payload, IMqttsnMessage message) throws MqttsnConnectorException {
        return new PublishResult(Result.STATUS.NOOP);
    }
}
