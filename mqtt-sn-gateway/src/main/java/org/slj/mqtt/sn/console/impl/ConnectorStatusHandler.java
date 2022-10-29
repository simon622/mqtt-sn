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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slj.mqtt.sn.cloud.IMqttsnCloudService;
import org.slj.mqtt.sn.cloud.MqttsnConnectorDescriptor;
import org.slj.mqtt.sn.console.http.HttpConstants;
import org.slj.mqtt.sn.console.http.HttpException;
import org.slj.mqtt.sn.console.http.HttpInternalServerError;
import org.slj.mqtt.sn.console.http.IHttpRequestResponse;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;

import java.io.IOException;
import java.util.List;

public class ConnectorStatusHandler extends MqttsnConsoleAjaxRealmHandler {

    protected final IMqttsnCloudService cloudService;

    public ConnectorStatusHandler(IMqttsnCloudService cloudService, ObjectMapper mapper, IMqttsnRuntimeRegistry registry) {
        super(mapper, registry);
        this.cloudService = cloudService;
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse request) throws HttpException, IOException {

        try {
            List<MqttsnConnectorDescriptor> descriptors = cloudService.getAvailableConnectors();
            MqttsnConnectorDescriptor descriptor = getRegistry().getBackendService().getInstalledDescriptor(descriptors);

            boolean status = getRegistry().getBackendService().running();
            boolean connected = getRegistry().getBackendService().isConnected(null);
            String connectionString = getRegistry().getConnector().getConnectionString();

            StringBuilder sb = new StringBuilder();
            String cssClass = connected ? "connected" : status ? "partial" : "disconnected";
            sb.append("<div class=\"bg-"+cssClass+" connector-status\">");

            if(connected){
//                sb.append("<div class=\"spinner-grow text-success\" style=\"width: 3rem; height: 3rem;\" role=\"status\"><span class=\"visually-hidden\">..</span></div>");
            }

            sb.append("<p>");
            sb.append("<span class=\"text-lg\">");
            if(connected){
                sb.append("Connected to "+ descriptor.getName());
            } else {
                sb.append("Disconnected from "+ descriptor.getName());
            }
            sb.append("</span><br/>");
            sb.append("<span class=\"text-sm\">");
            sb.append(connectionString);
            sb.append("</span>");
            sb.append("</p>");
            sb.append("</div>");
            writeHTMLResponse(request, HttpConstants.SC_OK, sb.toString());

        } catch(Exception e){
            throw new HttpInternalServerError("error determining status", e);
        }
    }
}

