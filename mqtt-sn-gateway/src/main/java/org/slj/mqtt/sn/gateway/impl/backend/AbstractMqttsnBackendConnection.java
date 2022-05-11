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
import org.slj.mqtt.sn.gateway.spi.broker.IMqttsnBackendConnection;
import org.slj.mqtt.sn.gateway.spi.broker.IMqttsnBackendService;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBackendException;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.TopicPath;

/**
 * Allow the connection to be bootstrapped so implementers can simply extend this to provide their callbacks
 */
public abstract class AbstractMqttsnBackendConnection implements IMqttsnBackendConnection {

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
    public boolean canAccept(IMqttsnContext context, TopicPath topicPath, byte[] payload, IMqttsnMessage message) {
        return true;
    }

    @Override
    public DisconnectResult disconnect(IMqttsnContext context, IMqttsnMessage message) throws MqttsnBackendException {
        return new DisconnectResult(Result.STATUS.NOOP);
    }

    @Override
    public ConnectResult connect(IMqttsnContext context, IMqttsnMessage message) throws MqttsnBackendException {
        return new ConnectResult(Result.STATUS.NOOP);
    }

    @Override
    public SubscribeResult subscribe(IMqttsnContext context, TopicPath topicPath, IMqttsnMessage message) throws MqttsnBackendException {
        return new SubscribeResult(Result.STATUS.NOOP);
    }

    @Override
    public UnsubscribeResult unsubscribe(IMqttsnContext context, TopicPath topicPath, IMqttsnMessage message) throws MqttsnBackendException {
        return new UnsubscribeResult(Result.STATUS.NOOP);
    }

    @Override
    public PublishResult publish(IMqttsnContext context, TopicPath topicPath, int qos, boolean retained, byte[] payload, IMqttsnMessage message) throws MqttsnBackendException {
        return new PublishResult(Result.STATUS.NOOP);
    }
}
