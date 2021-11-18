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

package org.slj.mqtt.sn.net;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

public class MqttsnTcpOptions {

    /**
     * Default connect timeout is 10000 milliseconds
     */
    public static int DEFAULT_CONNECT_TIMEOUT = 10000;

    /**
     * Default socket timeout is 0 (infinte)
     */
    public static int DEFAULT_SO_TIMEOUT = 0;

    /**
     * By default TCP keep alive is enabled
     */
    public static boolean DEFAULT_TCP_KEEPALIVE_ENABLED = true;

    /**
     * Default port is 1883 for gateways.
     */
    public static int DEFAULT_PORT = 1883;

    /**
     * Default secure port is 8883 for gateways.
     */
    public static int DEFAULT_SECURE_PORT = 8883;

    /**
     * Default localhost is null
     */
    public static String DEFAULT_HOST = null;

    /**
     * Default SSL cipher suites to enable
     */
    public static String[] DEFAULT_SSL_CIPHER_SUITES = null;
//    `new String[] {
//            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
//            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
//            "TLS_RSA_WITH_AES_256_CBC_SHA",
//            "TLS_RSA_WITH_AES_256_CBC_SHA256"
//    };

    /**
     * Default SSL protocols
     */
    public static String[] DEFAULT_SSL_PROTOCOLS = new String[] {
            "TLSv1","TLSv1.1","TLSv1.2"
    };

    /**
     * Default SSL protocols
     */
    public static boolean DEFAULT_IS_SECURE = false;

    /**
     * Default max concurrent client connections is 100
     */
    public static int DEFAULT_MAX_CLIENT_CONNECTIONS = 100;

    /**
     * Default keystore password is unset
     */
    public static String DEFAULT_KEYSTORE_PASSWORD = "password";

    /**
     * Default read buffer size is 1024
     */
    public static int DEFAULT_READ_BUFFER_SIZE = 1024;

    /**
     * Default read buffer size is 1024
     */
    public static String DEFAULT_SSL_ALGORITHM = "TLS";

    SocketFactory clientSocketFactory = SocketFactory.getDefault();
    ServerSocketFactory serverSocketFactory = ServerSocketFactory.getDefault();

    String host = DEFAULT_HOST;
    int port = DEFAULT_PORT;
    int securePort = DEFAULT_SECURE_PORT;

    int maxClientConnections = DEFAULT_MAX_CLIENT_CONNECTIONS;
    int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    int soTimeout = DEFAULT_SO_TIMEOUT;
    boolean tcpKeepAliveEnabled = DEFAULT_TCP_KEEPALIVE_ENABLED;
    int readBufferSize = DEFAULT_READ_BUFFER_SIZE;

    //-- SSL stuff
    boolean secure = DEFAULT_IS_SECURE;
    String[] sslProtocols = DEFAULT_SSL_PROTOCOLS;
    String[] cipherSuites = DEFAULT_SSL_CIPHER_SUITES;
    String keyStorePath = null;
    String trustStorePath = null;
    String keyStorePassword = DEFAULT_KEYSTORE_PASSWORD;
    String trustStorePassword = DEFAULT_KEYSTORE_PASSWORD;
    String sslAlgorithm = DEFAULT_SSL_ALGORITHM;

    public MqttsnTcpOptions withSecure(boolean secure){
        this.secure = secure;
        return this;
    }

    public MqttsnTcpOptions withSSLAlgorithm(String sslAlgorithm){
        this.sslAlgorithm = sslAlgorithm;
        return this;
    }

    public MqttsnTcpOptions withReadBufferSize(int readBufferSize){
        this.readBufferSize = readBufferSize;
        return this;
    }

    public MqttsnTcpOptions withKeystorePassword(String keyStorePassword){
        this.keyStorePassword = keyStorePassword;
        return this;
    }

    public MqttsnTcpOptions withTruststorePassword(String trustStorePassword){
        this.trustStorePassword = trustStorePassword;
        return this;
    }

    public MqttsnTcpOptions withTruststorePath(String trustStorePath){
        this.trustStorePath = trustStorePath;
        return this;
    }

    public MqttsnTcpOptions withKeystorePath(String keyStorePath){
        this.keyStorePath = keyStorePath;
        return this;
    }

    public MqttsnTcpOptions withMaxClientConnections(int maxClientConnections){
        this.maxClientConnections = maxClientConnections;
        return this;
    }

    public MqttsnTcpOptions withSSLProtocols(String[] sslProtocols){
        this.sslProtocols = sslProtocols;
        return this;
    }

    public MqttsnTcpOptions withCipherSuites(String[] cipherSuites){
        this.cipherSuites = cipherSuites;
        return this;
    }

    public MqttsnTcpOptions withTcpKeepAliveEnabled(boolean keepAliveEnabled){
        this.tcpKeepAliveEnabled = keepAliveEnabled;
        return this;
    }

    public MqttsnTcpOptions withSoTimeout(int soTimeout){
        this.soTimeout = soTimeout;
        return this;
    }

    public MqttsnTcpOptions withConnectTimeout(int connectTimeout){
        this.connectTimeout = connectTimeout;
        return this;
    }

    public MqttsnTcpOptions withHost(String host){
        this.host = host;
        return this;
    }

    public MqttsnTcpOptions withPort(int port){
        this.port = port;
        return this;
    }

    public MqttsnTcpOptions withSecurePort(int securePort){
        this.securePort = securePort;
        return this;
    }

    public MqttsnTcpOptions withServerSocketFactory(ServerSocketFactory serverSocketFactory){
        this.serverSocketFactory = serverSocketFactory;
        return this;
    }

    public MqttsnTcpOptions withSocketFactory(SocketFactory clientSocketFactory){
        this.clientSocketFactory = clientSocketFactory;
        return this;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public boolean isTcpKeepAliveEnabled() {
        return tcpKeepAliveEnabled;
    }

    public String[] getSslProtocols() {
        return sslProtocols;
    }

    public String[] getCipherSuites() {
        return cipherSuites;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isSecure() {
        return secure;
    }

    public int getMaxClientConnections() {
        return maxClientConnections;
    }

    public SocketFactory getClientSocketFactory() {
        return clientSocketFactory;
    }

    public ServerSocketFactory getServerSocketFactory() {
        return serverSocketFactory;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public int getSecurePort() {
        return securePort;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public String getSslAlgorithm() {
        return sslAlgorithm;
    }
}
