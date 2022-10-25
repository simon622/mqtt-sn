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

package org.slj.mqtt.sn.cloud.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slj.mqtt.sn.cloud.*;
import org.slj.mqtt.sn.cloud.client.MqttsnCloudServiceException;
import org.slj.mqtt.sn.cloud.client.http.HttpClient;
import org.slj.mqtt.sn.cloud.client.http.HttpResponse;
import org.slj.mqtt.sn.utils.General;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpCloudServiceImpl implements IMqttsnCloudService {

    static Logger logger = Logger.getLogger(HttpCloudServiceImpl.class.getName());

    protected MqttsnCloudToken cloudToken;

    protected String serviceDiscoveryEndpoint;
    protected int connectTimeoutMillis;
    protected int readTimeoutMillis;
    protected ObjectMapper mapper;
    protected volatile boolean hasConnectivity;

    private volatile List<MqttsnCloudServiceDescriptor> descriptors;

    public HttpCloudServiceImpl(ObjectMapper mapper, String serviceDiscoveryEndpoint, int connectTimeoutMillis, int readTimeoutMillis){
        this.mapper = mapper;
        this.serviceDiscoveryEndpoint = serviceDiscoveryEndpoint;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;

        //check connectivity quietly
        try {
            HttpResponse response = HttpClient.head(getHeaders(), serviceDiscoveryEndpoint, readTimeoutMillis);
            hasConnectivity = !response.isError();
            logger.log(Level.INFO, String.format("successfully established connection to cloud provider [%s], online", serviceDiscoveryEndpoint));
        } catch(IOException e){
            hasConnectivity = false;
            logger.log(Level.WARNING, String.format("connection to cloud provider [%s] could not be established, offline", serviceDiscoveryEndpoint));
        }
    }

    @Override
    public MqttsnCloudToken authorizeCloudAccount(MqttsnCloudAccount account)
            throws MqttsnCloudServiceException {
        //-- ALWAYS re-authenticates with the cloud service
        cloudToken = httpGet(
                loadDescriptor(
                        MqttsnCloudServiceDescriptor.ACCOUNT_AUTHORIZE).getServiceEndpoint(),
                MqttsnCloudToken.class);
        return cloudToken;
    }

    @Override
    public List<MqttsnConnectorDescriptor> getAvailableConnectors() throws MqttsnCloudServiceException {
        checkConnectivity();
        List<MqttsnConnectorDescriptor> connectors =
                httpGetList(
                        loadDescriptor(
                            MqttsnCloudServiceDescriptor.CONNECTOR_LISTING).getServiceEndpoint(),
                        MqttsnConnectorDescriptor.class);
        return connectors;
    }
    @Override
    public int getConnectedServiceCount() throws MqttsnCloudServiceException {
        checkConnectivity();
        return getServiceDescriptors().size();
    }

    public MqttsnCloudAccount registerAccount(String emailAddress, String firstName, String lastName, String companyName) throws MqttsnCloudServiceException {

        if(!General.validEmailAddress(emailAddress))
            throw new MqttsnCloudServiceException("invalid email entered; must confrom to OWASP guidelines");

        AccountCreate create = new AccountCreate();
        create.emailAddress = emailAddress;
        create.companyName = companyName;
        create.firstName = firstName;
        create.lastName = lastName;

        MqttsnCloudAccount account = httpPost(
                loadDescriptor(
                        MqttsnCloudServiceDescriptor.ACCOUNT_CREATE).getServiceEndpoint(),
                MqttsnCloudAccount.class, create);
        return account;
    }


    protected MqttsnCloudServiceDescriptor loadDescriptor(String name) throws MqttsnCloudServiceException {
        Optional<MqttsnCloudServiceDescriptor> descriptor =
                getServiceDescriptors().stream().filter(d -> name.equals(d.getServiceName())).findFirst();
        if(!descriptor.isPresent())
            throw new MqttsnCloudServiceException("cloud service not available on endpoint " + name);
        return descriptor.get();
    }

    protected List<MqttsnCloudServiceDescriptor> getServiceDescriptors() throws MqttsnCloudServiceException {
        loadDescriptorsInternal();
        return descriptors;
    }

    protected List<MqttsnCloudServiceDescriptor> loadDescriptorsInternal() throws MqttsnCloudServiceException {
        if(descriptors == null){
            synchronized (this){
                if(descriptors == null){
                    MqttsnCloudServiceDescriptor[] descriptorsArray =
                            httpGet(serviceDiscoveryEndpoint, MqttsnCloudServiceDescriptor[].class);
                    descriptors = Arrays.asList(descriptorsArray);
                }
            }
        }
        logger.log(Level.INFO, "mqtt-sn cloud provider has " + descriptors.size() + " available");
        return descriptors;
    }

    protected void checkResponse(HttpResponse response, boolean expectPayload) throws MqttsnCloudServiceException {
        if(response == null)
            throw new MqttsnCloudServiceException("cloud service ["+response.getRequestUrl()+"] failed to respond <null>");
        if(response.isError())
            throw new MqttsnCloudServiceException("cloud service ["+response.getRequestUrl()+"] failed ("+response.getStatusCode()+" - "+response.getStatusMessage()+")");
        if(expectPayload && response.getContentLength() <= 0){
            throw new MqttsnCloudServiceException("could service ["+response.getRequestUrl()+"] failed with no expected content ("+response.getStatusCode()+" - "+response.getStatusMessage()+")");
        }
    }

    protected void checkConnectivity() throws MqttsnCloudServiceException {
        if(!hasConnectivity){
            logger.log(Level.WARNING, "mqtt-sn cloud provider is offline");
            throw new MqttsnCloudServiceException("mqtt-sn cloud provider is unavailable at this time");
        }
    }

    @Override
    public boolean hasCloudConnectivity() {
        return hasConnectivity;
    }

    @Override
    public boolean isAuthorized() {
        return cloudToken != null;
    }

    protected  <T> List<T> httpGetList(String url, Class<? extends T> cls) throws MqttsnCloudServiceException {
        try {
            HttpResponse response =
                    HttpClient.get(getHeaders(), url, connectTimeoutMillis, readTimeoutMillis);
            logger.log(Level.INFO, String.format("obtaining cloud service list from [%s] -> [%s]", url, response));
            checkResponse(response, true);
            return mapper.readValue(response.getResponseBody(),
                    mapper.getTypeFactory().constructCollectionType(List.class, cls));
        } catch(IOException e){
            throw new MqttsnCloudServiceException("error in http socket connection", e);
        }
    }

    protected  <T> T httpGet(String url, Class<? extends T> cls) throws MqttsnCloudServiceException {
        try {
            HttpResponse response =
                    HttpClient.get(getHeaders(), url, connectTimeoutMillis, readTimeoutMillis);
            logger.log(Level.INFO, String.format("obtaining cloud service object from [%s] -> [%s]", url, response));
            checkResponse(response, true);
            return mapper.readValue(response.getResponseBody(), cls);
        } catch(IOException e){
            throw new MqttsnCloudServiceException("error in http socket connection", e);
        }
    }

    protected  <T> T httpPost(String url, Class<? extends T> cls, Object jsonPostObject) throws MqttsnCloudServiceException {
        try {
            String jsonBody = mapper.writeValueAsString(jsonPostObject);
            try (InputStream is = new ByteArrayInputStream(jsonBody.getBytes())) {
                HttpResponse response =
                        HttpClient.post(getHeaders(), url, is, connectTimeoutMillis, readTimeoutMillis);
                logger.log(Level.INFO, String.format("posting to cloud service object from [%s] -> [%s]", url, response));
                checkResponse(response, true);
                return mapper.readValue(response.getResponseBody(), cls);
            }
        } catch(IOException e){
            throw new MqttsnCloudServiceException("error in http socket connection", e);
        }
    }

    protected Map<String, String> getHeaders(){
        Map<String, String> map = new HashMap<>();
        if(cloudToken != null){
            map.put(MqttsnCloudConstants.AUTHORIZATION_HEADER,
                    String.format(MqttsnCloudConstants.BEARER_TOKEN_HEADER, cloudToken.getToken()));
        }
        return map;
    }

    class AccountCreate {
        public String emailAddress;
        public String companyName;
        public String firstName;
        public String lastName;
    }
}
