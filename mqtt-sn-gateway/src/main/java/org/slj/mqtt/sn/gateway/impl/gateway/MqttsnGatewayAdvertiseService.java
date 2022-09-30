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

import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayAdvertiseService;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.impl.AbstractMqttsnBackoffThreadService;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.util.logging.Level;

public class MqttsnGatewayAdvertiseService
        extends AbstractMqttsnBackoffThreadService implements IMqttsnGatewayAdvertiseService {

    long lastGatewayBroadcastTime;

    public synchronized void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        if(runtime.getOptions().isEnableDiscovery()){
            super.start(runtime);
        }
    }

    @Override
    protected long doWork() {
        int timeout = 0;
        try {
            timeout = ((MqttsnGatewayOptions)registry.getOptions()).getGatewayAdvertiseTime();
            int gatewayId = ((MqttsnGatewayOptions) registry.getOptions()).getGatewayId();

            logger.log(Level.INFO, String.format("advertising gateway id [%s], next sending time in [%s] seconds",
                    gatewayId, timeout));

            IMqttsnMessage msg = registry.getMessageFactory().createAdvertise(
                    ((MqttsnGatewayOptions) registry.getOptions()).getGatewayId(),
                    timeout);
            registry.getTransport().broadcast(msg);
            lastGatewayBroadcastTime = System.currentTimeMillis();

        } catch(Exception e){
            logger.log(Level.WARNING, String.format("error sending advertising message"), e);
        }

        return timeout * 1000;
    }

    @Override
    protected String getDaemonName() {
        return "gateway-advertise";
    }
}
