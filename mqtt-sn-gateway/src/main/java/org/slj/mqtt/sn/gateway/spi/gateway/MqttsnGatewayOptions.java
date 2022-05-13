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

package org.slj.mqtt.sn.gateway.spi.gateway;

import org.slj.mqtt.sn.model.MqttsnOptions;

import java.util.HashSet;
import java.util.Set;

public final class MqttsnGatewayOptions extends MqttsnOptions {

    /**
     * By default, any clientId ("*") will be allowed to connect to the gateway.
     */
    public static final String DEFAULT_CLIENT_ALLOWED_ALL = "*";

    /**
     * The default gatewayId used in advertise / discovery is 1
     */
    public static final int DEFAULT_GATEWAY_ID = 1;

    /**
     * A default gateway will allow 100 simultaneous connects to reside on the gateway
     */
    public static final int DEFAULT_MAX_CONNECTED_CLIENTS = 100;

    /**
     * The default advertise time is 60 seconds
     */
    public static final int DEFAULT_GATEWAY_ADVERTISE_TIME = 60;

    /**
     * The maximum number of publish operations per second that the gateway will send to the gateway - by default this is disabled and unlimited
     */
    public static final double DEFAULT_MAX_BROKER_PUBLISHES_PER_SECOND = 0d;

    private Set<String> allowedClientIds = new HashSet();
    {
        allowedClientIds.add(DEFAULT_CLIENT_ALLOWED_ALL);
    }

    private int maxConnectedClients = DEFAULT_MAX_CONNECTED_CLIENTS;
    private double maxBrokerPublishesPerSecond = DEFAULT_MAX_BROKER_PUBLISHES_PER_SECOND;
    private int gatewayAdvertiseTime = DEFAULT_GATEWAY_ADVERTISE_TIME;
    private int gatewayId = DEFAULT_GATEWAY_ID;

    public MqttsnGatewayOptions withMaxBrokerPublishesPerSecond(double maxBrokerPublishesPerSecond){
        this.maxBrokerPublishesPerSecond = maxBrokerPublishesPerSecond;
        return this;
    }

    public MqttsnGatewayOptions withMaxConnectedClients(int maxConnectedClients){
        this.maxConnectedClients = maxConnectedClients;
        return this;
    }

    public MqttsnGatewayOptions withGatewayId(int gatewayId){
        this.gatewayId = gatewayId;
        return this;
    }

    public MqttsnGatewayOptions withGatewayAdvertiseTime(int gatewayAdvertiseTime){
        this.gatewayAdvertiseTime = gatewayAdvertiseTime;
        return this;
    }

    public int getGatewayAdvertiseTime() {
        return gatewayAdvertiseTime;
    }

    public int getGatewayId() {
        return gatewayId;
    }

    public int getMaxConnectedClients() {
        return maxConnectedClients;
    }

    public Set<String> getAllowedClientIds() {
        return allowedClientIds;
    }

    public double getMaxBrokerPublishesPerSecond() {
        return maxBrokerPublishesPerSecond;
    }

    public MqttsnGatewayOptions withAllowedClientId(String clientId){

        //-- if the application specifies custom allow list, remove wildcard
        if(allowedClientIds.contains(DEFAULT_CLIENT_ALLOWED_ALL)){
            allowedClientIds.remove(DEFAULT_CLIENT_ALLOWED_ALL);
        }
        allowedClientIds.add(clientId);
        return this;
    }
}
