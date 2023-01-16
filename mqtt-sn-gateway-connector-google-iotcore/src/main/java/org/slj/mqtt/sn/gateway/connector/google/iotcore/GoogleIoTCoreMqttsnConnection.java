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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.gateway.connector.paho.PahoMqttsnBrokerConnection;
import org.slj.mqtt.sn.gateway.spi.ConnectResult;
import org.slj.mqtt.sn.gateway.spi.DisconnectResult;
import org.slj.mqtt.sn.gateway.spi.PublishResult;
import org.slj.mqtt.sn.gateway.spi.Result;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorException;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorOptions;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnMessageFactory;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.TopicPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * @author simonjohnson
 *
 * Uses the AWS SDK (which in turn uses PAHO) to connect to the AWS IoT Core
 */
public class GoogleIoTCoreMqttsnConnection extends PahoMqttsnBrokerConnection {

    private Logger logger = LoggerFactory.getLogger(GoogleIoTCoreMqttsnConnection.class);

    static final String ALG_RSA = "RS256";
    static final String ALG_ES = "ES256";

    static final int TOKEN_EXPIRY_MINUTES = 60;

    public GoogleIoTCoreMqttsnConnection(MqttsnConnectorOptions options) {
        super(options);
    }

    @Override
    protected String createClientId(MqttsnConnectorOptions options) {
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
    public ConnectResult connect(IClientIdentifierContext context, IMqttsnMessage message) throws MqttsnConnectorException {
        if(isConnected()){
            logger.info("attaching {} device by clientId", context.getId());
            IMqttsnMessageFactory factory = backendService.getRegistry().getCodec().createMessageFactory();
            //-- tell IoT core we are attaching a device & register for device changes
            String topicPath = String.format("/devices/%s/attach", context.getId());
            IMqttsnMessage publish =
                    factory.createPublish(1, false, false, topicPath, new byte[0]);
            if(!super.publish(context, new TopicPath(topicPath), 1, false, new byte[0], publish).isError()){
                logger.info("device {} attached, subscribing or config changes", context.getId());
                topicPath = String.format("/devices/%s/config", context.getId());
                IMqttsnMessage subscribe = factory.createSubscribe(0, topicPath);
                subscribe(context, new TopicPath(topicPath), subscribe);
            }
            return new ConnectResult(Result.STATUS.SUCCESS);
        }
        return new ConnectResult(Result.STATUS.NOOP);
    }

    @Override
    public DisconnectResult disconnect(IClientIdentifierContext context, IMqttsnMessage message) throws MqttsnConnectorException {
        if(isConnected()){
            logger.info("detaching gateway device " + context.getId());
            IMqttsnMessageFactory factory = backendService.getRegistry().getCodec().createMessageFactory();
            String topicPath = String.format("/devices/%s/detach", context.getId());
            IMqttsnMessage publish =
                    factory.createPublish(1, false, false, topicPath, new byte[0]);
            super.publish(context, new TopicPath(topicPath), 1, false, new byte[0], publish);
            return new DisconnectResult(Result.STATUS.SUCCESS);
        }
        return new DisconnectResult(Result.STATUS.NOOP);
    }

    @Override
    public PublishResult publish(IClientIdentifierContext context, TopicPath topicPath, int qos, boolean retained, byte[] data, IMqttsnMessage message) throws MqttsnConnectorException {
        return super.publish(context, topicPath, qos, retained, data, message);
    }

    @Override
    public boolean canAccept(IClientIdentifierContext context, TopicPath topicPath, byte[] data, IMqttsnMessage message) {
        return MqttsnUtils.in(topicPath.toString(), new String[] {
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
                logger.info("subscribing to Google gateway error topic {}", topic);
                client.subscribe(topic, 0);
            }

            ///devices/{gateway_ID}/config
            {
                String topic = String.format("/devices/%s/config", getGoogleIoTGatewayId(options));
                logger.info("subscribing to Google gateway error topic {}", topic);
                client.subscribe(topic, 0);
            }
        } catch(Exception e){
            logger.error("error subscribing to error topic", e);
        }
    }

    @Override
    protected String createConnectionString(MqttsnConnectorOptions options) {
        return "ssl://mqtt.googleapis.com:8883";
    }

    @Override
    protected MqttConnectOptions createConnectOptions(MqttsnConnectorOptions options) throws MqttsnConnectorException {
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
            return connectOptions;
        } catch(Exception e){
            throw new MqttsnConnectorException(e);
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

    protected String getGoogleIoTProjectId(MqttsnConnectorOptions options){
        final String projectId = System.getProperty("projectId");
        if(projectId == null){
            throw new IllegalArgumentException("please specify -DprojectId=<gglProjectId>");
        }
        return projectId;
    }

    protected String getGoogleIoTGatewayId(MqttsnConnectorOptions options){
        final String gatewayId = System.getProperty("gatewayId");
        if(gatewayId == null){
            throw new IllegalArgumentException("please specify -DprojectId=<gatewayId>");
        }
        return gatewayId;
    }

    protected String getGoogleIoTRegistryId(MqttsnConnectorOptions options){
        final String registryId = System.getProperty("registryId");
        if(registryId == null){
            throw new IllegalArgumentException("please specify -DregistryId=<registryId>");
        }
        return registryId;
    }

    protected String getGoogleIoTCloudRegion(MqttsnConnectorOptions options){
        final String cloudRegion = System.getProperty("cloudRegion");
        if(cloudRegion == null){
            throw new IllegalArgumentException("please specify -DcloudRegion=<cloudRegion>");
        }
        return cloudRegion;
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        logger.info("received message from google iot {} -> {}", s, new String(mqttMessage.getPayload()));
        super.messageArrived(s, mqttMessage);
    }
}
