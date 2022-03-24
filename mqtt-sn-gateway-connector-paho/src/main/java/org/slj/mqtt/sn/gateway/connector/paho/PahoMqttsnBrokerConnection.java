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

package org.slj.mqtt.sn.gateway.connector.paho;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slj.mqtt.sn.gateway.impl.backend.AbstractMqttsnBackendConnection;
import org.slj.mqtt.sn.gateway.spi.PublishResult;
import org.slj.mqtt.sn.gateway.spi.Result;
import org.slj.mqtt.sn.gateway.spi.SubscribeResult;
import org.slj.mqtt.sn.gateway.spi.UnsubscribeResult;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBackendException;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBackendOptions;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.utils.TopicPath;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnPublish;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnSubscribe;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author simonjohnson
 *
 * Really simple backend connection to an MQTT broker using the PAHO client library. A single connection is managed by the runtime
 * and will be connected either eagerly on startup or lazily according to configuration
 */
public class PahoMqttsnBrokerConnection extends AbstractMqttsnBackendConnection implements MqttCallback {

    private Logger logger = Logger.getLogger(PahoMqttsnBrokerConnection.class.getName());
    private volatile MqttClient client = null;
    protected MqttsnBackendOptions options;
    protected final String clientId;

    public PahoMqttsnBrokerConnection(MqttsnBackendOptions options, String clientId) {
        this.options = options;
        this.clientId = clientId;
    }

    public void connect() throws MqttsnBackendException {
        if(client == null){
            synchronized (this){
                if(client == null){
                    client = createClient(options);
                }
            }
        }

        if(client != null && !client.isConnected()){
            synchronized (this){
                if (client != null && !client.isConnected()){
                    MqttConnectOptions connectOptions = createConnectOptions(options);
                    try {
                        logger.log(Level.INFO, String.format("connecting client with options [%s]", options));
                        client.connect(connectOptions);
                        if(client.isConnected()){
                            onClientConnected(client);
                        }
                    } catch(MqttException e){
                        throw new MqttsnBackendException(e);
                    }
                }
            }
        }
    }

    /**
     * hook method called on successful initial connection
     */
    protected void onClientConnected(MqttClient client){

    }

    protected MqttConnectOptions createConnectOptions(MqttsnBackendOptions options) throws MqttsnBackendException {
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setAutomaticReconnect(false);
        if(options.getPassword() != null) connectOptions.setPassword(options.getPassword().toCharArray());
        if(options.getUsername() != null) connectOptions.setUserName(options.getUsername());
        connectOptions.setKeepAliveInterval(options.getKeepAlive());
        connectOptions.setConnectionTimeout(options.getConnectionTimeout());
        return connectOptions;
    }

    protected String createClientId(MqttsnBackendOptions options) throws MqttsnBackendException {
        return clientId;
    }

    protected String createConnectionString(MqttsnBackendOptions options) throws MqttsnBackendException {
        return String.format("%s://%s:%s", options.getProtocol(), options.getHost(), options.getPort());
    }

    protected MqttClient createClient(MqttsnBackendOptions options) throws MqttsnBackendException {
        try {
            String clientId = createClientId(options);
            String connectionStr = createConnectionString(options);
            logger.log(Level.INFO, String.format("creating new paho client with host [%s] and clientId [%s]", connectionStr, clientId));
            MqttClient client = new MqttClient(connectionStr, clientId, new MemoryPersistence());
            client.setCallback(this);
            client.setTimeToWait(options.getConnectionTimeout() * 1000);
            return client;
        } catch(MqttException e){
            throw new MqttsnBackendException(e);
        }
    }

    @Override
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    @Override
    public void close() {
        synchronized (this){
            try {
                if(client != null && client.isConnected()){
                    logger.log(Level.INFO, "closing connection to broker");
                    client.disconnectForcibly();
                }
            } catch(MqttException e){
                logger.log(Level.SEVERE, "error encountered closing paho client;", e);
            } finally {
                try {
                    if(client != null){
                        client.close(true);
                    }
                } catch(Exception e){
                } finally {
                    client = null;
                }
            }
        }
    }

    @Override
    public SubscribeResult subscribe(IMqttsnContext context, TopicPath topicPath, IMqttsnMessage message) throws MqttsnBackendException {
        try {
            if(isConnected()) {
                int QoS = backendService.getRuntimeRegistry().getCodec().getQoS(message, true);
                logger.log(Level.INFO, String.format("subscribing connection to [%s] -> [%s]", topicPath, QoS));
                client.subscribe(topicPath.toString(), QoS);
                return new SubscribeResult(Result.STATUS.SUCCESS);
            }
            return new SubscribeResult(Result.STATUS.NOOP);
        } catch(MqttException e){
            throw new MqttsnBackendException(e);
        }
    }

    @Override
    public UnsubscribeResult unsubscribe(IMqttsnContext context, TopicPath topicPath, IMqttsnMessage message) throws MqttsnBackendException {
        try {
            logger.log(Level.INFO, String.format("unsubscribing connection from [%s]", topicPath));
            if(isConnected()){
                client.unsubscribe(topicPath.toString());
                return new UnsubscribeResult(Result.STATUS.SUCCESS);
            }
            return new UnsubscribeResult(Result.STATUS.NOOP);
        } catch(MqttException e){
            throw new MqttsnBackendException(e);
        }
    }

    @Override
    public PublishResult publish(IMqttsnContext context, TopicPath topicPath, IMqttsnMessage message) throws MqttsnBackendException {
        try {
           if(isConnected()){
               int QoS = backendService.getRuntimeRegistry().getCodec().getQoS(message, true);
               boolean retained = backendService.getRuntimeRegistry().getCodec().isRetainedPublish(message);
               byte[] data = backendService.getRuntimeRegistry().getCodec().getData(message).getData();
               client.publish(topicPath.toString(), data, QoS, retained);
               return new PublishResult(Result.STATUS.SUCCESS);
           }
            return new PublishResult(Result.STATUS.NOOP);
        } catch(Exception e){
            throw new MqttsnBackendException(e);
        }
    }

    @Override
    public void connectionLost(Throwable t) {
        logger.log(Level.SEVERE, "connection reported lost on broker side", t);
        try {
            client.close(true);
        } catch(Exception e){
        } finally {
            client = null;
        }
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        try {
            byte[] data = mqttMessage.getPayload();
            logger.log(Level.INFO, String.format("recieved message from connection [%s] -> [%s] bytes", s, data.length));
            receive(s, data, mqttMessage.getQos());
        } catch(Exception e){
            logger.log(Level.SEVERE, "gateway reported issue receiving message from broker;", e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, String.format("broker confirm delivery complete [%s] -> [%s]",
                    token.getMessageId(), Arrays.toString(token.getTopics())));
        }
    }
}