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

package org.slj.mqtt.sn.model.session.impl;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.ClientState;
import org.slj.mqtt.sn.model.session.ISession;

import java.util.Date;
import java.util.Objects;

public class SessionImpl implements ISession {

    protected final IClientIdentifierContext context;
    protected volatile ClientState state;
    protected Date lastSeen;
    protected int keepAlive;
    protected long sessionExpiryInterval;
    protected Date sessionStarted;
    protected int maxPacketSize;
    protected int protocolVersion = MqttsnConstants.PROTOCOL_VERSION_UNKNOWN;

    public SessionImpl(final IClientIdentifierContext context, ClientState state){
        this.context = context;
        sessionStarted = new Date();
        this.state = state;
    }

    @Override
    public IClientIdentifierContext getContext() {
        return (IClientIdentifierContext) context;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }


    @Override
    public ClientState getClientState() {
        return state;
    }


    public void setClientState(ClientState state) {
        this.state = state;
    }

    @Override
    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public int getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    @Override
    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    public void setSessionExpiryInterval(long sessionExpiryInterval) {
        this.sessionExpiryInterval = sessionExpiryInterval;
    }

    @Override
    public Date getSessionStarted() {
        return sessionStarted;
    }

    public void setSessionStarted(Date sessionStarted) {
        this.sessionStarted = sessionStarted;
    }

    @Override
    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public void setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    @Override
    public String toString() {
        return "SessionImpl{" +
                "context=" + context +
                ", state=" + state +
                ", lastSeen=" + lastSeen +
                ", keepAlive=" + keepAlive +
                ", sessionExpiryInterval=" + sessionExpiryInterval +
                ", sessionStarted=" + sessionStarted +
                ", maxPacketSize=" + maxPacketSize +
                '}';
    }

    public void clearAll(){
        state = ClientState.DISCONNECTED;
        keepAlive = 0;
        sessionExpiryInterval = 0;
        sessionStarted = null;
        lastSeen = null;
        maxPacketSize = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionImpl that = (SessionImpl) o;
        return Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context);
    }
}
