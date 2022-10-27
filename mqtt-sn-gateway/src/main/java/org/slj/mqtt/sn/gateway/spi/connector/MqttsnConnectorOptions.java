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

package org.slj.mqtt.sn.gateway.spi.connector;

public class MqttsnConnectorOptions {

    public static final int DEFAULT_KEEPALIVE = 30 * 10;
    public static final int DEFAULT_CONNECTION_TIMEOUT = 30;
    public static final int DEFAULT_MQTT_PORT = 1883;
    public static final int DEFAULT_MQTT_TLS_PORT = 8883;
    public static final String DEFAULT_MQTT_PROTOCOL = "tcp";
    public static final String DEFAULT_MQTT_TLS_PROTOCOL = "ssl";
    private int keepAlive = DEFAULT_KEEPALIVE;
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private int port = DEFAULT_MQTT_PORT;
    private String protocol;
    private String username;
    private String password;
    private String hostName;
    private String keystoreLocation = null;
    private String keystorePassword = null;
    private String keyPassword = null;
    private String certificateFileLocation = null;
    private String privateKeyFileLocation = null;
    private String clientId = null;

    public MqttsnConnectorOptions(){

    }

    public MqttsnConnectorOptions withClientId(String clientId){
        this.clientId = clientId;
        return this;
    }

    public MqttsnConnectorOptions withCertificateFileLocation(String certificateFileLocation){
        this.certificateFileLocation = certificateFileLocation;
        return this;
    }

    public MqttsnConnectorOptions withPrivateKeyFileLocation(String privateKeyFileLocation){
        this.privateKeyFileLocation = privateKeyFileLocation;
        return this;
    }

    public MqttsnConnectorOptions withKeystoreLocation(String keystoreLocation){
        this.keystoreLocation = keystoreLocation;
        return this;
    }

    public MqttsnConnectorOptions withKeystorePassword(String keystorePassword){
        this.keystorePassword = keystorePassword;
        return this;
    }

    public MqttsnConnectorOptions withKeyPassword(String keyPassword){
        this.keyPassword = keyPassword;
        return this;
    }

    public MqttsnConnectorOptions withProtocol(String protocol){
        this.protocol = protocol;
        return this;
    }

    public MqttsnConnectorOptions withUsername(String username){
        this.username = username;
        return this;
    }

    public MqttsnConnectorOptions withPassword(String password){
        this.password = password;
        return this;
    }

    public MqttsnConnectorOptions withKeepAlive(int keepAlive){
        this.keepAlive = keepAlive;
        return this;
    }

    public MqttsnConnectorOptions withConnectionTimeout(int connectionTimeout){
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public MqttsnConnectorOptions withHostName(String hostName){
        this.hostName = hostName;
        return this;
    }

    public MqttsnConnectorOptions withPort(int port){
        this.port = port;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getKeepAlive() {
        return keepAlive;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getKeystoreLocation() {
        return keystoreLocation;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public String getCertificateFileLocation() {
        return certificateFileLocation;
    }

    public String getPrivateKeyFileLocation() {
        return privateKeyFileLocation;
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public String toString() {
        return "MqttsnConnectorOptions{" +
                "clientId='" + clientId + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", keepAlive=" + keepAlive +
                ", connectionTimeout=" + connectionTimeout +
                ", hostName='" + hostName + '\'' +
                ", port=" + port +
                ", protocol='" + protocol + '\'' +
                ", keystoreLocation='" + keystoreLocation + '\'' +
                ", keystorePassword='" + keystorePassword + '\'' +
                ", keyPassword='" + keyPassword + '\'' +
                ", certificateFileLocation='" + certificateFileLocation + '\'' +
                ", privateKeyFileLocation='" + privateKeyFileLocation + '\'' +
                '}';
    }
}
