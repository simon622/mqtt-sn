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

/**
 * Provides some pre-configured default configurations tailored to different deployment models
 */
public final class MqttsnGatewayPerformanceProfile {

    /**
     * For use in typical gateway deployments on site. Will limit CPU use while respecting incoming device traffic
     */
    public static MqttsnGatewayPerformanceProfile BALANCED_GATEWAY_GENERAL_PURPOSE =
            new MqttsnGatewayPerformanceProfile(5, 2, 1, 1, 250, 100, 25, 100);

    /**
     * For use in typical cloud deployments. Balanced between protocol responsiveness and outbound messaging from backend
     */
    public static MqttsnGatewayPerformanceProfile BALANCED_CLOUD_GENERAL_PURPOSE =
            new MqttsnGatewayPerformanceProfile(20, 10, 1, 2, 25, 5000, 50, 1000);

    /**
     * For use in cloud deployments where number of connecting devices and publish messages is higher than egress traffic.
     */
    public static MqttsnGatewayPerformanceProfile INGRESS_CLOUD =
            new MqttsnGatewayPerformanceProfile(40, 5, 1, 2, 50, 10000, 25, 5000);

    /**
     * For use in cloud deployments where data going out to devices is of high importance
     */
    public static MqttsnGatewayPerformanceProfile EGRESS_CLOUD =
            new MqttsnGatewayPerformanceProfile(10, 180, 8, 2, 5, 5000, 2500, 25000);


    private int transportProtocolHandoffThreadCount;
    private int transportPublishHandoffThreadCount;
    private int queueProcessorThreadCount;
    private int generalPurposeThreadCount;
    private int minFlushTime;
    private int maxConnectedClients;
    private int maxSessionQueueSize;
    private int maxBackendQueueSize;


    private MqttsnGatewayPerformanceProfile(int transportProtocolHandoffThreadCount, int transportPublishHandoffThreadCount, int queueProcessorThreadCount, int generalPurposeThreadCount, int minFlushTime, int maxConnectedClients, int maxSessionQueueSize, int maxBackendQueueSize) {
        this.transportProtocolHandoffThreadCount = transportProtocolHandoffThreadCount;
        this.transportPublishHandoffThreadCount = transportPublishHandoffThreadCount;
        this.queueProcessorThreadCount = queueProcessorThreadCount;
        this.generalPurposeThreadCount = generalPurposeThreadCount;
        this.minFlushTime = minFlushTime;
        this.maxConnectedClients = maxConnectedClients;
        this.maxSessionQueueSize = maxSessionQueueSize;
        this.maxBackendQueueSize = maxBackendQueueSize;
    }

    public int getMaxSessionQueueSize() {
        return maxSessionQueueSize;
    }

    public int getMaxBackendQueueSize() {
        return maxBackendQueueSize;
    }

    public int getTransportProtocolHandoffThreadCount() {
        return transportProtocolHandoffThreadCount;
    }

    public int getTransportPublishHandoffThreadCount() {
        return transportPublishHandoffThreadCount;
    }

    public int getQueueProcessorThreadCount() {
        return queueProcessorThreadCount;
    }

    public int getGeneralPurposeThreadCount() {
        return generalPurposeThreadCount;
    }

    public int getMinFlushTime() {
        return minFlushTime;
    }

    public int getMaxConnectedClients() {
        return maxConnectedClients;
    }
}
