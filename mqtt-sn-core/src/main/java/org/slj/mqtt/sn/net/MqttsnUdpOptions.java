/*
 * Copyright (c) 2020 Simon Johnson <simon622 AT gmail DOT com>
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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.slj.mqtt.sn.net;

/**
 * Options to configure the behaviour of the UDP transport layer
 */
public class MqttsnUdpOptions {

    /**
     * Default max tranmission unit for UDP transport
     */
    public static int DEFAULT_MTU = 1024;

    /**
     * Default host binds to wildcard (kernel assigned)
     */
    public static String DEFAULT_LOCAL_BIND_INTERFACE = "0.0.0.0";

    /**
     * Default local port for client mode is 0 (any available local port) for clients.
     */
    public static int DEFAULT_LOCAL_CLIENT_PORT = 0;

    /**
     * Default local port is 2442 for gateways.
     */
    public static int DEFAULT_LOCAL_PORT = 2442;

    /**
     * Default local DTLS port is 2443 for gateways and unspecified (any available local port) for clients.
     */
    public static int DEFAULT_SECURE_PORT = 2443;

    /**
     * Default multicast port is 2224
     */
    public static int DEFAULT_BROADCAST_PORT = 2224;

    /**
     * Default receive buffer size is 1024
     */
    public static int DEFAULT_RECEIVE_BUFFER_SIZE = 1024;

    /**
     * Default receive buffer size is 1024
     */
    public static boolean DEFAULT_BIND_BROADCAST_LISTENER = false;

    String host = DEFAULT_LOCAL_BIND_INTERFACE;
    int port = DEFAULT_LOCAL_PORT;
    int mtu = DEFAULT_MTU;
    int securePort = DEFAULT_SECURE_PORT;
    int broadcastPort = DEFAULT_BROADCAST_PORT;
    int receiveBuffer = DEFAULT_RECEIVE_BUFFER_SIZE;
    boolean bindBroadcastListener = DEFAULT_BIND_BROADCAST_LISTENER;

    /**
     * Max allowable tranmission unit
     *
     * @see {@link MqttsnUdpOptions#DEFAULT_MTU} for default gateway value
     *
     * @param mtu - Mtu for the medium
     * @return this config
     */
    public MqttsnUdpOptions withMtu(int mtu){
        this.mtu = mtu;
        return this;
    }

    /**
     * Which local port to use for datagram messaging. When running in client mode, set 0 (default) which will mean
     * any available local port will be used.
     *
     * @see {@link MqttsnUdpOptions#DEFAULT_LOCAL_PORT} for default gateway value
     * @see {@link MqttsnUdpOptions#DEFAULT_LOCAL_CLIENT_PORT} for default client value
     *
     * @param port - Which local port to use for datagram messaging
     * @return this config
     */
    public MqttsnUdpOptions withPort(int port){
        this.port = port;
        return this;
    }

    /**
     * For the most part, the datagram binds to the local wildcard address (0.0.0.0) which will be decided
     * by the kernal. In certain implementations you can supply the host on which to bind here
     * @param host - the host to bind the listening socket to
     * @return this config
     */
    public MqttsnUdpOptions withHost(String host){
        this.host = host;
        return this;
    }

    /**
     * Which local port to use for secure datagram messaging. When running in client mode, set 0 (default) which will mean
     * any available local port will be used.
     *
     * @see {@link MqttsnUdpOptions#DEFAULT_SECURE_PORT} for default gateway value
     * @see {@link MqttsnUdpOptions#DEFAULT_LOCAL_CLIENT_PORT} for default client value
     *
     * @param port - Which local port to use for datagram messaging
     * @return this config
     */
    public MqttsnUdpOptions withSecurePort(int port){
        this.securePort = port;
        return this;
    }

    /**
     * Set the size of the transport buffer used to receive messages
     *
     * @see {@link MqttsnUdpOptions#DEFAULT_RECEIVE_BUFFER_SIZE}
     *
     * @param receiveBuffer - Set the size of the transport buffer used to receive messages
     * @return this config
     */
    public MqttsnUdpOptions withReceiveBuffer(int receiveBuffer){
        this.receiveBuffer = receiveBuffer;
        return this;
    }

    /**
     * Which local port to use for broadcast messaging
     *
     * @see {@link MqttsnUdpOptions#DEFAULT_BROADCAST_PORT} for default gateway
     * @see {@link MqttsnUdpOptions#DEFAULT_LOCAL_CLIENT_PORT} for default client value
     *
     * @param multicastPort - Which local port to use for multicast messaging
     * @return this config
     */
    public MqttsnUdpOptions withBroadcastPort(int multicastPort){
        this.broadcastPort = broadcastPort;
        return this;
    }

    /**
     * Should the runtime listen on the broadcast port for discovery messages
     *
     * @see {@link MqttsnUdpOptions#DEFAULT_BIND_BROADCAST_LISTENER} for default gateway
     *
     * @param broadcastListener - Should the runtime listen on the broadcast port for discovery messages
     * @return this config
     */
    public MqttsnUdpOptions withBindBroadcastListener(boolean broadcastListener){
        this.bindBroadcastListener = broadcastListener;
        return this;
    }

    public boolean getBindBroadcastListener() {
        return bindBroadcastListener;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getReceiveBuffer() {
        return receiveBuffer;
    }

    public int getBroadcastPort(){ return this.broadcastPort; }

    public int getSecurePort() {
        return securePort;
    }

    public int getMtu() { return mtu; }
}
