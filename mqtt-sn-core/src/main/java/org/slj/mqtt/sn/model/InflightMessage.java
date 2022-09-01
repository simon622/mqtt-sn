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

import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnOriginatingMessageSource;

import java.io.Serializable;

public class InflightMessage implements Serializable {

    transient MqttsnWaitToken token;
    private IMqttsnMessage message;
    private long time;
    private IMqttsnOriginatingMessageSource source;

    public InflightMessage(IMqttsnMessage message, IMqttsnOriginatingMessageSource source, MqttsnWaitToken token) {
        this.time = System.currentTimeMillis();
        this.source = source;
        this.message = message;
        this.token = token;
    }

    public long getTime(){
        return time;
    }

    public IMqttsnMessage getMessage(){
        return message;
    }

    public MqttsnWaitToken getToken() {
        return token;
    }

    public IMqttsnOriginatingMessageSource getOriginatingMessageSource() {
        return source;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "InflightMessage{" +
                "token=" + token +
                ", message=" + message +
                ", time=" + time +
                ", source=" + source +
                '}';
    }
}
