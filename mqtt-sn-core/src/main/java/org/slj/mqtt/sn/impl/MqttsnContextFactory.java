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

package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.MqttsnContext;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.net.NetworkContext;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnConnect;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MqttsnContextFactory<T extends IMqttsnRuntimeRegistry>
        extends MqttsnService<T> implements IMqttsnContextFactory {

    protected static Logger logger = Logger.getLogger(MqttsnContextFactory.class.getName());

    @Override
    public INetworkContext createInitialNetworkContext(NetworkAddress address) throws MqttsnException {

        logger.log(Level.INFO,
                String.format("create new network context for [%s]", address));
        NetworkContext context = new NetworkContext(address);
        return context;
    }

    @Override
    public IMqttsnContext createInitialApplicationContext(INetworkContext networkContext, String clientId) throws MqttsnSecurityException {

        logger.log(Level.INFO, String.format("create new mqtt-sn context for [%s]", clientId));
        MqttsnContext context = new MqttsnContext(clientId);
        return context;
    }

    @Override
    public IMqttsnContext createTemporaryApplicationContext(INetworkContext networkContext) throws MqttsnSecurityException {

        logger.log(Level.INFO, String.format("create temporary mqtt-sn context for [%s]", networkContext));
        MqttsnContext context = new MqttsnContext(null);
        return context;
    }
}
