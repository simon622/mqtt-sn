/*
 * Copyright (c) 2026 Ian Craggs
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

package org.slj.mqtt.sn.wire.version2_0.payload;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;

/**
 * ADVERTISE - wire format per OASIS mqtt-sn-v2.0 CSD01 (05 Feb 2026), section 3.20.1, Figure 31.
 * Same fixed shape as v1.2's ADVERTISE, just a different packet type byte (0x16 vs 0x00).
 */
public class MqttsnAdvertise_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected int gatewayId;
    protected int duration;

    @Override
    public int getMessageType() {
        return MqttsnConstants.ADVERTISE_V2_0;
    }

    @Override
    public boolean needsId() {
        return false;
    }

    public int getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(int gatewayId) {
        this.gatewayId = gatewayId;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        gatewayId = readUInt8Adjusted(data, 2);
        duration = readUInt16Adjusted(data, 3);
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {
        byte[] data = new byte[5];
        data[0] = (byte) data.length;
        data[1] = (byte) getMessageType();
        data[2] = (byte) gatewayId;
        data[3] = (byte) ((duration >> 8) & 0xFF);
        data[4] = (byte) (duration & 0xFF);
        return data;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateUInt8(gatewayId);
        MqttsnSpecificationValidator.validateUInt16(duration);
    }

    @Override
    public String toString() {
        return "MqttsnAdvertise_V2_0{" +
                "gatewayId=" + gatewayId +
                ", duration=" + duration +
                '}';
    }
}
