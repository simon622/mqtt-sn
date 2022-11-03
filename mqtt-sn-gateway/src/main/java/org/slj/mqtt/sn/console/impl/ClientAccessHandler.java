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
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.console.http.HttpConstants;
import org.slj.mqtt.sn.console.http.HttpException;
import org.slj.mqtt.sn.console.http.HttpInternalServerError;
import org.slj.mqtt.sn.console.http.IHttpRequestResponse;
import org.slj.mqtt.sn.model.MqttsnClientCredentials;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.io.IOException;

public class ClientAccessHandler extends MqttsnConsoleAjaxRealmHandler {

    public ClientAccessHandler(ObjectMapper mapper, IMqttsnRuntimeRegistry registry) {
        super(mapper, registry);
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse request) throws HttpException, IOException {
        try {
            MqttsnClientCredentials creds = registry.getOptions().getClientCredentials();
            boolean modified = false;
            if(null != getParameter(request, "removeClientId")){
                String clientId = getParameter(request, "removeClientId");
                modified |= creds.removeClientId(clientId);
                writeMessageBeanResponse(request, HttpConstants.SC_OK, new Message("Removed Client Identifier from runtime (if existed)", modified));
            } else if (null != getParameter(request, "removeCreds")){
                String username = getParameter(request, "removeCreds");
                modified |= creds.removeUsername(username);
                writeMessageBeanResponse(request, HttpConstants.SC_OK, new Message("Removed credentials from runtime (if existed)", modified));
            }
            else if (null != getParameter(request, "enableWhitelist")){
                String enableWhitelist = getParameter(request, "enableWhitelist");
                boolean allowAllClientIds = !Boolean.parseBoolean(enableWhitelist);
                creds.setAllowAllClientIds(allowAllClientIds);
                modified = true;
                writeSimpleOKResponse(request);
//                writeMessageBeanResponse(request, HttpConstants.SC_OK,
//                        new Message("Changed whitelist settings", modified));
            }

            else {
                MqttsnClientCredentials bean = populateBean(creds);
                writeJSONBeanResponse(request, HttpConstants.SC_OK, bean);
            }
            if(modified){
                registry.getStorageService().writeRuntimeOptions(registry.getOptions());
            }
        } catch(Exception e){
            throw new HttpInternalServerError("error populating bean;", e);
        }
    }

    @Override
    protected void handleHttpPost(IHttpRequestResponse request) throws HttpException, IOException {

        try {
            MqttsnClientCredentials creds = registry.getOptions().getClientCredentials();
            boolean modified = false;

            if("clientId".equals(getMandatoryParameter(request, "type"))){
                ClientIdForm form = readRequestBody(request, ClientIdForm.class);
                String clientId = form.name;
                if(MqttsnSpecificationValidator.validClientId(clientId, false, 23)){
                    modified |= creds.addAllowedClientId(clientId,
                            registry.getClientIdFactory().createClientId(clientId));
                    writeMessageBeanResponse(request, HttpConstants.SC_OK, new Message("New Client Identifier added to allow list", true));
                } else {
                    writeMessageBeanResponse(request, HttpConstants.SC_OK, new Message("Client Identifier was incorrect format", false));
                }
            } else if("creds".equals(getMandatoryParameter(request, "type"))){
                CredsForm form = readRequestBody(request, CredsForm.class);
                modified |= creds.addCredentials(form.username, form.password);
                writeMessageBeanResponse(request, HttpConstants.SC_OK, new Message("New credentials pair added to allow list", true));
            }

            if(modified){
                registry.getStorageService().writeRuntimeOptions(registry.getOptions());
            }

        } catch(MqttsnException e){
            throw new HttpInternalServerError("error creating client Id", e);
        }
    }

    protected MqttsnClientCredentials populateBean(MqttsnClientCredentials creds) {
        return creds;
    }



}

class ClientIdForm {

    public ClientIdForm() {
    }

    public String name;
}

class CredsForm {

    public CredsForm() {
    }

    public String username;
    public String password;
}
