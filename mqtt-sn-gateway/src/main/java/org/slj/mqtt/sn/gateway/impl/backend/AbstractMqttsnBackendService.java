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
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBackendOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.impl.AbstractMqttsnBackoffThreadService;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.logging.Level;

public abstract class AbstractMqttsnBackendService
        extends AbstractMqttsnBackoffThreadService implements IMqttsnBackendService {

    protected MqttsnBackendOptions options;

    public AbstractMqttsnBackendService(MqttsnBackendOptions options){
        this.options = options;
    }

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        validateBrokerConnectionDetails();
    }

    protected void validateBrokerConnectionDetails(){
        if(!options.validConnectionDetails()){
            throw new MqttsnRuntimeException("invalid broker connection details!");
        }
    }

    @Override
    public ConnectResult connect(IMqttsnContext context, IMqttsnMessage message) throws MqttsnBackendException {
        IMqttsnBackendConnection connection = getBrokerConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnBackendException("underlying broker connection was not connected");
        }
        ConnectResult result = connection.connect(context, message);
        return result;
    }

    @Override
    public DisconnectResult disconnect(IMqttsnContext context, IMqttsnMessage message) throws MqttsnBackendException {
        IMqttsnBackendConnection connection = getBrokerConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnBackendException("underlying broker connection was not connected");
        }
        DisconnectResult result = connection.disconnect(context, message);
        return result;
    }

    @Override
    public PublishResult publish(IMqttsnContext context, TopicPath topic, int qos, boolean retained, byte[] payload, IMqttsnMessage message) throws MqttsnBackendException {
        IMqttsnBackendConnection connection = getBrokerConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnBackendException("underlying broker connection was not connected");
        }
        PublishResult result = connection.publish(context, topic, qos, retained, payload, message);
        return result;
    }

    @Override
    public SubscribeResult subscribe(IMqttsnContext context, TopicPath topic, IMqttsnMessage message) throws MqttsnBackendException {
        IMqttsnBackendConnection connection = getBrokerConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnBackendException("underlying broker connection was not connected");
        }
        SubscribeResult res = connection.subscribe(context, topic, message);
        return res;
    }

    @Override
    public UnsubscribeResult unsubscribe(IMqttsnContext context, TopicPath topic, IMqttsnMessage message) throws MqttsnBackendException {
        IMqttsnBackendConnection connection = getBrokerConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnBackendException("underlying broker connection was not connected");
        }
        UnsubscribeResult res = connection.unsubscribe(context, topic, message);
        return res;
    }

    protected IMqttsnBackendConnection getBrokerConnection(IMqttsnContext context) throws MqttsnBackendException {
        synchronized (this){
            IMqttsnBackendConnection connection = getBrokerConnectionInternal(context);
            if(!connection.isConnected()){
                throw new MqttsnBackendException("underlying broker connection was not connected");
            }
            return connection;
        }
    }

    @Override
    public void receive(String topicPath, int qos, boolean retained, byte[] payload) {
        registry.getRuntime().async(() -> {
            try {
                getRegistry().getGatewaySessionService().receiveToSessions(topicPath,qos, retained, payload);
                getRegistry().getMetrics().getMetric(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH_RECEIVE).increment(1);
            } catch(Exception e){
                logger.log(Level.SEVERE, "error receiving to sessions;", e);
            }
        });
    }

    @Override
    public IMqttsnGatewayRuntimeRegistry getRegistry() {
        return (IMqttsnGatewayRuntimeRegistry) registry;
    }

    @Override
    public int getQueuedCount() {
        return 0;
    }


    protected abstract void close(IMqttsnBackendConnection connection) throws MqttsnBackendException;

    protected abstract IMqttsnBackendConnection getBrokerConnectionInternal(IMqttsnContext context) throws MqttsnBackendException;
}
