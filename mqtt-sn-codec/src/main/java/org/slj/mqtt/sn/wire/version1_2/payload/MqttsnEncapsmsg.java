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
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;

import java.util.Arrays;

public class MqttsnEncapsmsg extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected byte[] encapsulatedMsg;
    protected byte[] wirelessNodeId;
    int radius;

    public byte[] getEncapsulatedMsg() {
        return encapsulatedMsg;
    }

    public void setEncapsulatedMsg(byte[] encapsulatedMsg) {
        this.encapsulatedMsg = encapsulatedMsg;
    }

    public String getWirelessNodeId() {
        return wirelessNodeId != null && wirelessNodeId.length > 0 ?
                new String(wirelessNodeId, MqttsnConstants.CHARSET) : null;
    }

    public void setWirelessNodeId(String wirelessNodeIdStr) {
        this.wirelessNodeId =
                wirelessNodeIdStr == null ? null : wirelessNodeIdStr.getBytes(MqttsnConstants.CHARSET);
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    @Override
    public int getMessageType() {
        return MqttsnConstants.ENCAPSMSG;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        int length = readMessageLength(data);

        int offset = 0;
        if (isLargeMessage(data)) {
            offset = 2;
        }
        int idx = 2 + offset;
        byte ctrl = data[idx];
        radius = ctrl & 0x03;

        //idx = 4 | 2
        wirelessNodeId = new byte[length - idx];
        if (wirelessNodeId.length > 0) {
            System.arraycopy(data, idx, wirelessNodeId, 0, wirelessNodeId.length);
            idx += wirelessNodeId.length;
        }

        //-- the rest is the next message
        //idx = 4 | 2
        encapsulatedMsg = new byte[data.length - length];
        if (encapsulatedMsg.length > 0) {
            System.arraycopy(data, idx, encapsulatedMsg, 0, encapsulatedMsg.length);
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {


        //Length: 1-octet long, specifies the number of octets up to the end of the "Wireless Node Id" field (incl. the Length octet itself)
        int length = 3 +
                (wirelessNodeId != null ? wirelessNodeId.length : 0);
        byte[] msg;
        int idx = 0;

        if ((length) > 0xFF) {
            length += 2;
            msg = new byte[length];
            msg[idx++] = (byte) 0x01;
            msg[idx++] = ((byte) (0xFF & (length >> 8)));
            msg[idx++] = ((byte) (0xFF & length));
        } else {
            msg = new byte[length];
            msg[idx++] = (byte) length;
        }

        msg[idx++] = (byte) getMessageType();

        //CTRL
//        reserved (bit 7:2)
        byte ctrl = 0;
        ctrl |= radius; //(bit 1,0)
        msg[idx++] = ctrl;

        byte[] nodeId;
        if (wirelessNodeId != null && wirelessNodeId.length > 0) {
            System.arraycopy(wirelessNodeId, 0, msg, idx, wirelessNodeId.length);
            idx += wirelessNodeId.length;
        }

        if (encapsulatedMsg != null && encapsulatedMsg.length > 0) {
            System.arraycopy(encapsulatedMsg, 0, msg, idx, encapsulatedMsg.length);
        }

        return msg;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttsnEncapsmsg{");
        sb.append("wirelessNodeId=").append(Arrays.toString(wirelessNodeId));
        sb.append(", radius=").append(radius);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void validate() throws MqttsnCodecException {

        if(wirelessNodeId != null) MqttsnSpecificationValidator.validStringData(getWirelessNodeId(), false);
        MqttsnSpecificationValidator.validateUInt8(radius);
        MqttsnSpecificationValidator.validateEncapsulatedData(encapsulatedMsg);
    }
}
