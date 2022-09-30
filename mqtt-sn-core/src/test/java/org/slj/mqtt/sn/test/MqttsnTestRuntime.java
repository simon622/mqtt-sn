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

package org.slj.mqtt.sn.test;

import org.slj.mqtt.sn.impl.AbstractMqttsnRuntime;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.io.IOException;

public class MqttsnTestRuntime extends AbstractMqttsnRuntime {

    public static MqttsnOptions TEST_OPTIONS = new MqttsnOptions();
    public static String TEST_CLIENT_ID = "testClientId";
    public static NetworkAddress TEST_ADDRESS = NetworkAddress.localhost(10001);

    @Override
    public void close() throws IOException {
    }

    @Override
    protected void startupServices(final IMqttsnRuntimeRegistry registry) throws MqttsnException {
        callStartup(registry.getMessageHandler());
        callStartup(registry.getMessageQueue());
        callStartup(registry.getMessageRegistry());
        callStartup(registry.getTopicRegistry());
        callStartup(registry.getWillRegistry());
        callStartup(registry.getSecurityService());
        callStartup(registry.getSubscriptionRegistry());
        callStartup(registry.getMessageStateService());
        callStartup(registry.getQueueProcessorStateCheckService());
        callStartup(registry.getQueueProcessor());
        callStartup(registry.getContextFactory());
        if (registry.getAuthenticationService() != null) callStartup(registry.getAuthenticationService());
        if (registry.getAuthorizationService() != null) callStartup(registry.getAuthorizationService());
        callStartup(registry.getTransport());
        callStartup(registry.getSessionRegistry());
    }

    @Override
    protected void stopServices(final IMqttsnRuntimeRegistry registry) throws MqttsnException {
        callShutdown(registry.getMessageHandler());
        callShutdown(registry.getMessageQueue());
        callShutdown(registry.getMessageRegistry());
        callShutdown(registry.getTopicRegistry());
        callShutdown(registry.getWillRegistry());
        callShutdown(registry.getSecurityService());
        callShutdown(registry.getSubscriptionRegistry());
        callShutdown(registry.getMessageStateService());
        callShutdown(registry.getQueueProcessorStateCheckService());
        callShutdown(registry.getQueueProcessor());
        callShutdown(registry.getContextFactory());
        if (registry.getAuthenticationService() != null) callShutdown(registry.getAuthenticationService());
        if (registry.getAuthorizationService() != null) callShutdown(registry.getAuthorizationService());
        callShutdown(registry.getTransport());
        callShutdown(registry.getSessionRegistry());
    }
}
