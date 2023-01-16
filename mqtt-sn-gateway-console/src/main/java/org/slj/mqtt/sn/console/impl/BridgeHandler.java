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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slj.mqtt.sn.cloud.*;
import org.slj.mqtt.sn.console.http.HttpConstants;
import org.slj.mqtt.sn.console.http.HttpException;
import org.slj.mqtt.sn.console.http.HttpInternalServerError;
import org.slj.mqtt.sn.console.http.IHttpRequestResponse;
import org.slj.mqtt.sn.gateway.spi.bridge.ProtocolBridgeOptions;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorException;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.IMqttsnStorageService;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.General;
import org.slj.mqtt.sn.utils.MqttsnUtils;

import java.io.IOException;
import java.util.*;

public class BridgeHandler extends MqttsnConsoleAjaxRealmHandler {

    static final String ACTION = "action";
    static final String BRIDGE = "bridge";
    static final String START = "start";
    static final String STOP = "stop";
    static final String BRIDGE_DETAILS = "bridgeDetails";

    protected IMqttsnCloudService cloudService;

    public BridgeHandler(IMqttsnCloudService cloudService, ObjectMapper mapper, IMqttsnRuntimeRegistry registry) {
        super(mapper, registry);
        this.cloudService = cloudService;
    }

    protected void handleHttpPost(IHttpRequestResponse request) throws HttpException, IOException {
        try {
            PropertyForm form = readRequestBody(request, PropertyForm.class);
            Iterator<String> itr = form.getProperties().keySet().iterator();
            List<ProtocolBridgeDescriptor> descriptors = runtimeBridges().bridges;


            MqttsnConnectorDescriptor descriptor = getRegistry().getBackendService().getDescriptorById(
                    descriptors, form.getProperties().get("bridgeId"));


            IMqttsnStorageService storageService = getRegistry().getStorageService().
                    getPreferenceNamespace(descriptor);
            while(itr.hasNext()){
                String key = itr.next();
                String value = form.getProperties().get(key);
                storageService.setStringPreference(key, value);
            }

            //-- restart the server
            ProtocolBridgeOptions options = new ProtocolBridgeOptions();
            storageService.initializeFieldsFromStorage(options);


            getRegistry().getBackendService().initializeConnector(descriptor, options);

            writeMessageBeanResponse(request, HttpConstants.SC_OK,
                    new Message("Connector successfully initialised", true));

        } catch(MqttsnCloudServiceException | MqttsnException e){
            writeHTMLResponse(request, HttpConstants.SC_INTERNAL_SERVER_ERROR,
                    General.getRootCauseMessage(e));
        }
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse request) throws HttpException, IOException {
        try {

            List<ProtocolBridgeDescriptor> descriptors = runtimeBridges().bridges;

            ProtocolBridgeDescriptor descriptor = getRegistry().getBackendService().getInstalledDescriptor(descriptors);

            String action = null;
            if((action = getParameter(request, ACTION)) != null){
                String connectorId = null;
                if(STOP.equals(action)){
                    connectorId = getMandatoryParameter(request, BRIDGE);
                    getRegistry().getBackendService().stop();
                    writeSimpleOKResponse(request);
                } else if(START.equals(action)){
                    connectorId = getMandatoryParameter(request, BRIDGE);
                    getRegistry().getBackendService().start(getRegistry());
                    writeSimpleOKResponse(request);
                }
                else if(BRIDGE_DETAILS.equals(action)){
                    connectorId = getMandatoryParameter(request, BRIDGE);
                    writeJSONBeanResponse(request, HttpConstants.SC_OK,
                            getRegistry().getBackendService().getDescriptorById(descriptors, connectorId));
                }
            } else {
                //return the connector bean list
                BeanList beanList = runtimeBridges();
                writeJSONBeanResponse(request, HttpConstants.SC_OK, beanList);
            }
        } catch(Exception e){
            throw new HttpInternalServerError("error populating bean;", e);
        }
    }

    protected BeanList runtimeBridges() throws MqttsnCloudServiceException {

        List<ProtocolBridgeDescriptor> bridges = null;
        if(cloudService != null && bridges == null){
            bridges = cloudService.getAvailableBridges();
        }

        //for each connector apply its local runtime status
        if(bridges != null && !bridges.isEmpty()){
            bridges = applyRuntimeStatus(bridges);
        }

        BeanList beanList = new BeanList();
        beanList.bridges = bridges;
        return beanList;
    }

    protected List<ProtocolBridgeDescriptor> applyRuntimeStatus(List<ProtocolBridgeDescriptor> list){

        List<ProtocolBridgeDescriptor> runtimeBridges = new ArrayList<>();
        Iterator<ProtocolBridgeDescriptor> itr = list.iterator();
        while(itr.hasNext()){
            ProtocolBridgeDescriptor bean = itr.next();
            RuntimeBridgeBean runtimeBean = new RuntimeBridgeBean(bean);
            try {
                applyRuntimeStatus(runtimeBean);
                runtimeBridges.add(runtimeBean);
            } catch(MqttsnConnectorException e){
                //ignore
            }
        }
        return runtimeBridges;
    }

    private void applyRuntimeStatus(RuntimeBridgeBean runtimeBean) throws MqttsnConnectorException{

        runtimeBean.available = getRegistry().getBackendService().connectorAvailable(runtimeBean);
        runtimeBean.running = getRegistry().getBackendService().matchesRunningConnector(runtimeBean) &&
                getRegistry().getBackendService().isConnected(null);
        //-- check if its running
        if(runtimeBean.available){
            //-- decorate the properties with those that would be used
            List<DescriptorProperty> properties = runtimeBean.getProperties();
            IMqttsnStorageService storageService = getRegistry().getStorageService();
            if(properties != null){
                //load the connector specific settings and read back to the global ones
                for(DescriptorProperty p : properties){
                    p.setValue(
                            storageService.getPreferenceNamespace(runtimeBean).
                                    getStringPreference(p.getName(),
                                            storageService.getStringPreference(p.getName(), null)));
                    if(p.getDefaultValue() != null){
                        p.setValue(p.getDefaultValue());
                    }

                    p.setDisplayName(
                            MqttsnUtils.upperCaseWords(
                            MqttsnUtils.splitCamelCase(p.getName())));
                }
            }
        }
    }

    private static String status(boolean status, String text){
        String statusStr = null;
        if(status){
            statusStr = "<button class=\"btn btn-sm btn-success\" type=\"button\" style=\"opacity: 100%\" disabled>\n" +
                    "                <span class=\"spinner-grow spinner-grow-sm\" role=\"status\" aria-hidden=\"true\"></span>\n" +
                    "                "+text+"\n" +
                    "            </button>";
        } else {
            statusStr = "<button class=\"btn btn-sm btn-danger\" type=\"button\" style=\"opacity: 100%\" disabled>\n" +
                    "                "+text+"\n" +
                    "            </button>";
        }

        return statusStr;
    }

    class BeanList {
        public List<ProtocolBridgeDescriptor> bridges;
    }

    class RuntimeBridgeBean extends ProtocolBridgeDescriptor {

        public boolean running = false;
        public boolean available = false;
        public RuntimeBridgeBean(ProtocolBridgeDescriptor bean){
            copyFrom(bean);
        }
    }


    static class PropertyForm {

        public PropertyForm() {
        }

        private Map<String, String> properties = new HashMap<>();

        @JsonAnyGetter
        public Map<String, String> getProperties() {
            return properties;
        }

        @JsonAnySetter
        public void setProperty(String property, String value){
            properties.put(property, value);
        }
    }
}

