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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slj.mqtt.sn.cloud.IMqttsnCloudService;
import org.slj.mqtt.sn.cloud.MqttsnCloudAccount;
import org.slj.mqtt.sn.cloud.MqttsnCloudServiceException;
import org.slj.mqtt.sn.cloud.MqttsnCloudToken;
import org.slj.mqtt.sn.console.IMqttsnConsole;
import org.slj.mqtt.sn.console.http.HttpConstants;
import org.slj.mqtt.sn.console.http.HttpException;
import org.slj.mqtt.sn.console.http.HttpInternalServerError;
import org.slj.mqtt.sn.console.http.IHttpRequestResponse;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.utils.Environment;
import org.slj.mqtt.sn.utils.MqttsnUtils;

import java.io.IOException;

public class CloudHandler extends MqttsnConsoleAjaxRealmHandler {

    protected IMqttsnCloudService cloudService;

    public CloudHandler(IMqttsnCloudService cloudService, ObjectMapper mapper, IMqttsnRuntimeRegistry registry) {
        super(mapper, registry);
        this.cloudService = cloudService;
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse request) throws HttpException, IOException {
        try {

            if("true".equals(getParameter(request, "dereg"))){

                getRegistry().getStorageService().setStringPreference(IMqttsnCloudService.CLOUD_TOKEN, null);
                cloudService.setToken(null);
                writeMessageBeanResponse(request, HttpConstants.SC_OK,
                        new Message("Success", "Your details were de-registered from the cloud."));

            } else {
                getRegistry().getService(IMqttsnConsole.class).authorizeCloud();
                MqttsnCloudAccount account = null;
                if(cloudService.isAuthorized()){
                    try {
                        account = cloudService.readAccount();
                        if(account != null){
                            writeJSONBeanResponse(request, HttpConstants.SC_OK, account);
                        }
                    } catch(MqttsnCloudServiceException e){
                        // account could not be read - uninitialise
                        getRegistry().getStorageService().setStringPreference(IMqttsnCloudService.CLOUD_TOKEN, null);
                        cloudService.setToken(null);
                        writeMessageBeanResponse(request, HttpConstants.SC_OK,
                                new Message("Error", "The cloud service could not find your account, please reinitialise"));
                    }
                } else {
                    //-- no value in prefs
                    writeSimpleOKResponse(request);
                }
            }

        }
        catch(Exception e){
            throw new HttpInternalServerError("error populating bean;", e);
        }
    }


    @Override
    protected void handleHttpPost(IHttpRequestResponse request) throws HttpException, IOException {
        try {
            CloudForm form = readRequestBody(request, CloudForm.class);
            String mac = Environment.getLocalHostMacAddress();
            MqttsnCloudToken token = cloudService.registerAccount(
                    form.emailAddress.trim().toLowerCase(),
                    MqttsnUtils.upperCaseWords(form.firstName),
                    MqttsnUtils.upperCaseWords(form.lastName),
                    MqttsnUtils.upperCaseWords(form.companyName),
                    mac, getRegistry().getOptions().getContextId());

            if(token != null){
                boolean existed = cloudService.isAuthorized();
                cloudService.setToken(token);
                getRegistry().getStorageService().setStringPreference(IMqttsnCloudService.CLOUD_TOKEN, token.getToken());
                if(existed){
                    writeMessageBeanResponse(request, HttpConstants.SC_OK,
                            new Message("Successfully updated", "Your details were updated in your cloud account"));
                } else {
                    writeMessageBeanResponse(request, HttpConstants.SC_OK,
                            new Message("Successfully registered", "A new cloud account was linked to this installation, please check your inbox for verification email."));
                }
            } else {
                writeMessageBeanResponse(request, HttpConstants.SC_OK,
                        new Message("Error creating account", "Unable to create / update cloud account", false));
            }
        } catch(Exception e){
            throw new HttpInternalServerError("error posting bean;", e);
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CloudForm {

    public CloudForm() {
    }

    public String firstName;
    public String lastName;
    public String emailAddress;
    public String password;
    public String companyName;
    public String country;

}
