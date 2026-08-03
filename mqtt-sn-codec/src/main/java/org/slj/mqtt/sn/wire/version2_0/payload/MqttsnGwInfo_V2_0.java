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
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

/**
 * GWINFO - wire format per OASIS mqtt-sn-v2.0 CSD01 (05 Feb 2026), section 3.20.3, Figure 33.
 * Response to SEARCHGW. Gateway Address is optional - only present when this packet is sent by
 * a Client (relaying a Gateway it already knows about), not when sent directly by a Gateway.
 */
public class MqttsnGwInfo_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected int gatewayId;
    protected String gatewayAddress;

    @Override
    public int getMessageType() {
        return MqttsnConstants.GWINFO_V2_0;
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

    public String getGatewayAddress() {
        return gatewayAddress;
    }

    public void setGatewayAddress(String gatewayAddress) {
        this.gatewayAddress = gatewayAddress;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        gatewayId = readUInt8Adjusted(data, 2);

        int consumedLength = MqttsnWireUtils.isLargeMessage(data) ? 5 : 3;
        if (data.length > consumedLength) {
            gatewayAddress = readRemainingUTF8EncodedAdjustedNoLength(data, 3);
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {
        int length = 3 + (gatewayAddress == null ? 0 : gatewayAddress.getBytes(MqttsnConstants.CHARSET).length);
        byte[] data = new byte[length];
        data[0] = (byte) length;
        data[1] = (byte) getMessageType();
        data[2] = (byte) gatewayId;
        if (gatewayAddress != null) {
            writeUTF8EncodedStringDataNoLength(data, 3, gatewayAddress);
        }
        return data;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateUInt8(gatewayId);
        if (gatewayAddress != null) {
            MqttsnSpecificationValidator.validateStringData(gatewayAddress, false);
        }
    }

    @Override
    public String toString() {
        return "MqttsnGwInfo_V2_0{" +
                "gatewayId=" + gatewayId +
                ", gatewayAddress='" + gatewayAddress + '\'' +
                '}';
    }
}
