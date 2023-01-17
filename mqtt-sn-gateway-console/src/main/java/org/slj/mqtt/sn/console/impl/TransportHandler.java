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
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.IMqttsnTransport;
import org.slj.mqtt.sn.spi.ITransport;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TransportHandler extends MqttsnConsoleAjaxRealmHandler {


    public TransportHandler(ObjectMapper mapper, IMqttsnRuntimeRegistry registry) {
        super(mapper, registry);
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
        List<IMqttsnTransport> l = registry.getTransports();
        for(IMqttsnTransport t : l){
            bean.add(t.getName(), String.valueOf(t.getPort()), String.valueOf(t.getDescription()), "");
        }

        return bean;
    }

    class PropertiesBean {

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = HttpConstants.DEFAULT_DATE_FORMAT)
        public Date generatedAt;
        public List<PropertyBean> properties;

        public void add(String name, String port, String description, String MTU){
            if(properties == null)
                properties = new ArrayList<>();
            PropertyBean bean = new PropertyBean();
            bean.name = name;
            bean.port = port;
            bean.description = description;
            bean.MTU = MTU;
            bean.editable = false;
            properties.add(bean);
        }
    }

    class PropertyBean {

        public String name;
        public String port;
        public String description;
        public String MTU;
        public boolean editable = false;
    }
}
