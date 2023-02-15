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

package org.slj.mqtt.sn.gateway.impl;

import org.slj.mqtt.sn.gateway.impl.bridge.ProtocolBridgeService;
import org.slj.mqtt.sn.gateway.impl.gateway.*;
import org.slj.mqtt.sn.gateway.spi.bridge.IProtocolBridgeService;
import org.slj.mqtt.sn.gateway.spi.connector.IMqttsnBackendService;
import org.slj.mqtt.sn.gateway.spi.connector.IMqttsnConnector;
import org.slj.mqtt.sn.gateway.spi.gateway.*;
import org.slj.mqtt.sn.impl.*;
import org.slj.mqtt.sn.impl.metrics.MqttsnMetricsService;
import org.slj.mqtt.sn.impl.ram.*;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.net.ContextTransportLocator;
import org.slj.mqtt.sn.net.NetworkAddressRegistry;
import org.slj.mqtt.sn.spi.IMqttsnStorageService;

public class MqttsnGatewayRuntimeRegistry extends AbstractMqttsnRuntimeRegistry implements IMqttsnGatewayRuntimeRegistry {

    public MqttsnGatewayRuntimeRegistry(IMqttsnStorageService storageService, MqttsnOptions options){
        super(storageService, options);
    }

    public static MqttsnGatewayRuntimeRegistry defaultConfiguration(IMqttsnStorageService storageService, MqttsnGatewayOptions options){
        final MqttsnGatewayRuntimeRegistry registry = (MqttsnGatewayRuntimeRegistry)
                    new MqttsnGatewayRuntimeRegistry(storageService, options).
                withGatewaySessionService(new MqttsnGatewaySessionService()).
                withExpansionHandler(new MqttsnGatewayExpansionHandler()).
                withGatewayAdvertiseService(new MqttsnGatewayAdvertiseService()).
                withMessageHandler(new MqttsnGatewayMessageHandler()).
                withTransportLocator(new ContextTransportLocator()).
                withMessageRegistry(new MqttsnInMemoryMessageRegistry()).
                withNetworkAddressRegistry(new NetworkAddressRegistry(options.getMaxClientSessions())).
                withWillRegistry(new MqttsnInMemoryWillRegistry()).
                withTopicModifier(new MqttsnDefaultTopicModifier()).
                withMessageQueue(new MqttsnInMemoryMessageQueue()).
//                withMessageQueue(new MqttsnFileBackedInMemoryMessageQueue(
//                        new MqttsnJacksonReaderWriter())).
                withContextFactory(new MqttsnContextFactory()).
                withSessionRegistry(new MqttsnSessionRegistry()).
                withSecurityService(new MqttsnSecurityService()).
                withTopicRegistry(new MqttsnInMemoryTopicRegistry()).
                withMetrics(new MqttsnMetricsService()).
                withDeadLetterQueue(new MqttsnInMemoryDeadLetterQueue()).
                withQueueProcessor(new MqttsnMessageQueueProcessor(false)).
                withQueueProcessorStateCheck(new MqttsnGatewayQueueProcessorStateService()).
                withSubscriptionRegistry(new MqttsnInMemorySubscriptionRegistry()).
                withAuthenticationService(new MqttsnGatewayAuthenticationService()).
                withClientIdFactory(new MqttsnDefaultClientIdFactory()).
                withMessageStateService(new MqttsnInMemoryMessageStateService(false));

        registry.withProtocolBridgeService(new ProtocolBridgeService());
        return registry;
    }

    public MqttsnGatewayRuntimeRegistry withGatewayClusterService(IMqttsnGatewayClusterService clusterService){
        withService(clusterService);
        return this;
    }

    public MqttsnGatewayRuntimeRegistry withGatewayAdvertiseService(IMqttsnGatewayAdvertiseService advertiseService){
        withService(advertiseService);
        return this;
    }

    public MqttsnGatewayRuntimeRegistry withConnector(IMqttsnConnector connector){
        withServiceReplaceIfExists(IMqttsnConnector.class, connector);
        return this;
    }

    public MqttsnGatewayRuntimeRegistry withGatewaySessionService(IMqttsnGatewaySessionService sessionService){
        withService(sessionService);
        return this;
    }

    public MqttsnGatewayRuntimeRegistry withBackendService(IMqttsnBackendService brokerService){
        withService(brokerService);
        return this;
    }

    public MqttsnGatewayRuntimeRegistry withExpansionHandler(IMqttsnGatewayExpansionHandler handler){
        withService(handler);
        return this;
    }

    @Override
    public IMqttsnGatewaySessionService getGatewaySessionService() {
        return getService(IMqttsnGatewaySessionService.class);
    }

    @Override
    public IMqttsnBackendService getBackendService() {
        return getService(IMqttsnBackendService.class);
    }

    @Override
    public IMqttsnConnector getConnector() {
        return getService(IMqttsnConnector.class);
    }

    @Override
    public IMqttsnGatewayAdvertiseService getGatewayAdvertiseService() {
        return getOptionalService(IMqttsnGatewayAdvertiseService.class).orElse(null);
    }

    @Override
    public IMqttsnGatewayClusterService getGatewayClusterService() {
        return getOptionalService(IMqttsnGatewayClusterService.class).orElse(null);
    }

    public IMqttsnGatewayExpansionHandler getExpansionHandler() {
        return getService(IMqttsnGatewayExpansionHandler.class);
    }

    @Override
    public IMqttsnGatewayRuntimeRegistry withProtocolBridgeService(IProtocolBridgeService protocolBridge) {
        withService(protocolBridge);
        return this;
    }

    @Override
    public IProtocolBridgeService getProtocolBridgeService() {
        return getService(IProtocolBridgeService.class);
    }
}