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

import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnClientState;
import org.slj.mqtt.sn.model.session.IMqttsnSession;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@MqttsnService
public interface IMqttsnSessionRegistry extends IMqttsnService {

    Optional<IMqttsnContext> lookupClientIdSession(String clientId) throws MqttsnException;
    IMqttsnSession createNewSession(IMqttsnContext context) throws MqttsnException;
    IMqttsnSession getSession(IMqttsnContext context, boolean createIfNotExists) throws MqttsnException;
    Iterator<IMqttsnSession> iterator();
    boolean cleanSession(IMqttsnContext context, boolean deepClean) throws MqttsnException ;
    void clear(IMqttsnSession session, boolean clearNetworking) throws MqttsnException;
    void clear(IMqttsnSession session) throws MqttsnException ;

    long countSessions(MqttsnClientState state) ;

    long countTotalSessions();
    boolean hasSession(IMqttsnContext context);

    List<String> prefixSearch(String prefix);

    //-- attributes of the session setters
    void modifyClientState(IMqttsnSession session, MqttsnClientState state);
    void modifyLastSeen(IMqttsnSession session);
    void modifyKeepAlive(IMqttsnSession session, int keepAlive);
    void modifySessionExpiryInterval(IMqttsnSession session, long sessionExpiryInterval);
    void modifyMaxPacketSize(IMqttsnSession session, int maxPacketSize);

    void modifyProtocolVersion(IMqttsnSession session, int protocolVersion);

}
