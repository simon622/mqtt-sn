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

package org.slj.mqtt.sn.gateway.spi.gateway;

import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.IMqttsnSessionState;
import org.slj.mqtt.sn.model.TopicInfo;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.util.Iterator;
import java.util.Optional;

public interface IMqttsnGatewaySessionService extends IMqttsnRegistry<IMqttsnGatewayRuntimeRegistry> {

    Optional<IMqttsnContext> lookupClientIdSession(String clientId) throws MqttsnException;

    IMqttsnSessionState getSessionState(IMqttsnContext context, boolean createIfNotExists) throws MqttsnException;

    void wake(IMqttsnSessionState state) throws MqttsnException;

    void ping(IMqttsnSessionState state) throws MqttsnException;

    void updateLastSeen(IMqttsnSessionState state);

    void clear(IMqttsnContext context, boolean cleanSession, boolean networkLayer) throws MqttsnException;

    void cleanSession(IMqttsnContext state, boolean deepClean) throws MqttsnException;

    void receiveToSessions(String topicPath, int qos, boolean retained, byte[] payload) throws MqttsnException ;

    void markSessionLost(IMqttsnSessionState state) throws MqttsnException;

    Iterator<IMqttsnContext> iterator();

    //-- methods that deletegate to the backend

    ConnectResult connect(IMqttsnSessionState state, IMqttsnMessage message) throws MqttsnException;

    SubscribeResult subscribe(IMqttsnSessionState state, TopicInfo info, IMqttsnMessage message) throws MqttsnException;

    UnsubscribeResult unsubscribe(IMqttsnSessionState state, TopicInfo info, IMqttsnMessage message) throws MqttsnException;

    RegisterResult register(IMqttsnSessionState state, String topicPath) throws MqttsnException;

    DisconnectResult disconnect(IMqttsnSessionState state, IMqttsnMessage message) throws MqttsnException;
}
