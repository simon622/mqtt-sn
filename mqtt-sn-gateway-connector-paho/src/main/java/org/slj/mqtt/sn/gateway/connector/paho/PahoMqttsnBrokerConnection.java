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
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.gateway.impl.backend.AbstractMqttsnBackendConnection;
import org.slj.mqtt.sn.gateway.spi.PublishResult;
import org.slj.mqtt.sn.gateway.spi.Result;
import org.slj.mqtt.sn.gateway.spi.SubscribeResult;
import org.slj.mqtt.sn.gateway.spi.UnsubscribeResult;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorException;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorOptions;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.utils.TopicPath;

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
    protected MqttsnConnectorOptions options;

    public PahoMqttsnBrokerConnection(MqttsnConnectorOptions options) {
        this.options = options;
    }

    public void connect() throws MqttsnConnectorException {
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
                        throw new MqttsnConnectorException(e);
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

    protected MqttConnectOptions createConnectOptions(MqttsnConnectorOptions options) throws MqttsnConnectorException {
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setAutomaticReconnect(false);
        if(options.getPassword() != null) connectOptions.setPassword(options.getPassword().toCharArray());
        if(options.getUsername() != null) connectOptions.setUserName(options.getUsername());
        connectOptions.setKeepAliveInterval(options.getKeepAlive());
        connectOptions.setConnectionTimeout(options.getConnectionTimeout());
        return connectOptions;
    }

    protected String createClientId(MqttsnConnectorOptions options) {
        return options.getClientId();
    }

    protected String createConnectionString(MqttsnConnectorOptions options) {

        String protocol = options.getProtocol();
        protocol = protocol == null ? options.getPort() ==
                MqttsnConnectorOptions.DEFAULT_MQTT_TLS_PORT ?
                MqttsnConnectorOptions.DEFAULT_MQTT_TLS_PROTOCOL : MqttsnConnectorOptions.DEFAULT_MQTT_PROTOCOL :
                protocol;

        return String.format("%s://%s:%s", protocol, options.getHostName(), options.getPort());
    }

    protected MqttClient createClient(MqttsnConnectorOptions options) throws MqttsnConnectorException {
        try {
            String clientId = createClientId(options);
            String connectionStr = createConnectionString(options);
            logger.log(Level.INFO, String.format("creating new paho client with host [%s] and clientId [%s]", connectionStr, clientId));
            MqttClient client = new MqttClient(connectionStr, clientId, new MemoryPersistence());
            client.setCallback(this);
            client.setTimeToWait(options.getConnectionTimeout() * 1000);
            return client;
        } catch(MqttException e){
            throw new MqttsnConnectorException(e);
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
    public SubscribeResult subscribe(IMqttsnContext context, TopicPath topicPath, IMqttsnMessage message) throws MqttsnConnectorException {
        try {
            int QoS = message == null ? MqttsnConstants.QoS2 : backendService.getRegistry().getCodec().getQoS(message, true);
            if(isConnected()) {
                logger.log(Level.INFO, String.format("subscribing connection to [%s] -> [%s]", topicPath, QoS));
                client.subscribe(topicPath.toString(), QoS);
                return new SubscribeResult(QoS);
            }
            return new SubscribeResult(Result.STATUS.NOOP);
        } catch(MqttException e){
            throw new MqttsnConnectorException(e);
        }
    }

    @Override
    public UnsubscribeResult unsubscribe(IMqttsnContext context, TopicPath topicPath, IMqttsnMessage message) throws MqttsnConnectorException {
        try {
            logger.log(Level.INFO, String.format("unsubscribing connection from [%s]", topicPath));
            if(isConnected()){
                client.unsubscribe(topicPath.toString());
                return new UnsubscribeResult(Result.STATUS.SUCCESS);
            }
            return new UnsubscribeResult(Result.STATUS.NOOP);
        } catch(MqttException e){
            throw new MqttsnConnectorException(e);
        }
    }

    @Override
    public PublishResult publish(IMqttsnContext context, TopicPath topicPath, int qos, boolean retained, byte[] payload, IMqttsnMessage message) throws MqttsnConnectorException {
        try {
           if(isConnected()){
               client.publish(topicPath.toString(), payload, qos, retained);
               return new PublishResult(Result.STATUS.SUCCESS);
           }
            return new PublishResult(Result.STATUS.NOOP);
        } catch(Exception e){
            throw new MqttsnConnectorException(e);
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
            receive(s, mqttMessage.getQos(), mqttMessage.isRetained(), data);
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