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

package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.ClientState;
import org.slj.mqtt.sn.model.session.ISession;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@MqttsnService
public interface IMqttsnSessionRegistry extends IMqttsnService {

    Optional<IClientIdentifierContext> lookupClientIdSession(String clientId) throws MqttsnException;
    ISession createNewSession(IClientIdentifierContext context) throws MqttsnException;
    ISession getSession(IClientIdentifierContext context, boolean createIfNotExists) throws MqttsnException;
    Iterator<ISession> iterator();
    boolean cleanSession(IClientIdentifierContext context, boolean deepClean) throws MqttsnException ;
    void clear(ISession session, boolean clearNetworking) throws MqttsnException;
    void clear(ISession session) throws MqttsnException ;

    long countSessions(ClientState state) ;

    long countTotalSessions();
    boolean hasSession(IClientIdentifierContext context);

    //-- attributes of the session setters
    void modifyClientState(ISession session, ClientState state);
    void modifyLastSeen(ISession session);
    void modifyKeepAlive(ISession session, int keepAlive);
    void modifySessionExpiryInterval(ISession session, long sessionExpiryInterval);
    void modifyMaxPacketSize(ISession session, int maxPacketSize);

    void modifyProtocolVersion(ISession session, int protocolVersion);

}
