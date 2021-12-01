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

package org.slj.mqtt.sn.gateway.impl.broker;

import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.broker.IMqttsnBrokerConnection;
import org.slj.mqtt.sn.gateway.spi.broker.IMqttsnBrokerService;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBrokerException;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBrokerOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.impl.AbstractMqttsnBackoffThreadService;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;

import java.util.logging.Level;

public abstract class AbstractMqttsnBrokerService
        extends AbstractMqttsnBackoffThreadService<IMqttsnGatewayRuntimeRegistry> implements IMqttsnBrokerService {

    protected MqttsnBrokerOptions options;

    public AbstractMqttsnBrokerService(MqttsnBrokerOptions options){
        this.options = options;
    }

    @Override
    public void start(IMqttsnGatewayRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        validateBrokerConnectionDetails();
        if(options.getConnectOnStartup()){
            logger.log(Level.INFO, "connect during startup requested..");
            try {
                getBrokerConnection(null);
            } catch(MqttsnBrokerException e){
                logger.log(Level.SEVERE, "encountered error attempting broker connect..", e);
                throw new MqttsnException("encountered error attempting broker connect..",e);
            }
            logger.log(Level.INFO, "connection complete, broker service ready.");
        }
    }

    protected void validateBrokerConnectionDetails(){
        if(!options.validConnectionDetails()){
            throw new MqttsnRuntimeException("invalid broker connection details!");
        }
    }

    @Override
    public ConnectResult connect(IMqttsnContext context, String clientId, boolean cleanSession, int keepAlive) throws MqttsnBrokerException {
        IMqttsnBrokerConnection connection = getBrokerConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnBrokerException("underlying broker connection was not connected");
        }
        boolean success = connection.connect(context, cleanSession, keepAlive);
        return new ConnectResult(success ? Result.STATUS.SUCCESS : Result.STATUS.ERROR, success  ? "connection success" : "connection refused by broker side");
    }

    @Override
    public DisconnectResult disconnect(IMqttsnContext context, int keepAlive) throws MqttsnBrokerException {
        IMqttsnBrokerConnection connection = getBrokerConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnBrokerException("underlying broker connection was not connected");
        }
        boolean success = connection.disconnect(context, keepAlive);
        return new DisconnectResult(success ? Result.STATUS.SUCCESS : Result.STATUS.ERROR, success  ? "disconnection success" : "disconnection refused by broker side");
    }

    @Override
    public PublishResult publish(IMqttsnContext context, String topicPath, int QoS, byte[] payload) throws MqttsnBrokerException {
        IMqttsnBrokerConnection connection = getBrokerConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnBrokerException("underlying broker connection was not connected");
        }
        boolean success = connection.publish(context, topicPath, QoS, false, payload);
        return new PublishResult(success ? Result.STATUS.SUCCESS : Result.STATUS.ERROR, success ? "publish success" : "publish refused by broker side");
    }

    @Override
    public SubscribeResult subscribe(IMqttsnContext context, String topicPath, int QoS) throws MqttsnBrokerException {
        IMqttsnBrokerConnection connection = getBrokerConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnBrokerException("underlying broker connection was not connected");
        }
        boolean success = connection.subscribe(context, topicPath, QoS);
        SubscribeResult res = new SubscribeResult(success ? Result.STATUS.SUCCESS : Result.STATUS.ERROR);
        if(success) res.setGrantedQoS(QoS);
        return res;
    }

    @Override
    public UnsubscribeResult unsubscribe(IMqttsnContext context, String topicPath) throws MqttsnBrokerException {
        IMqttsnBrokerConnection connection = getBrokerConnection(context);
        if(!connection.isConnected()){
            throw new MqttsnBrokerException("underlying broker connection was not connected");
        }
        boolean success = connection.unsubscribe(context, topicPath);
        return new UnsubscribeResult(success ? Result.STATUS.SUCCESS : Result.STATUS.ERROR);
    }

    protected IMqttsnBrokerConnection getBrokerConnection(IMqttsnContext context) throws MqttsnBrokerException{
        synchronized (this){
            IMqttsnBrokerConnection connection = getBrokerConnectionInternal(context);
            if(!connection.isConnected()){
                throw new MqttsnBrokerException("underlying broker connection was not connected");
            }
            return connection;
        }
    }

    @Override
    public void receive(String topicPath, byte[] payload, int QoS) throws MqttsnException {
        registry.getGatewaySessionService().receiveToSessions(topicPath, payload, QoS);
    }

    @Override
    public IMqttsnRuntimeRegistry getRuntimeRegistry() {
        return registry;
    }

    protected abstract void close(IMqttsnBrokerConnection connection) throws MqttsnBrokerException;

    protected abstract IMqttsnBrokerConnection getBrokerConnectionInternal(IMqttsnContext context) throws MqttsnBrokerException;
}
