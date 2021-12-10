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

package org.slj.mqtt.sn.gateway.connector.google.iotcore;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.joda.time.DateTime;
import org.slj.mqtt.sn.gateway.connector.paho.PahoMqttsnBrokerConnection;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBrokerException;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBrokerOptions;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.utils.MqttsnUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author simonjohnson
 *
 * Uses the AWS SDK (which in turn uses PAHO) to connect to the AWS IoT Core
 */
public class GoogleIoTCoreMqttsnBrokerConnection extends PahoMqttsnBrokerConnection {

    private Logger logger = Logger.getLogger(GoogleIoTCoreMqttsnBrokerConnection.class.getName());

    static final String ALG_RSA = "RS256";
    static final String ALG_ES = "ES256";

    static final int TOKEN_EXPIRY_MINUTES = 60;

    public GoogleIoTCoreMqttsnBrokerConnection(MqttsnBrokerOptions options, String clientId) {
        super(options, clientId);
    }

    @Override
    protected String createClientId(MqttsnBrokerOptions options) {
        final String mqttClientId =
                String.format(
                        "projects/%s/locations/%s/registries/%s/devices/%s",
                        getGoogleIoTProjectId(options),
                        getGoogleIoTCloudRegion(options),
                        getGoogleIoTRegistryId(options),
                        getGoogleIoTGatewayId(options));

        return mqttClientId;
    }

