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

package org.slj.mqtt.sn.console.impl;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.slj.mqtt.sn.console.http.IHttpRequestResponse;
import org.slj.mqtt.sn.console.http.impl.handlers.AsyncFieldHandler;
import org.slj.mqtt.sn.gateway.spi.GatewayMetrics;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.impl.metrics.IMqttsnMetrics;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;

public class ConsoleAsyncMetricFieldHandler extends AsyncFieldHandler {

    private IMqttsnRuntimeRegistry registry;

    public ConsoleAsyncMetricFieldHandler(ObjectWriter writer, IMqttsnRuntimeRegistry registry) {
        super(writer);
        this.registry = registry;
    }

    @Override
    protected String getValue(IHttpRequestResponse request, String fieldName) {

        switch(fieldName){
            case "currentSessions":
                return safeValue(registry.getSessionRegistry().countTotalSessions());
            case "totalSessions":
                int max = ((MqttsnGatewayOptions)registry.getOptions()).getMaxConnectedClients();
                return safeValue(new Long(max));
            case "currentEgress":
                return safeValue(registry.getMetrics().getMetric(IMqttsnMetrics.PUBLISH_MESSAGE_OUT).getLastSample().getLongValue());
            case "currentIngress":
                return safeValue(registry.getMetrics().getMetric(IMqttsnMetrics.PUBLISH_MESSAGE_IN).getLastSample().getLongValue());
            case "currentConnector":
                return safeValue(registry.getMetrics().getMetric(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH).getLastSample().getLongValue());
            case "totalConnector":
                double permits = ((MqttsnGatewayOptions)registry.getOptions()).getMaxBrokerPublishesPerSecond();
                return permits == 0 ? "&infin;" : safeValue(new Long((int) permits));
        }

        return "NaN";
    }

    protected static String safeValue(Long val){
        return val == null ? "0" : val.toString();
    }
}
