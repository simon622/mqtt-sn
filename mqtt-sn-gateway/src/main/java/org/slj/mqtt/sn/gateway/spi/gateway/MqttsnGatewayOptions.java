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

import java.util.Set;

public final class MqttsnGatewayOptions extends MqttsnOptions {

    /**
     * The default gatewayId used in advertise / discovery is 1
     */
    public static final int DEFAULT_GATEWAY_ID = 1;

    /**
     * A default gateway will allow 100 simultaneous connects to reside on the gateway
     */
    public static final int DEFAULT_MAX_CLIENT_SESSIONS = 100;

    /**
     * The default advertise time is 60 seconds
     */
    public static final int DEFAULT_GATEWAY_ADVERTISE_TIME = 60;

    /**
     * The maximum number of publish operations per second that the gateway will send to the gateway -
     * By default this is disabled and unlimited (0)
     */
    public static final double DEFAULT_MAX_BROKER_PUBLISHES_PER_SECOND = 0d;

    /**
     * The maximum number of publish operations queued up to send to backend
     */
    public static final int DEFAULT_MAX_BACKEND_QUEUE_SIZE = 10000;

    public int maxClientSessions = DEFAULT_MAX_CLIENT_SESSIONS;
    public double maxBrokerPublishesPerSecond = DEFAULT_MAX_BROKER_PUBLISHES_PER_SECOND;

    public int maxBackendQueueSize = DEFAULT_MAX_BACKEND_QUEUE_SIZE;

    public int gatewayAdvertiseTime = DEFAULT_GATEWAY_ADVERTISE_TIME;
    public int gatewayId = DEFAULT_GATEWAY_ID;

    public MqttsnGatewayOptions withMaxBrokerPublishesPerSecond(double maxBrokerPublishesPerSecond){
        this.maxBrokerPublishesPerSecond = maxBrokerPublishesPerSecond;
        return this;
    }

    public MqttsnGatewayOptions withMaxClientSessions(int maxClientSessions){
        this.maxClientSessions = maxClientSessions;
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

    public int getMaxClientSessions() {
        return maxClientSessions;
    }

    public Set<String> getAllowedClientIds() {
        return getClientCredentials().getClientIdTokens();
    }

    public double getMaxBrokerPublishesPerSecond() {
        return maxBrokerPublishesPerSecond;
    }

    public int getMaxBackendQueueSize() {
        return maxBackendQueueSize;
    }

    public MqttsnGatewayOptions withAllowedClientId(String clientId){
        super.getClientCredentials().addAllowedClientId(clientId, clientId);
        return this;
    }

    public MqttsnGatewayOptions withMaxBackendQueueSize(int maxBackendQueueSize){
        this.maxBackendQueueSize = maxBackendQueueSize;
        return this;
    }

    public void withPerformanceProfile(MqttsnGatewayPerformanceProfile profile){
        withMaxClientSessions(profile.getMaxConnectedClients());
        withMaxMessagesInQueue(profile.getMaxSessionQueueSize());
        withTransportIngressThreadCount(profile.getTransportProtocolHandoffThreadCount());
        withTransportEgressThreadCount(profile.getTransportPublishHandoffThreadCount());
        withGeneralPurposeThreadCount(profile.getGeneralPurposeThreadCount());
        withQueueProcessorThreadCount(profile.getQueueProcessorThreadCount());
        withMinFlushTime(profile.getMinFlushTime());
        withMaxBackendQueueSize(profile.getMaxBackendQueueSize());
    }
}
