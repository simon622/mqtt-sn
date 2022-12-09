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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.cloud.*;
import org.slj.mqtt.sn.cloud.client.http.HttpClient;
import org.slj.mqtt.sn.cloud.client.http.HttpResponse;
import org.slj.mqtt.sn.utils.General;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class HttpCloudServiceImpl implements IMqttsnCloudService {

    static Logger logger = LoggerFactory.getLogger(HttpCloudServiceImpl.class.getName());
    static final int DEFAULT_CLOUD_MONITOR_TIMEOUT = 10000;
    protected MqttsnCloudToken cloudToken;
    protected String serviceDiscoveryEndpoint;
    protected int connectTimeoutMillis;
    protected int readTimeoutMillis;
    protected int cloudMonitorTimeoutMillis;

    protected long lastRequestTime;

    protected ObjectMapper mapper;
    protected volatile boolean hasConnectivity;
    protected volatile boolean verified;
    private volatile List<MqttsnCloudServiceDescriptor> descriptors;
    private Thread cloudClientMonitor;
    private volatile boolean running = false;
    private Object monitor = new Object();

    public HttpCloudServiceImpl(ObjectMapper mapper, String serviceDiscoveryEndpoint, int connectTimeoutMillis, int readTimeoutMillis){
        this(mapper, serviceDiscoveryEndpoint, connectTimeoutMillis, readTimeoutMillis, DEFAULT_CLOUD_MONITOR_TIMEOUT);
    }

    public HttpCloudServiceImpl(ObjectMapper mapper, String serviceDiscoveryEndpoint, int connectTimeoutMillis, int readTimeoutMillis, int cloudMonitorTimeoutMillis){
        this.mapper = mapper;
        this.serviceDiscoveryEndpoint = serviceDiscoveryEndpoint;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.cloudMonitorTimeoutMillis = Math.max(2000, cloudMonitorTimeoutMillis);
        initMonitor();
        checkCloudStatus();
    }

    protected void initMonitor(){
        cloudClientMonitor = new Thread(() -> {
            while(running){
                try {
                    checkCloudStatus();
                    synchronized (monitor){
                        monitor.wait(cloudMonitorTimeoutMillis);
                    }
                } catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
        }, "mqtt-sn-cloud-monitor");
        cloudClientMonitor.setDaemon(true);
        cloudClientMonitor.setPriority(Thread.MIN_PRIORITY);
        cloudClientMonitor.start();
        running = true;
    }

    public void stop(){
        running = false;
        synchronized (monitor){
            monitor.notifyAll();
        }
        cloudClientMonitor = null;
    }


    @Override
    public MqttsnCloudToken authorizeCloudAccount(MqttsnCloudAccount account)
            throws MqttsnCloudServiceException {
        //-- ALWAYS re-authenticates with the cloud service
        checkConnectivity();
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

    public synchronized MqttsnCloudToken registerAccount(String emailAddress, String firstName, String lastName, String companyName, String macAddress, String contextId) throws MqttsnCloudServiceException {

        checkConnectivity();

        if(!General.validEmailAddress(emailAddress))
            throw new MqttsnCloudServiceException("invalid email entered; must conform to OWASP guidelines");

        AccountDetails create = new AccountDetails();
        create.emailAddress = emailAddress;
        create.companyName = companyName;
        create.firstName = firstName;
        create.lastName = lastName;
        create.macAddress = macAddress;
        create.contextId = contextId;

        MqttsnCloudToken token = httpPost(
                loadDescriptor(
                        MqttsnCloudServiceDescriptor.ACCOUNT_CREATE).getServiceEndpoint(),
                MqttsnCloudToken.class, create);
        return token;
    }


    public synchronized MqttsnCloudAccount readAccount() throws MqttsnCloudServiceException {

        checkConnectivity();

        MqttsnCloudAccount account = httpGet(
                loadDescriptor(MqttsnCloudServiceDescriptor.ACCOUNT_DETAILS).
                        getServiceEndpoint(), MqttsnCloudAccount.class);
        if(account != null){
            verified = account.isVerified();
        }
        return account;
    }

    @Override
    public void sendCloudEmail(MqttsnCloudEmail email) throws MqttsnCloudServiceException {

        checkConnectivity();

        if(email.getBody() == null || email.getSubject() == null ||
                email.getToAddresses() == null || email.getToAddresses().isEmpty()){
            throw new MqttsnCloudServiceException("unable to send email, required fields needed");
        }

        httpPost(
                loadDescriptor(
                        MqttsnCloudServiceDescriptor.SEND_EMAIL_SERVICE).getServiceEndpoint(),
                Void.class, email);
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
        logger.debug("mqtt-sn cloud provider has {} descriptors", descriptors.size());
        return descriptors;
    }

    protected void checkResponse(HttpResponse response, boolean expectPayload) throws MqttsnCloudServiceException {
        if(response == null)
            throw new MqttsnCloudServiceException("cloud service ["+response.getRequestUrl()+"] failed to respond <null>");
        if(response.isError())
            throw new MqttsnCloudServiceException("cloud service ["+response.getRequestUrl()+"] failed ("+response.getStatusCode()+" - "+response.getStatusMessage()+")");
        if(expectPayload && response.getContentLength() <= 0){
            throw new MqttsnCloudServiceException("cloud service ["+response.getRequestUrl()+"] failed with no expected content ("+response.getStatusCode()+" - "+response.getStatusMessage()+")");
        }
    }

    protected void checkConnectivity() throws MqttsnCloudServiceException {
        if(!hasConnectivity){
            logger.warn("mqtt-sn cloud provider is offline");
            throw new MqttsnCloudServiceException("mqtt-sn cloud provider is unavailable at this time");
        }
    }

    public void refreshCloudConnectivity(){
        checkCloudStatus();
    }

    @Override
    public boolean hasCloudConnectivity() {
        return hasConnectivity;
    }

    @Override
    public boolean isAuthorized() {
        return cloudToken != null;
    }

    @Override
    public boolean isVerified() {
        return isAuthorized() && verified;
    }

    @Override
    public void setToken(MqttsnCloudToken token) throws MqttsnCloudServiceException {
        this.cloudToken = token;
    }

    protected  <T> List<T> httpGetList(String url, Class<? extends T> cls) throws MqttsnCloudServiceException {
        try {
            HttpResponse response =
                    HttpClient.get(getHeaders(), url, connectTimeoutMillis, readTimeoutMillis);
            logger.debug("obtaining cloud service list from {} -> {}", url, response);
            checkResponse(response, true);
            return mapper.readValue(response.getResponseBody(),
                    mapper.getTypeFactory().constructCollectionType(List.class, cls));
        } catch(IOException e){
            throw new MqttsnCloudServiceException("error in http socket connection", e);
        } finally {
            updateLastAttempt();
        }
    }

    protected  <T> T httpGet(String url, Class<? extends T> cls) throws MqttsnCloudServiceException {
        try {
            HttpResponse response =
                    HttpClient.get(getHeaders(), url, connectTimeoutMillis, readTimeoutMillis);
            logger.debug("obtaining cloud service object from {} -> {}", url, response);
            checkResponse(response, true);
            return mapper.readValue(response.getResponseBody(), cls);
        } catch(IOException e){
            throw new MqttsnCloudServiceException("error in http socket connection", e);
        } finally {
            updateLastAttempt();
        }
    }

    protected  <T> T httpPost(String url, Class<? extends T> cls, Object jsonPostObject) throws MqttsnCloudServiceException {
        try {
            String jsonBody = mapper.writeValueAsString(jsonPostObject);

            try (InputStream is = new ByteArrayInputStream(jsonBody.getBytes())) {
                Map<String, String> headers = getHeaders();
                headers.put(MqttsnCloudConstants.CONTENT_TYPE, "application/json");
                headers.put(MqttsnCloudConstants.CONTENT_ENCODING, "UTF-8");
                HttpResponse response =
                        HttpClient.post(headers, url, is, connectTimeoutMillis, readTimeoutMillis);

                logger.info("post to cloud service object from {} {} -> {}", url, jsonBody, response);

                if(cls == Void.class){
                    return null;
                } else {

                    checkResponse(response, true);
                    byte[] b = response.getResponseBody();
                    logger.error("response is -> {}", new String(b));
                    return mapper.readValue(b, cls);
                }
            }
        } catch(IOException e){
            throw new MqttsnCloudServiceException("error reading response body;", e);
        } finally {
            updateLastAttempt();
        }
    }

    protected void checkCloudStatus(){
        try {
            HttpResponse response = HttpClient.head(getHeaders(), serviceDiscoveryEndpoint, readTimeoutMillis);
            hasConnectivity = !response.isError();
            logger.debug("successfully established connection to cloud provider {}, online", serviceDiscoveryEndpoint);
        } catch(IOException e){
            hasConnectivity = false;
            logger.warn("connection to cloud provider {} could not be established, offline", serviceDiscoveryEndpoint);
        }  finally {
            updateLastAttempt();
        }
    }

    protected void updateLastAttempt(){
        lastRequestTime = System.currentTimeMillis();
    }

    protected Map<String, String> getHeaders(){
        Map<String, String> map = new HashMap<>();
        if(cloudToken != null){
            map.put(MqttsnCloudConstants.AUTHORIZATION_HEADER,
                    String.format(MqttsnCloudConstants.BEARER_TOKEN_HEADER, cloudToken.getToken()));
        }
        return map;
    }

    class AccountDetails {
        public String emailAddress;
        public String companyName;
        public String firstName;
        public String lastName;
        public String macAddress;
        public String contextId;
    }
}
