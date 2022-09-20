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

package org.slj.mqtt.sn.gateway.spi.broker;

import java.util.Objects;

public class MqttsnBackendOptions {

    public static final boolean DEFAULT_CONNECT_ON_STARTUP = true;
    public static final boolean DEFAULT_MANAGED_CONNECTIONS = true;
    public static final int DEFAULT_KEEPALIVE = 30 * 10;
    public static final int DEFAULT_CONNECTION_TIMEOUT = 30;
    public static final int DEFAULT_MQTT_PORT = 1883;
    public static final int DEFAULT_MQTT_TLS_PORT = 8883;
    public static final String DEFAULT_MQTT_PROTOCOL = "tcp";
    public static final String DEFAULT_MQTT_TLS_PROTOCOL = "ssl";

    private boolean connectOnStartup = DEFAULT_CONNECT_ON_STARTUP;
    private boolean managedConnections = DEFAULT_MANAGED_CONNECTIONS;
    private String username;
    private String password;
    private int keepAlive = DEFAULT_KEEPALIVE;
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private String host;
    private int port = DEFAULT_MQTT_PORT;
    private String protocol = DEFAULT_MQTT_PROTOCOL;

    private String keystoreLocation = null;
    private String keystorePassword = null;
    private String keyPassword = null;

    private String certificateFileLocation = null;
    private String privateKeyFileLocation = null;

    public MqttsnBackendOptions(){

    }

    public MqttsnBackendOptions withCertificateFileLocation(String certificateFileLocation){
        this.certificateFileLocation = certificateFileLocation;
        return this;
    }

    public MqttsnBackendOptions withPrivateKeyFileLocation(String privateKeyFileLocation){
        this.privateKeyFileLocation = privateKeyFileLocation;
        return this;
    }

    public MqttsnBackendOptions withKeystoreLocation(String keystoreLocation){
        this.keystoreLocation = keystoreLocation;
        return this;
    }

    public MqttsnBackendOptions withKeystorePassword(String keystorePassword){
        this.keystorePassword = keystorePassword;
        return this;
    }

    public MqttsnBackendOptions withKeyPassword(String keyPassword){
        this.keyPassword = keyPassword;
        return this;
    }

    public MqttsnBackendOptions withConnectOnStartup(boolean connectOnStartup){
        this.connectOnStartup = connectOnStartup;
        return this;
    }

    public MqttsnBackendOptions withManagedConnections(boolean managedConnections){
        this.managedConnections = managedConnections;
        return this;
    }

    public MqttsnBackendOptions withProtocol(String protocol){
        this.protocol = protocol;
        return this;
    }

    public MqttsnBackendOptions withUsername(String username){
        this.username = username;
        return this;
    }

    public MqttsnBackendOptions withPassword(String password){
        this.password = password;
        return this;
    }

    public MqttsnBackendOptions withKeepAlive(int keepAlive){
        this.keepAlive = keepAlive;
        return this;
    }

    public MqttsnBackendOptions withConnectionTimeout(int connectionTimeout){
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public MqttsnBackendOptions withHost(String host){
        this.host = host;
        return this;
    }

    public MqttsnBackendOptions withPort(int port){
        this.port = port;
        return this;
    }

    public boolean getManagedConnections() {
        return managedConnections;
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

    public String getHost() {
        return host;
    }

    public boolean getConnectOnStartup() {
        return connectOnStartup;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean validConnectionDetails(){
        return !nonEmpty(protocol) && !nonEmpty(host) && port > 0;
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

    static boolean nonEmpty(String val){
        return !Objects.isNull(val) && "".equals(val.trim());
    }

    @Override
    public String toString() {
        return "MqttsnBrokerOptions{" +
                "connectOnStartup=" + connectOnStartup +
                ", managedConnections=" + managedConnections +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", keepAlive=" + keepAlive +
                ", connectionTimeout=" + connectionTimeout +
                ", host='" + host + '\'' +
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
