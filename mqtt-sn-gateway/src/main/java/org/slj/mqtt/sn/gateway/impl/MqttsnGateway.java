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
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.spi.IMqttsnConnectionStateListener;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.io.IOException;

public class MqttsnGateway extends AbstractMqttsnRuntime {


    protected void notifyServicesStarted() {

        //-- notify the backend of confirmed message
        registerPublishReceivedListener((context, topicPath, qos, retained, data, message) -> {
            try {
                ((IMqttsnGatewayRuntimeRegistry) registry).
                        getBackendService().publish(context, topicPath, qos, retained, data, message);
            } catch (MqttsnException e) {
                logger.error("error publishing message to backend", e);
            }
        });

        registerConnectionListener(new IMqttsnConnectionStateListener() {
            @Override
            public void notifyConnected(IClientIdentifierContext context) {
            }

            @Override
            public void notifyRemoteDisconnect(IClientIdentifierContext context) {
                notifyCluster(context);
            }

            @Override
            public void notifyActiveTimeout(IClientIdentifierContext context) {
            }

            @Override
            public void notifyLocalDisconnect(IClientIdentifierContext context, Throwable t) {
                notifyCluster(context);
            }

            @Override
            public void notifyConnectionLost(IClientIdentifierContext context, Throwable t) {
                try {
                    ISession session = registry.getSessionRegistry().getSession(context, false);
                    if(session != null){
                        ((IMqttsnGatewayRuntimeRegistry) registry).
                                getGatewaySessionService().markSessionLost(session);
                    }
                } catch (MqttsnException e) {
                    logger.error("error marking session lost;", e);
                } finally {
                    notifyCluster(context);
                }
            }

            private void notifyCluster(IClientIdentifierContext context){
                if(((MqttsnGatewayRuntimeRegistry)registry).getGatewayClusterService() != null){
                    try {
                        ((MqttsnGatewayRuntimeRegistry)registry).getGatewayClusterService().notifyDisconnection(context);
                    } catch (MqttsnException e) {
                        logger.warn("error notifying cluster of disconnection/connection loss", e);
                    }
                }
            }
        });
    }

    public boolean handleRemoteDisconnect(IClientIdentifierContext context) {
        return true;
    }

    public boolean handleLocalDisconnectError(IClientIdentifierContext context, Throwable t) {
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
