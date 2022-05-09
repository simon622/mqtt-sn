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

import org.slj.mqtt.sn.gateway.impl.gateway.*;
import org.slj.mqtt.sn.gateway.spi.broker.IMqttsnBackendConnectionFactory;
import org.slj.mqtt.sn.gateway.spi.broker.IMqttsnBackendService;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayAdvertiseService;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewaySessionService;
import org.slj.mqtt.sn.impl.*;
import org.slj.mqtt.sn.impl.ram.*;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.net.NetworkAddressRegistry;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;

public class MqttsnGatewayRuntimeRegistry extends AbstractMqttsnRuntimeRegistry implements IMqttsnGatewayRuntimeRegistry {

    private IMqttsnGatewayAdvertiseService advertiseService;
    private IMqttsnBackendService brokerService;
    private IMqttsnBackendConnectionFactory connectionFactory;
    private IMqttsnGatewaySessionService sessionService;

    public MqttsnGatewayRuntimeRegistry(MqttsnOptions options){
        super(options);
    }

    public static MqttsnGatewayRuntimeRegistry defaultConfiguration(MqttsnOptions options){
        MqttsnGatewaySessionService sessionService = new MqttsnGatewaySessionService();
        final MqttsnGatewayRuntimeRegistry registry = (MqttsnGatewayRuntimeRegistry) new MqttsnGatewayRuntimeRegistry(options).
                withGatewaySessionService(new MqttsnGatewaySessionService()).
                withGatewayAdvertiseService(new MqttsnGatewayAdvertiseService()).
                withMessageHandler(new MqttsnGatewayMessageHandler()).
                withMessageRegistry(new MqttsnInMemoryMessageRegistry()).
                withNetworkAddressRegistry(new NetworkAddressRegistry(options.getMaxNetworkAddressEntries())).
                withWillRegistry(new MqttsnInMemoryWillRegistry()).
                withMessageQueue(new MqttsnInMemoryMessageQueue()).
                withContextFactory(new MqttsnContextFactory()).
                withTopicRegistry(new MqttsnInMemoryTopicRegistry()).
                withQueueProcessor(new MqttsnMessageQueueProcessor(false)).
                withQueueProcessorStateCheck(new MqttsnGatewayQueueProcessorStateService()).
                withSubscriptionRegistry(new MqttsnInMemorySubscriptionRegistry()).
                withAuthenticationService(new MqttsnGatewayAuthenticationService()).
                withMessageStateService(new MqttsnInMemoryMessageStateService(false));
        return registry;
    }

    public MqttsnGatewayRuntimeRegistry withGatewayAdvertiseService(IMqttsnGatewayAdvertiseService advertiseService){
        this.advertiseService = advertiseService;
        return this;
    }

    public MqttsnGatewayRuntimeRegistry withBrokerConnectionFactory(IMqttsnBackendConnectionFactory connectionFactory){
        this.connectionFactory = connectionFactory;
        return this;
    }

    public MqttsnGatewayRuntimeRegistry withGatewaySessionService(IMqttsnGatewaySessionService sessionService){
        this.sessionService = sessionService;
        return this;
    }

    public MqttsnGatewayRuntimeRegistry withBrokerService(IMqttsnBackendService brokerService){
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
    public IMqttsnBackendConnectionFactory getBackendConnectionFactory() {
        return connectionFactory;
    }

    public IMqttsnGatewayAdvertiseService getGatewayAdvertiseService() {
        return advertiseService;
    }

    @Override
    protected void validateOnStartup() throws MqttsnRuntimeException {
        if(brokerService == null) throw new MqttsnRuntimeException("message state service must be bound for valid runtime");
        if(connectionFactory == null) throw new MqttsnRuntimeException("connection factory must be bound for valid runtime");
        if(sessionService == null) throw new MqttsnRuntimeException("session service must be bound for valid runtime");
    }
}