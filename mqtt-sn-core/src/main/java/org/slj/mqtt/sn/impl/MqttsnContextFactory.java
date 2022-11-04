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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.model.*;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.net.NetworkContext;
import org.slj.mqtt.sn.spi.IMqttsnContextFactory;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnSecurityException;
import org.slj.mqtt.sn.spi.AbstractMqttsnService;

public class MqttsnContextFactory
        extends AbstractMqttsnService implements IMqttsnContextFactory {

    protected static Logger logger = LoggerFactory.getLogger(MqttsnContextFactory.class.getName());

    @Override
    public INetworkContext createInitialNetworkContext(NetworkAddress address) throws MqttsnException {

        logger.info("create new network context for {}", address);
        NetworkContext context = new NetworkContext(address);
        return context;
    }

    @Override
    public IMqttsnContext createInitialApplicationContext(INetworkContext networkContext, String clientId, int protocolVersion) throws MqttsnSecurityException {

        logger.info("create new mqtt-sn context for {}, protocolVersion {}", clientId, protocolVersion);
        MqttsnContext context = new MqttsnContext(clientId);
        context.setProtocolVersion(protocolVersion);
        return context;
    }

    @Override
    public IMqttsnContext createTemporaryApplicationContext(INetworkContext networkContext, int protocolVersion) throws MqttsnSecurityException {

        logger.info("create temporary mqtt-sn context for {}, protocolVersion {}", networkContext, protocolVersion);
        MqttsnContext context = new MqttsnContext(null);
        context.setProtocolVersion(protocolVersion);
        return context;
    }

    @Override
    public IMqttsnMessageContext createMessageContext(INetworkContext networkContext) throws MqttsnSecurityException, MqttsnException {

        MqttsnMessageContext connectionContext = new MqttsnMessageContext(networkContext);

        IMqttsnContext mqttsnContext = getRegistry().getNetworkRegistry().getMqttsnContext(networkContext);
        connectionContext.setMqttsnContext(mqttsnContext);

        IMqttsnSession session =
                getRegistry().getSessionRegistry().getSession(mqttsnContext, false);
        connectionContext.setMqttsnSession(session);

        logger.info("creating mqtt-sn message context for processing {}", connectionContext);

        return connectionContext;
    }
}
