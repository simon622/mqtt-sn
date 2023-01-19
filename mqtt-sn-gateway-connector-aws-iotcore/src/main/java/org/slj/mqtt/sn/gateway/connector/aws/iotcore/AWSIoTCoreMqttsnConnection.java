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

package org.slj.mqtt.sn.gateway.connector.aws.iotcore;

import com.amazonaws.services.iot.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.gateway.impl.backend.AbstractMqttsnBackendConnection;
import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorException;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorOptions;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.utils.TopicPath;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnSubscribe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * @author simonjohnson
 *
 * Uses the AWS SDK (which in turn uses PAHO) to connect to the AWS IoT Core
 */
public class AWSIoTCoreMqttsnConnection
        extends AbstractMqttsnBackendConnection {

    private Logger logger = LoggerFactory.getLogger(AWSIoTCoreMqttsnConnection.class.getName());

    static int MIN_TIMEOUT = 5000;

    private volatile AWSIotMqttClient client = null;
    private MqttsnConnectorOptions options;
    private final String clientId;

    public AWSIoTCoreMqttsnConnection(MqttsnConnectorOptions options) {
        this.options = options;
        this.clientId = options.getClientId();
    }

    protected int getOperationTimeout(){
        return Math.max(options.getConnectionTimeout() * 1000, MIN_TIMEOUT);
    }

    public void connect() throws MqttsnConnectorException {
        if(client == null || !isConnected()){
            synchronized (this){
                if(client == null || !isConnected()){
                    try {
                        if(client != null){
                            client = null;
                        }
                        initClient();
                        client.connect(getOperationTimeout());
                        logger.info("connecting new AWS client with username {} and keepAlive {}",
                                options.getUsername(), options.getKeepAlive());
                    } catch(Exception e){
                        throw new MqttsnConnectorException(e);
                    }
                }
            }
        }
    }

    private synchronized void initClient()
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        if(client == null){

            //-- try using the keystore
            if(options.getKeystoreLocation() != null){
                if(!validFile(options.getKeystoreLocation())){
                    throw new ExceptionInInitializerError("invalid keystore location");
                }

                File keystoreFile = new File(options.getKeystoreLocation());
                logger.info("loading keystore from {}", keystoreFile.getAbsolutePath());
                String keyStorePassword = options.getKeystorePassword();
                String keyPassword = options.getKeyPassword();
                KeyStore store = loadKeyStore(keystoreFile, keyStorePassword);
                client = new AWSIotMqttClient(options.getHostName(), clientId, store, keyPassword);

            } else {

                String certFile = null;
                if(!validFile(certFile = options.getCertificateFileLocation())){
                    throw new ExceptionInInitializerError("invalid certificate location");
                }

                String keyFile = null;
                if(!validFile(keyFile = options.getPrivateKeyFileLocation())){
                    throw new ExceptionInInitializerError("invalid private key location");
                }

                logger.info("loading keystore from certificate {} and private-key {}", certFile, keyFile);
                AwsCertUtils.KeyStorePasswordPair pair = AwsCertUtils.getKeyStorePasswordPair(certFile, keyFile);
                client = new AWSIotMqttClient(options.getHostName(), clientId, pair.keyStore, pair.keyPassword);
            }

            client.setKeepAliveInterval(options.getKeepAlive());
            client.setCleanSession(true);
            client.setNumOfClientThreads(1);
        }
    }

    private static int awsSafeQoS(int QoS){
        return Math.min(Math.max(QoS, 0), 1);
    }

    private static boolean validFile(String filePath){
        if(filePath == null) return false;
        File f = new File(filePath);
        return f.exists() && f.isFile() && f.canRead();
    }

    @Override
    public boolean isConnected() {
        return client != null &&
                client.getConnectionStatus() == AWSIotConnectionStatus.CONNECTED;
    }

    @Override
    public synchronized void close() {
        try {
            logger.info("disconnecting & closing connection to broker");
            if(client != null){
                if(client.getConnectionStatus() != AWSIotConnectionStatus.DISCONNECTED){
                    client.disconnect(getOperationTimeout(), true);
                }
            }
        } catch(AWSIotException | AWSIotTimeoutException e){
            logger.error("error encountered closing AWS IoT client;", e);
        } finally {
            client = null;
        }
    }

    @Override
    public SubscribeResult subscribe(IClientIdentifierContext context, TopicPath topicPath, IMqttsnMessage message)
            throws MqttsnConnectorException {
        try {
            if(isConnected()){
                int QoS = ((MqttsnSubscribe)message).getQoS();
                logger.info("subscribing connection to {} -> {}", topicPath, QoS);
                client.subscribe(new AWSIotTopic(topicPath.toString(), AWSIotQos.valueOf(awsSafeQoS(QoS))){
                    @Override
                    public void onMessage(AWSIotMessage message) {
                        try {
                            byte[] data = message.getPayload();
                            logger.info("received message from AWS IoT broker {} -> {} bytes", getTopic(), data.length);
                            receive(getTopic(), message.getQos().getValue(), false, data);
                        } catch(Exception e){
                            logger.error("error receiving message from broker;", e);
                        }
                    }
                });
                return new SubscribeResult(Result.STATUS.SUCCESS);
            }
            return new SubscribeResult(Result.STATUS.NOOP);
        } catch(AWSIotException e){
            throw new MqttsnConnectorException(e);
        }
    }

    @Override
    public UnsubscribeResult unsubscribe(IClientIdentifierContext context, TopicPath topicPath, IMqttsnMessage message)
            throws MqttsnConnectorException {
        try {
            if(isConnected()){
                logger.info("unsubscribing broker from {}", topicPath);
                client.unsubscribe(topicPath.toString());
                return new UnsubscribeResult(Result.STATUS.SUCCESS);
            }
            return new UnsubscribeResult(Result.STATUS.NOOP);
        } catch(AWSIotException e){
            throw new MqttsnConnectorException(e);
        }
    }

    @Override
    public PublishResult publish(IClientIdentifierContext context, TopicPath topicPath, int qos, boolean retained, byte[] payload, IMqttsnMessage message)
            throws MqttsnConnectorException {
        try {
           if(isConnected()){
               try {
                   client.publish(topicPath.toString(), AWSIotQos.valueOf(awsSafeQoS(qos)), payload, getOperationTimeout());
                   return new PublishResult(Result.STATUS.SUCCESS);
               } catch(AWSIotTimeoutException e){
                   logger.warn("timedout sending message to broker {}", topicPath);
                   return new PublishResult(Result.STATUS.ERROR, "timed out publishing to broker");
               }
           }
           return new PublishResult(Result.STATUS.NOOP);
        } catch(Exception e){
            throw new MqttsnConnectorException(e);
        }
    }

    @Override
    public DisconnectResult disconnect(IClientIdentifierContext context, IMqttsnMessage message) {
        return new DisconnectResult(Result.STATUS.SUCCESS);
    }

    @Override
    public ConnectResult connect(IClientIdentifierContext context, IMqttsnMessage message) {
        return new ConnectResult(Result.STATUS.SUCCESS);
    }

    protected static KeyStore loadKeyStore(File keyStoreFile, String password)
            throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
        return keyStore;
    }
}
