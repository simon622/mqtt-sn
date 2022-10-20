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
import org.slj.mqtt.sn.console.http.UsernamePassword;
import org.slj.mqtt.sn.console.http.impl.handlers.StaticFileHandler;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;

public class MqttsnStaticWebsiteHandler extends StaticFileHandler {

    static final String ROOT = "httpd";
    protected IMqttsnRuntimeRegistry registry;

    public MqttsnStaticWebsiteHandler(ObjectMapper mapper, IMqttsnRuntimeRegistry registry) {
        super(mapper, ROOT);
        this.registry = registry;
    }

    @Override
    protected UsernamePassword getRequiredCredentials() {
        String userName = ((MqttsnGatewayOptions)registry.getOptions()).getConsoleOptions().getUserName();
        String password = ((MqttsnGatewayOptions)registry.getOptions()).getConsoleOptions().getPassword();
        if(userName != null) {
            return new UsernamePassword(userName, password, "mqtt-sn-gateway");
        }
        return null;
    }
}
