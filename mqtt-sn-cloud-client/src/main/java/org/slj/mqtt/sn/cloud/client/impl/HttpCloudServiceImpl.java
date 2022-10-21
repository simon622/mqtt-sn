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
import org.slj.mqtt.sn.cloud.IMqttsnCloudService;
import org.slj.mqtt.sn.cloud.MqttsnCloudServiceDescriptor;
import org.slj.mqtt.sn.cloud.MqttsnConnectorDescriptor;
import org.slj.mqtt.sn.cloud.client.MqttsnCloudServiceException;
import org.slj.mqtt.sn.cloud.client.http.HttpClient;
import org.slj.mqtt.sn.cloud.client.http.HttpResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpCloudServiceImpl implements IMqttsnCloudService {

    static Logger logger = Logger.getLogger(HttpCloudServiceImpl.class.getName());

    protected String accountKey;
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
            HttpResponse response = HttpClient.head(serviceDiscoveryEndpoint, readTimeoutMillis);
            hasConnectivity = !response.isError();
            logger.log(Level.INFO, String.format("successfully established connection to cloud provider [%s], online", serviceDiscoveryEndpoint));
        } catch(IOException e){
            hasConnectivity = false;
            logger.log(Level.WARNING, String.format("connection to cloud provider [%s] could not be established, offline", serviceDiscoveryEndpoint));
        }
    }

    @Override
    public void updateAccessKey(String accountKey)
            throws MqttsnCloudServiceException {
        this.accountKey = accountKey;
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
        return descriptors;
    }

    protected  <T> List<T> httpGetList(String url, Class<? extends T> cls) throws MqttsnCloudServiceException {
        try {
            HttpResponse response =
                    HttpClient.get(url, connectTimeoutMillis, readTimeoutMillis);
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
                    HttpClient.get(url, connectTimeoutMillis, readTimeoutMillis);
            logger.log(Level.INFO, String.format("obtaining cloud service object from [%s] -> [%s]", url, response));
            checkResponse(response, true);

//System.err.println(new String(response.getResponseBody()));
            return mapper.readValue(response.getResponseBody(), cls);
        } catch(IOException e){
            throw new MqttsnCloudServiceException("error in http socket connection", e);
        }
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
            throw new MqttsnCloudServiceException("mqtt-sn cloud provider is unavailable at this time");
        }
    }

    @Override
    public boolean hasCloudConnectivity() {
        return hasConnectivity;
    }

    @Override
    public boolean hasCloudAccount() {
        return hasConnectivity;
    }
}
