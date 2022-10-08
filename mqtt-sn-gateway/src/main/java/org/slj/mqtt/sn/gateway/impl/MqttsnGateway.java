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

import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntime;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.spi.IMqttsnConnectionStateListener;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.io.IOException;
import java.util.logging.Level;

public class MqttsnGateway extends AbstractMqttsnRuntime {

    protected void startupServices(IMqttsnRuntimeRegistry runtime) throws MqttsnException {

        //-- ensure we start all the startable services
        callStartup(runtime.getMessageHandler());
        callStartup(runtime.getMessageQueue());
        callStartup(runtime.getMessageRegistry());
        callStartup(runtime.getTopicRegistry());
        callStartup(runtime.getWillRegistry());
        callStartup(runtime.getSecurityService());
        callStartup(runtime.getSubscriptionRegistry());
        callStartup(runtime.getMessageStateService());
        callStartup(runtime.getQueueProcessorStateCheckService());
        callStartup(runtime.getQueueProcessor());
        callStartup(runtime.getContextFactory());
        callStartup(runtime.getSessionRegistry());
        if (runtime.getMetrics() != null) callStartup(runtime.getMetrics());
        if (runtime.getAuthenticationService() != null) callStartup(runtime.getAuthenticationService());
        if (runtime.getAuthorizationService() != null) callStartup(runtime.getAuthorizationService());

        //-- start the network last
        callStartup(((IMqttsnGatewayRuntimeRegistry) runtime).getGatewaySessionService());
        callStartup(((IMqttsnGatewayRuntimeRegistry) runtime).getBackendConnectionFactory());
        callStartup(((IMqttsnGatewayRuntimeRegistry) runtime).getBackendService());

        //-- start discovery
        if (runtime.getOptions().isEnableDiscovery()) {
            callStartup(((IMqttsnGatewayRuntimeRegistry) runtime).getGatewayAdvertiseService());
        }

        if(((IMqttsnGatewayRuntimeRegistry) runtime).getConsole() != null){
            callStartup(((IMqttsnGatewayRuntimeRegistry) runtime).getConsole());
        }

        //-- start transport
        callStartup(runtime.getTransport());

        //-- notify the backend of confirmed message
        registerPublishReceivedListener((context, topicPath, qos, retained, data, message) -> {
            try {
                ((IMqttsnGatewayRuntimeRegistry) registry).
                        getBackendService().publish(context, topicPath, qos, retained, data, message);
            } catch (MqttsnException e) {
                logger.log(Level.SEVERE, "error publishing message to backend", e);
            }
        });

        //-- notify the backend of confirmed message
        registerPublishSentListener((context, messageId, topicPath, qos, retained, data, message) ->  {
            try {
                registry.getMessageRegistry().removeWhenCommitted(messageId);
            } catch (MqttsnException e) {
                logger.log(Level.SEVERE, "error publishing message to backend", e);
            }
        });

        registerConnectionListener(new IMqttsnConnectionStateListener() {
            @Override
            public void notifyConnected(IMqttsnContext context) {
            }

            @Override
            public void notifyRemoteDisconnect(IMqttsnContext context) {
                notifyCluster(context);
            }

            @Override
            public void notifyActiveTimeout(IMqttsnContext context) {
            }

            @Override
            public void notifyLocalDisconnect(IMqttsnContext context, Throwable t) {
                notifyCluster(context);
            }

            @Override
            public void notifyConnectionLost(IMqttsnContext context, Throwable t) {
                try {
                    IMqttsnSession session = registry.getSessionRegistry().getSession(context, false);
                    if(session != null){
                        ((IMqttsnGatewayRuntimeRegistry) registry).
                                getGatewaySessionService().markSessionLost(session);
                    }
                } catch (MqttsnException e) {
                    logger.log(Level.SEVERE, "error marking session lost;", e);
                } finally {
                    notifyCluster(context);
                }
            }

            private void notifyCluster(IMqttsnContext context){
                if(((MqttsnGatewayRuntimeRegistry)registry).getGatewayClusterService() != null){
                    try {
                        ((MqttsnGatewayRuntimeRegistry)registry).getGatewayClusterService().notifyDisconnection(context);
                    } catch (MqttsnException e) {
                        logger.log(Level.WARNING, String.format("error notifying cluster of disconnection/connection loss"), e);
                    }
                }
            }
        });
    }

    public void stopServices(IMqttsnRuntimeRegistry runtime) throws MqttsnException {

        //-- stop the networks first
        callShutdown(runtime.getTransport());

        if (runtime.getOptions().isEnableDiscovery()) {
            callShutdown(((IMqttsnGatewayRuntimeRegistry) runtime).getGatewayAdvertiseService());
        }

        if(((IMqttsnGatewayRuntimeRegistry) runtime).getConsole() != null){
            callShutdown(((IMqttsnGatewayRuntimeRegistry) runtime).getConsole());
        }

        callShutdown(runtime.getSecurityService());

        callShutdown(((IMqttsnGatewayRuntimeRegistry) runtime).getGatewaySessionService());
        callShutdown(((IMqttsnGatewayRuntimeRegistry) runtime).getBackendConnectionFactory());
        callShutdown(((IMqttsnGatewayRuntimeRegistry) runtime).getBackendService());

        //-- ensure we stop all the startable services

        if (runtime.getMetrics() != null) callShutdown(runtime.getMetrics());
        if (runtime.getAuthenticationService() != null) callShutdown(runtime.getAuthenticationService());
        if (runtime.getAuthorizationService() != null) callShutdown(runtime.getAuthorizationService());
        callShutdown(runtime.getContextFactory());
        callShutdown(runtime.getSessionRegistry());
        callShutdown(runtime.getMessageHandler());
        callShutdown(runtime.getMessageQueue());
        callShutdown(runtime.getMessageRegistry());
        callShutdown(runtime.getWillRegistry());
        callShutdown(runtime.getTopicRegistry());
        callShutdown(runtime.getSubscriptionRegistry());
        callShutdown(runtime.getQueueProcessorStateCheckService());
        callShutdown(runtime.getQueueProcessor());
        callShutdown(runtime.getMessageStateService());
    }

    public boolean handleRemoteDisconnect(IMqttsnContext context) {
        return true;
    }

    public boolean handleLocalDisconnectError(IMqttsnContext context, Throwable t) {
        return true;
    }

    @Override
    public void close() throws IOException {
        try {
            stop();
        } catch(Exception e){
            throw new IOException(e);
        }
    }
}