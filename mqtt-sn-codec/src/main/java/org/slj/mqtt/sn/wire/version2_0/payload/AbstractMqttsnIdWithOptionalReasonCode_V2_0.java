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

import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

/**
 * Shared shape of PUBACK / PUBREC / PUBREL / PUBCOMP / UNSUBACK in MQTT-SN 2.0 (CSD01),
 * sections 3.6.4-3.6.7 and 3.10 (Figures 14-17, 21) - all five are: Length, Packet Type,
 * Packet Identifier, and an optional Reason Code whose presence is inferred from the packet
 * length (absent == 0x00 Success). None of them have a flags byte.
 */
public abstract class AbstractMqttsnIdWithOptionalReasonCode_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected boolean reasonCodeSet;

    @Override
    public boolean needsId() {
        return true;
    }

    @Override
    public void setReturnCode(int returnCode) {
        reasonCodeSet = true;
        super.setReturnCode(returnCode);
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        id = readUInt16Adjusted(data, 2);

        int consumedLength = MqttsnWireUtils.isLargeMessage(data) ? 6 : 4;
        if (data.length > consumedLength) {
            returnCode = readUInt8Adjusted(data, 4);
            reasonCodeSet = true;
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        int length = 4 + (reasonCodeSet ? 1 : 0);
        byte[] data = new byte[length];
        data[0] = (byte) length;
        data[1] = (byte) getMessageType();
        data[2] = (byte) ((id >> 8) & 0xFF);
        data[3] = (byte) (id & 0xFF);
        if (reasonCodeSet) {
            data[4] = (byte) getReturnCode();
        }
        return data;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validatePacketIdentifier(id);
        if (reasonCodeSet) {
            MqttsnSpecificationValidator.validateReturnCode(returnCode);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getMessageName()).append('{');
        sb.append("id=").append(id);
        if (reasonCodeSet) {
            sb.append(", returnCode=").append(returnCode);
        }
        sb.append('}');
        return sb.toString();
    }
}
