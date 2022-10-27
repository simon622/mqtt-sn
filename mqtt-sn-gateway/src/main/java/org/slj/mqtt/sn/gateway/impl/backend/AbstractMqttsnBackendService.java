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

import org.slj.mqtt.sn.cloud.MqttsnConnectorDescriptor;
import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.connector.*;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.impl.AbstractMqttsnBackoffThreadService;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.logging.Level;

public abstract class AbstractMqttsnBackendService
        extends AbstractMqttsnBackoffThreadService implements IMqttsnBackendService {

    public AbstractMqttsnBackendService(){
    }

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        if(!running){
            super.start(runtime);
        }
    }

    public void stop() throws MqttsnException {
        if(running){
            super.stop();
        }
    }

    public synchronized boolean initializeConnector(MqttsnConnectorDescriptor descriptor, MqttsnConnectorOptions options) throws MqttsnException {
        try {
            if(connectorAvailable(descriptor)){
                if(running()){
                    stop();
                }
                logger.log(Level.INFO, String.format("starting new instance of connector [%s] using [%s]", descriptor.getClassName(), options));
                IMqttsnConnector connector = getConnectorClass(descriptor).getConstructor(
                                MqttsnConnectorDescriptor.class, MqttsnConnectorOptions.class).
                        newInstance(descriptor, options);

                getRegistry().withConnector(connector);
                start(getRegistry());
                return true;
            } else {
                return false;
            }
        }
        catch (Exception e){
            throw new MqttsnException("unable to initialize connector;", e);
        }
    }

    @Override
    public ConnectResult connect(IMqttsnContext context, IMqttsnMessage message) throws MqttsnConnectorException {
        IMqttsnConnectorConnection connection = getConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnConnectorException("underlying broker connection was not connected");
        }
        ConnectResult result = connection.connect(context, message);
        return result;
    }

    @Override
    public DisconnectResult disconnect(IMqttsnContext context, IMqttsnMessage message) throws MqttsnConnectorException {
        IMqttsnConnectorConnection connection = getConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnConnectorException("underlying broker connection was not connected");
        }
        DisconnectResult result = connection.disconnect(context, message);
        return result;
    }

    @Override
    public PublishResult publish(IMqttsnContext context, TopicPath topic, int qos, boolean retained, byte[] payload, IMqttsnMessage message) throws MqttsnConnectorException {
        IMqttsnConnectorConnection connection = getConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnConnectorException("underlying broker connection was not connected");
        }
        PublishResult result = connection.publish(context, topic, qos, retained, payload, message);
        return result;
    }

    @Override
    public SubscribeResult subscribe(IMqttsnContext context, TopicPath topic, IMqttsnMessage message) throws MqttsnConnectorException {
        IMqttsnConnectorConnection connection = getConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnConnectorException("underlying broker connection was not connected");
        }
        SubscribeResult res = connection.subscribe(context, topic, message);
        return res;
    }

    @Override
    public UnsubscribeResult unsubscribe(IMqttsnContext context, TopicPath topic, IMqttsnMessage message) throws MqttsnConnectorException {
        IMqttsnConnectorConnection connection = getConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnConnectorException("underlying broker connection was not connected");
        }
        UnsubscribeResult res = connection.unsubscribe(context, topic, message);
        return res;
    }

    protected IMqttsnConnectorConnection getConnection(IMqttsnContext context) throws MqttsnConnectorException {
        synchronized (this){
            IMqttsnConnectorConnection connection = getConnectionInternal(context);
            if(!connection.isConnected()){
                throw new MqttsnConnectorException("underlying broker connection was not connected");
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

    @Override
    public boolean connectorAvailable(MqttsnConnectorDescriptor descriptor) {
        try {
            return getConnectorClass(descriptor) != null;
        } catch(MqttsnNotFoundException e){
            return false;
        }
    }

    @Override
    public boolean matchesRunningConnector(MqttsnConnectorDescriptor descriptor) {
        return descriptor.getClassName().equals(
                getRegistry().getConnector().getClass().getName());
    }

    protected Class<? extends IMqttsnConnector> getConnectorClass(MqttsnConnectorDescriptor descriptor) throws MqttsnNotFoundException {
        String className = descriptor.getClassName();
        if(className != null && !className.isEmpty()){
            try {
                return (Class<? extends IMqttsnConnector>) Class.forName(className);
            } catch(ClassNotFoundException e){
                //-- check the context classloader
                try {
                    ClassLoader cls = Thread.currentThread().getContextClassLoader();
                    return (Class<? extends IMqttsnConnector>)  cls.loadClass(className);
                } catch(ClassNotFoundException E){
                }
            }
        }
        throw new MqttsnNotFoundException("unable to load connector from runtime <" + className + ">");
    }

    protected abstract void close(IMqttsnConnectorConnection connection) throws MqttsnConnectorException;

    protected abstract IMqttsnConnectorConnection getConnectionInternal(IMqttsnContext context) throws MqttsnConnectorException;
}
