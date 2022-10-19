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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slj.mqtt.sn.console.http.HttpConstants;
import org.slj.mqtt.sn.console.http.HttpException;
import org.slj.mqtt.sn.console.http.HttpInternalServerError;
import org.slj.mqtt.sn.console.http.IHttpRequestResponse;
import org.slj.mqtt.sn.console.http.impl.AbstractHttpRequestResponseHandler;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConfigHandler extends AbstractHttpRequestResponseHandler {


    protected IMqttsnRuntimeRegistry registry;

    public ConfigHandler(ObjectMapper mapper, IMqttsnRuntimeRegistry registry) {
        super(mapper);
        this.registry = registry;
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse request) throws HttpException, IOException {
        try {
            PropertiesBean bean = populateBean();
            writeJSONBeanResponse(request, HttpConstants.SC_OK, bean);
        } catch(Exception e){
            throw new HttpInternalServerError("error populating bean;", e);
        }
    }


    protected PropertiesBean populateBean() throws MqttsnException {
        PropertiesBean bean = new PropertiesBean();
        MqttsnOptions options = registry.getOptions();
        bean.add("Context Id", options.getContextId(), "");
        bean.add("Transport Protocol Handoff Thread Count", options.getTransportProtocolHandoffThreadCount(), "");
        bean.add("Transport Publish Handoff Thread Count", options.getTransportPublishHandoffThreadCount(), "");
        bean.add("Queue Processor Thread Count", options.getQueueProcessorThreadCount(), "");
        bean.add("General Purpose Thread Count", options.getGeneralPurposeThreadCount(), "");
        bean.add("Queue Back Pressure", options.getQueueBackPressure(), "");
        bean.add("Enable Discovery", options.isEnableDiscovery(), "");
        bean.add("Sleep Clears Registrations", options.isSleepClearsRegistrations(), "");
        bean.add("Min Flush Time", options.getMinFlushTime(), "");
        bean.add("Max Topics In Registry", options.getMaxTopicsInRegistry(), "");
        bean.add("Msg Id Start At", options.getMsgIdStartAt(), "");
        bean.add("Alias Start At", options.getAliasStartAt(), "");
        bean.add("Max Messages Inflight", options.getMaxMessagesInflight(), "");
        bean.add("Max Messages In Queue", options.getMaxMessagesInQueue(), "");
        bean.add("Requeue On Inflight Timeout", options.isRequeueOnInflightTimeout(), "");
        bean.add("Max Network Address Entries", options.getMaxNetworkAddressEntries(), "");
        bean.add("Max Wait", options.getMaxWait(), "");
        bean.add("Max Time Inflight", options.getMaxTimeInflight(), "");
        bean.add("Search Gateway Radius", options.getSearchGatewayRadius(), "");
        bean.add("Discovery Time", options.getDiscoveryTime(), "");
        bean.add("Ping Divisor", options.getPingDivisor(), "");
        bean.add("Max Protocol Message Size", options.getMaxProtocolMessageSize(), "");
        bean.add("Is Wire Logging Enabled", options.isWireLoggingEnabled(), "");
        bean.add("Active Context Timeout", options.getActiveContextTimeout(), "");
        bean.add("State Loop Timeout", options.getStateLoopTimeout(), "");
        bean.add("Log Pattern", options.getLogPattern(), "");
        bean.add("Max Error Retries", options.getMaxErrorRetries(), "");
        bean.add("Max Error Retry Time", options.getMaxErrorRetryTime(), "");
        bean.add("Congestion Wait", options.getCongestionWait(), "");
        bean.add("Remove Disconnected Sessions Seconds", options.getRemoveDisconnectedSessionsSeconds(), "");
        bean.add("Reap Receiving Messages", options.isReapReceivingMessages(), "");
        bean.add("Metrics Enabled", options.isMetricsEnabled(), "");
        return bean;
    }

    class PropertiesBean {

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = HttpConstants.DEFAULT_DATE_FORMAT)
        public Date generatedAt;
        public List<PropertyBean> properties;

        public void add(String name, Boolean value, String description){
            add(name, String.valueOf(value), description);
        }

        public void add(String name, Integer value, String description){
            add(name, String.valueOf(value), description);
        }

        public void add(String name, Long value, String description){
            add(name, String.valueOf(value), description);
        }

        public void add(String name, String value, String description){
            if(properties == null)
                properties = new ArrayList<>();
            PropertyBean bean = new PropertyBean();
            bean.name = name;
            bean.value = value;
            bean.description = description;
            bean.editable = false;
            properties.add(bean);
        }
    }

    class PropertyBean {

        public String name;
        public String value;
        public String description;
        public boolean editable = false;
    }
}
