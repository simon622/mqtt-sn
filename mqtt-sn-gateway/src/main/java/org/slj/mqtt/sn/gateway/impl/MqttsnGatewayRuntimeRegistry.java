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

import org.slj.mqtt.sn.console.IMqttsnConsole;
import org.slj.mqtt.sn.console.MqttsnConsoleOptions;
import org.slj.mqtt.sn.console.impl.MqttsnConsoleService;
import org.slj.mqtt.sn.gateway.impl.gateway.*;
import org.slj.mqtt.sn.gateway.spi.connector.IMqttsnConnector;
import org.slj.mqtt.sn.gateway.spi.connector.IMqttsnBackendService;
import org.slj.mqtt.sn.gateway.spi.gateway.*;
import org.slj.mqtt.sn.impl.*;
import org.slj.mqtt.sn.impl.metrics.MqttsnMetricsService;
import org.slj.mqtt.sn.impl.ram.*;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.net.NetworkAddressRegistry;
import org.slj.mqtt.sn.spi.IMqttsnStorageService;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;

public class MqttsnGatewayRuntimeRegistry extends AbstractMqttsnRuntimeRegistry implements IMqttsnGatewayRuntimeRegistry {
    private IMqttsnGatewayAdvertiseService advertiseService;
    private IMqttsnBackendService brokerService;
    private IMqttsnConnector connectionFactory;
    private IMqttsnGatewaySessionService sessionService;
    private IMqttsnGatewayClusterService clusterService;
    private IMqttsnConsole console;

    public MqttsnGatewayRuntimeRegistry(IMqttsnStorageService storageService, MqttsnOptions options){
        super(storageService, options);
    }

    public static MqttsnGatewayRuntimeRegistry defaultConfiguration(IMqttsnStorageService storageService, MqttsnGatewayOptions options){
        final MqttsnGatewayRuntimeRegistry registry = (MqttsnGatewayRuntimeRegistry) new MqttsnGatewayRuntimeRegistry(storageService, options).
                withGatewaySessionService(new MqttsnGatewaySessionService()).
                withGatewayAdvertiseService(new MqttsnGatewayAdvertiseService()).
                withMessageHandler(new MqttsnGatewayMessageHandler()).
                withMessageRegistry(new MqttsnInMemoryMessageRegistry()).
                withNetworkAddressRegistry(new NetworkAddressRegistry(options.getMaxNetworkAddressEntries())).
                withWillRegistry(new MqttsnInMemoryWillRegistry()).
                withTopicModifier(new MqttsnDefaultTopicModifier()).
                withMessageQueue(new MqttsnInMemoryMessageQueue()).
                withContextFactory(new MqttsnContextFactory()).
                withSessionRegistry(new MqttsnSessionRegistry()).
                withSecurityService(new MqttsnSecurityService()).
                withTopicRegistry(new MqttsnInMemoryTopicRegistry()).
                withMetrics(new MqttsnMetricsService()).
                withQueueProcessor(new MqttsnMessageQueueProcessor(false)).
                withQueueProcessorStateCheck(new MqttsnGatewayQueueProcessorStateService()).
                withSubscriptionRegistry(new MqttsnInMemorySubscriptionRegistry()).
                withAuthenticationService(new MqttsnGatewayAuthenticationService()).
                withClientIdFactory(new MqttsnDefaultClientIdFactory()).
                withMessageStateService(new MqttsnInMemoryMessageStateService(false));

        MqttsnConsoleOptions consoleOptions = options.getConsoleOptions();
        if(consoleOptions != null &&
                consoleOptions.isConsoleEnabled()){
            registry.withConsole(new MqttsnConsoleService(consoleOptions));
        }
        return registry;
    }

    public MqttsnGatewayRuntimeRegistry withGatewayClusterService(IMqttsnGatewayClusterService clusterService){
        this.clusterService = clusterService;
        return this;
    }

    public MqttsnGatewayRuntimeRegistry withConsole(IMqttsnConsole console){
        this.console = console;
        return this;
    }

    public MqttsnGatewayRuntimeRegistry withGatewayAdvertiseService(IMqttsnGatewayAdvertiseService advertiseService){
        this.advertiseService = advertiseService;
        return this;
    }

    public MqttsnGatewayRuntimeRegistry withBrokerConnectionFactory(IMqttsnConnector connectionFactory){
        this.connectionFactory = connectionFactory;
        return this;
    }

    public MqttsnGatewayRuntimeRegistry withGatewaySessionService(IMqttsnGatewaySessionService sessionService){
        this.sessionService = sessionService;
        return this;
    }

    public MqttsnGatewayRuntimeRegistry withBackendService(IMqttsnBackendService brokerService){
        this.brokerService = brokerService;
        return this;
    }

    @Override
    public IMqttsnGatewaySessionService getGatewaySessionService() {
        return sessionService;
    }

    @Override
    public IMqttsnBackendService getBackendService() {
        return brokerService;
    }

    @Override
    public IMqttsnConnector getConnector() {
        return connectionFactory;
    }

    @Override
    public IMqttsnGatewayAdvertiseService getGatewayAdvertiseService() {
        return advertiseService;
    }

    @Override
    public IMqttsnGatewayClusterService getGatewayClusterService() {
        return clusterService;
    }

    @Override
    public IMqttsnConsole getConsole() {
        return console;
    }

    @Override
    protected void validateOnStartup() throws MqttsnRuntimeException {

        super.validateOnStartup();
        if(brokerService == null) throw new MqttsnRuntimeException("message state service must be bound for valid runtime");
        if(connectionFactory == null) throw new MqttsnRuntimeException("connection factory must be bound for valid runtime");
        if(sessionService == null) throw new MqttsnRuntimeException("session service must be bound for valid runtime");
        if(clientIdFactory == null) throw new MqttsnRuntimeException("clientIdFactory must be bound for valid runtime");
    }
}