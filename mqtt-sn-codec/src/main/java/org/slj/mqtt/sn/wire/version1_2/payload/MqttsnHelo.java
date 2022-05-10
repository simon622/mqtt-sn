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

package org.slj.mqtt.sn.wire.version1_2.payload;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;

public class MqttsnHelo extends AbstractMqttsnMessage {

    protected String userAgent;


    @Override
    public int getMessageType() {
        return MqttsnConstants.HELO;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        if(data.length > 2){
            byte[] body = new byte[data[0] - 2];
            if (body.length > 0) {
                System.arraycopy(data, 2, body, 0, body.length);
                userAgent = new String(body, MqttsnConstants.CHARSET);
            }
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        int length = 2 + (userAgent == null ? 0 : userAgent.length());
        byte[] data = new byte[length];
        data[0] = (byte) length;
        data[1] = (byte) getMessageType();
        if (userAgent != null) {
            System.arraycopy(userAgent.getBytes(MqttsnConstants.CHARSET), 0, data, 2, userAgent.length());
        }
        return data;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public String toString() {
        return "MqttsnHelo{" +
                "userAgent='" + userAgent + '\'' +
                '}';
    }
}