    @Override
    public boolean connect(IMqttsnContext context, boolean cleanSession, int keepAlive) throws MqttsnBrokerException {
        if(isConnected()){
            logger.log(Level.INFO, String.format("attaching [%s] device by clientId", context.getId()));
            //-- tell IoT core we are attaching a device & register for device changes
            if(super.publish(context, String.format("/devices/%s/attach", context.getId()), 1, false, new byte[0])){
                logger.log(Level.INFO, String.format("device [%s] attached, subscribing or config changes", context.getId()));
                subscribe(context, String.format("/devices/%s/config", context.getId()), 0);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean disconnect(IMqttsnContext context, int keepAlive) throws MqttsnBrokerException {
        if(isConnected()){
            logger.log(Level.INFO, String.format("detaching gateway device " + context.getId()));
            super.publish(context, String.format("/devices/%s/detach", context.getId()), 1, false, new byte[0]);
            return true;
        }
        return false;
    }

    @Override
    public boolean publish(IMqttsnContext context, String topicPath, int QoS, boolean retain, byte[] data) throws MqttsnBrokerException {
        if(isConnected()){
            super.publish(context, topicPath, 1, false, data);
            return true;
        }
        return false;
    }

    @Override
    public boolean canAccept(IMqttsnContext context, String topicPath, int QoS, byte[] data) throws MqttsnBrokerException {
        return MqttsnUtils.in(topicPath, new String[] {
                String.format("/devices/%s/state", context.getId()),
                String.format("/devices/%s/events", context.getId())
        });
    }

    @Override
    protected void onClientConnected(MqttClient client){
        try {
            ///devices/{gateway_ID}/errors
            {
                String topic = String.format("/devices/%s/errors", getGoogleIoTGatewayId(options));
                logger.log(Level.INFO, String.format("subscribing to Google gateway error topic [%s]", topic));
                client.subscribe(topic, 0);
            }

            ///devices/{gateway_ID}/config
            {
                String topic = String.format("/devices/%s/config", getGoogleIoTGatewayId(options));
                logger.log(Level.INFO, String.format("subscribing to Google gateway error topic [%s]", topic));
                client.subscribe(topic, 0);
            }
        } catch(Exception e){
            logger.log(Level.SEVERE, String.format("error subscribing to error topic"), e);
        }
    }

    @Override
    protected String createConnectionString(MqttsnBrokerOptions options) throws MqttsnBrokerException {
        return "ssl://mqtt.googleapis.com:8883";
    }

    @Override
    protected MqttConnectOptions createConnectOptions(MqttsnBrokerOptions options) throws MqttsnBrokerException {
        try {
            MqttConnectOptions connectOptions = super.createConnectOptions(options);
            connectOptions.setUserName("unused"); //per the GGL documents
            connectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

            String alg = System.getProperty("algorithm");
            if(alg == null || alg.equals(ALG_ES)){
                connectOptions.setPassword(
                        createJwtEs(getGoogleIoTProjectId(options),
                                options.getPrivateKeyFileLocation()).toCharArray());
            } else {
                connectOptions.setPassword(
                        createJwtRsa(getGoogleIoTProjectId(options),
                                options.getPrivateKeyFileLocation()).toCharArray());
            }
            logger.log(Level.INFO, new String(connectOptions.getPassword()));
            return connectOptions;
        } catch(Exception e){
            throw new MqttsnBrokerException(e);
        }
    }

    /** Create a Cloud IoT Core JWT for the given project id, signed with the given RSA key. */
    private static String createJwtRsa(String projectId, String privateKeyFile)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        DateTime now = new DateTime();
        // Create a JWT to authenticate this device. The device will be disconnected after the token
        // expires, and will have to reconnect with a new token. The audience field should always be set
        // to the GCP project id.
        JwtBuilder jwtBuilder =
                Jwts.builder()
                        .setIssuedAt(now.toDate())
                        .setExpiration(now.plusMinutes(TOKEN_EXPIRY_MINUTES).toDate())
                        .setAudience(projectId);

        byte[] keyBytes = Files.readAllBytes(Paths.get(privateKeyFile));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return jwtBuilder.signWith(SignatureAlgorithm.RS256, kf.generatePrivate(spec)).compact();
    }

    /** Create a Cloud IoT Core JWT for the given project id, signed with the given ES key. */
    private static String createJwtEs(String projectId, String privateKeyFile)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        DateTime now = new DateTime();
        // Create a JWT to authenticate this device. The device will be disconnected after the token
        // expires, and will have to reconnect with a new token. The audience field should always be set
        // to the GCP project id.
        JwtBuilder jwtBuilder =
                Jwts.builder()
                        .setIssuedAt(now.toDate())
                        .setExpiration(now.plusMinutes(TOKEN_EXPIRY_MINUTES).toDate())
                        .setAudience(projectId);

        byte[] keyBytes = Files.readAllBytes(Paths.get(privateKeyFile));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return jwtBuilder.signWith(SignatureAlgorithm.ES256, kf.generatePrivate(spec)).compact();
    }

    protected String getGoogleIoTProjectId(MqttsnBrokerOptions options){
        final String projectId = System.getProperty("projectId");
        if(projectId == null){
            throw new IllegalArgumentException("please specify -DprojectId=<gglProjectId>");
        }
        return projectId;
    }

    protected String getGoogleIoTGatewayId(MqttsnBrokerOptions options){
        final String gatewayId = System.getProperty("gatewayId");
        if(gatewayId == null){
            throw new IllegalArgumentException("please specify -DprojectId=<gatewayId>");
        }
        return gatewayId;
    }

    protected String getGoogleIoTRegistryId(MqttsnBrokerOptions options){
        final String registryId = System.getProperty("registryId");
        if(registryId == null){
            throw new IllegalArgumentException("please specify -DregistryId=<registryId>");
        }
        return registryId;
    }

    protected String getGoogleIoTCloudRegion(MqttsnBrokerOptions options){
        final String cloudRegion = System.getProperty("cloudRegion");
        if(cloudRegion == null){
            throw new IllegalArgumentException("please specify -DcloudRegion=<cloudRegion>");
        }
        return cloudRegion;
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        logger.log(Level.INFO, String.format("received message from google iot [%s] -> [%s]", s, new String(mqttMessage.getPayload())));
        super.messageArrived(s, mqttMessage);
    }
}