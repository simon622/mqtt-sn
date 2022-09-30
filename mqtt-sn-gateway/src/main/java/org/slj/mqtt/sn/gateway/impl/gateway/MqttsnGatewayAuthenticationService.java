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

package org.slj.mqtt.sn.gateway.impl.gateway;

import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.IMqttsnAuthenticationService;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnService;

import java.util.Set;

public class MqttsnGatewayAuthenticationService
        extends MqttsnService  implements IMqttsnAuthenticationService {

    @Override
    public boolean allowConnect(IMqttsnContext context, String clientId) {
        Set<String> allowedClientId = ((MqttsnGatewayOptions)registry.getOptions()).getAllowedClientIds();
        if(allowedClientId != null && !allowedClientId.isEmpty()){
            return allowedClientId.contains(MqttsnGatewayOptions.DEFAULT_CLIENT_ALLOWED_ALL)
                    || allowedClientId.contains(clientId);
        }
        return false;
    }
}
