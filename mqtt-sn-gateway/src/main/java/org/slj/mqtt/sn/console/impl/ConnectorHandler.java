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
import org.slj.mqtt.sn.cloud.client.MqttsnCloudServiceException;
import org.slj.mqtt.sn.cloud.client.impl.HttpCloudServiceImpl;
import org.slj.mqtt.sn.console.http.HttpConstants;
import org.slj.mqtt.sn.console.http.HttpException;
import org.slj.mqtt.sn.console.http.HttpInternalServerError;
import org.slj.mqtt.sn.console.http.IHttpRequestResponse;
import org.slj.mqtt.sn.gateway.impl.MqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.gateway.spi.connector.IMqttsnConnector;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class ConnectorHandler extends MqttsnConsoleAjaxRealmHandler {

    protected IMqttsnCloudService cloudService;

    public ConnectorHandler(ObjectMapper mapper, IMqttsnRuntimeRegistry registry) {
        super(mapper, registry);
        cloudService = new HttpCloudServiceImpl(mapper, "http://mqtt-sn.cloud/api/services.json", 5000, 5000);
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse request) throws HttpException, IOException {
        try {
            BeanList beanList = populateBean();
            writeJSONBeanResponse(request, HttpConstants.SC_OK, beanList);
        } catch(Exception e){
            throw new HttpInternalServerError("error populating bean;", e);
        }
    }

    protected BeanList populateBean() throws MqttsnCloudServiceException {
        List<MqttsnConnectorDescriptor> connectors = null;
        if(cloudService == null){
            connectors = cloudService.getAvailableConnectors();
        }

        //for each connector apply its local runtime status
        if(connectors != null && !connectors.isEmpty()){
            connectors = applyRuntimeStatus(connectors);
        }

        BeanList beanList = new BeanList();
        beanList.connectors = connectors;
        return beanList;
    }

    protected List<MqttsnConnectorDescriptor> applyRuntimeStatus(List<MqttsnConnectorDescriptor> list){

        Iterator<MqttsnConnectorDescriptor> itr = list.iterator();
        while(itr.hasNext()){
            MqttsnConnectorDescriptor bean = itr.next();
            RuntimeConnectorBean runtimeBean = new RuntimeConnectorBean(bean);
            try {
                Class.forName(runtimeBean.getClassName());
                runtimeBean.available = true;
            } catch(Exception e){
                // not available on runtime
            }

            try {
                IMqttsnConnector connector =
                        ((MqttsnGatewayRuntimeRegistry)getRegistry()).getConnector();
                runtimeBean.running = connector.getClass().getName().equals(runtimeBean.getClassName());
            } catch(Exception e){
                // not available on runtime
            }
        }

        return list;
    }

    class BeanList {
        public List<MqttsnConnectorDescriptor> connectors;
    }

    class RuntimeConnectorBean extends MqttsnConnectorDescriptor {

        public boolean running = false;
        public boolean available = false;
        public RuntimeConnectorBean(MqttsnConnectorDescriptor bean){
            copyFrom(bean);
        }
    }
}