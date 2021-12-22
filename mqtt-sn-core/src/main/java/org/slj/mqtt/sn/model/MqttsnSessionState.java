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

package org.slj.mqtt.sn.model;

import java.util.Date;

public class MqttsnSessionState implements IMqttsnSessionState {

    private IMqttsnContext context;
    private MqttsnClientState state;
    private Date lastSeen;
    private int keepAlive;
    private long sessionExpiryInterval;
    private Date sessionStarted;

    public MqttsnSessionState(IMqttsnContext context, MqttsnClientState state){
        this.context = context;
        sessionStarted = new Date();
        this.state = state;
    }

    @Override
    public IMqttsnContext getContext() {
        return context;
    }

    @Override
    public MqttsnClientState getClientState() {
        return state;
    }

    public void setContext(IMqttsnContext context) {
        this.context = context;
    }

    public void setClientState(MqttsnClientState state) {
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

    @Override
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
    public String toString() {
        return "MqttsnSessionState{" +
                "context=" + context +
                ", state=" + state +
                ", lastSeen=" + lastSeen +
                ", keepAlive=" + keepAlive +
                ", sessionExpiryInterval=" + sessionExpiryInterval +
                ", sessionStarted=" + sessionStarted +
                '}';
    }
}
