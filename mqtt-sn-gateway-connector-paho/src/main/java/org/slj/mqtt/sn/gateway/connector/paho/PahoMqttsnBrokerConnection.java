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
import org.slj.mqtt.sn.gateway.impl.broker.AbstractMqttsnBrokerConnection;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBrokerException;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBrokerOptions;
import org.slj.mqtt.sn.model.IMqttsnContext;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author simonjohnson
 *
 * Really simple backend connection to an MQTT broker using the PAHO client library. A single connection is managed by the runtime
 * and will be connected either eagerly on startup or lazily according to configuration
 */
public class PahoMqttsnBrokerConnection extends AbstractMqttsnBrokerConnection implements MqttCallback {

    private Logger logger = Logger.getLogger(PahoMqttsnBrokerConnection.class.getName());
    private volatile MqttClient client = null;
    private MqttsnBrokerOptions options;
    private final String clientId;

    public PahoMqttsnBrokerConnection(MqttsnBrokerOptions options, String clientId) {
        this.options = options;
        this.clientId = clientId;
    }

    public void connect() throws MqttException {
        if(client == null){
            synchronized (this){
                if(client == null){
                    client = createClient();
                }
            }
        }

        if(client != null && !client.isConnected()){
            synchronized (this){
                if (client != null && !client.isConnected()){
                    MqttConnectOptions connectOptions = new MqttConnectOptions();
                    connectOptions.setAutomaticReconnect(false);
                    if(options.getPassword() != null) connectOptions.setPassword(options.getPassword().toCharArray());
                    if(options.getUsername() != null) connectOptions.setUserName(options.getUsername());
                    connectOptions.setKeepAliveInterval(options.getKeepAlive());
                    connectOptions.setConnectionTimeout(options.getConnectionTimeout());
                    client.connect(connectOptions);
                    logger.log(Level.INFO, String.format("connecting client with username [%s] and keepAlive [%s]", options.getUsername(), options.getKeepAlive()));
                }
            }
        }
    }

    private MqttClient createClient() throws MqttException {
        String connectionStr = String.format("%s://%s:%s", options.getProtocol(), options.getHost(), options.getPort());
        logger.log(Level.INFO, String.format("creating new paho client with host [%s] and clientId [%s]", connectionStr, clientId));
        MqttClient client = new MqttClient(connectionStr, clientId, new MemoryPersistence());
        client.setCallback(this);
        client.setTimeToWait(options.getConnectionTimeout() * 1000);
        return client;
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
    public boolean subscribe(IMqttsnContext context, String topicPath, int QoS) throws MqttsnBrokerException {
        try {
            logger.log(Level.INFO, String.format("subscribing connection to [%s] -> [%s]", topicPath, QoS));
            if(isConnected()) {
                client.subscribe(topicPath, QoS);
                return true;
            }
            return false;
        } catch(MqttException e){
            throw new MqttsnBrokerException(e);
        }
    }

    @Override
    public boolean unsubscribe(IMqttsnContext context, String topicPath) throws MqttsnBrokerException {
        try {
            logger.log(Level.INFO, String.format("unsubscribing connection from [%s]", topicPath));
            if(isConnected()){
                client.unsubscribe(topicPath);
                return true;
            }
            return false;
        } catch(MqttException e){
            throw new MqttsnBrokerException(e);
        }
    }

    @Override
    public boolean publish(IMqttsnContext context, String topicPath, int QoS, boolean retain, byte[] data) throws MqttsnBrokerException {
        try {
           if(isConnected()){
               client.publish(topicPath, data, QoS, retain);
               return true;
           }
           return false;
        } catch(Exception e){
            throw new MqttsnBrokerException(e);
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

    @Override
    public boolean disconnect(IMqttsnContext context, int keepAlive) throws MqttsnBrokerException {
        return true;
    }

    @Override
    public boolean connect(IMqttsnContext context, boolean cleanSession, int keepAlive) throws MqttsnBrokerException{
        return true;
    }
}