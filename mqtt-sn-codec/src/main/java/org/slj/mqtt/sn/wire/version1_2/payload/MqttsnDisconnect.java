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
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnDisconnectPacket;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;

public class MqttsnDisconnect extends AbstractMqttsnMessage implements IMqttsnMessageValidator, IMqttsnDisconnectPacket {

    protected int duration;

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public int getMessageType() {
        return MqttsnConstants.DISCONNECT;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        if (data.length == 4) {
            duration = readUInt16Adjusted(data, 2);
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {
        byte[] data = new byte[duration > 0 ? 4 : 2];
        data[0] = (byte) data.length;
        data[1] = (byte) getMessageType();
        if (data.length == 4) {
            data[2] = (byte) ((duration >> 8) & 0xFF);
            data[3] = (byte) (duration & 0xFF);
        }
        return data;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttsnDisconnect{");
        sb.append("duration=").append(duration);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateDuration(duration);
    }
}