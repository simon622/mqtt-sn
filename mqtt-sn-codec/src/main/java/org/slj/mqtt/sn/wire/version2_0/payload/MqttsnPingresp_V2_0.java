/*
 * Copyright (c) 2021-2026 Simon Johnson <simon622 AT gmail DOT com>, Ian Craggs
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
 * PINGRESP - wire format per OASIS mqtt-sn-v2.0 CSD01 (05 Feb 2026), section 3.12, Figure 23.
 *
 * NB: unlike earlier drafts, this carries a mandatory Packet Identifier (matching the PINGREQ
 * being acknowledged) followed by an optional Application Messages Remaining byte - not a
 * Messages Remaining byte immediately after the type with no Packet Identifier at all.
 */
public class MqttsnPingresp_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected boolean applicationMessagesRemainingSet;
    protected int applicationMessagesRemaining;

    @Override
    public int getMessageType() {
        return MqttsnConstants.PINGRESP_V2_0;
    }

    @Override
    public boolean needsId() {
        return true;
    }

    public int getApplicationMessagesRemaining() {
        return applicationMessagesRemaining;
    }

    public void setApplicationMessagesRemaining(int applicationMessagesRemaining) {
        this.applicationMessagesRemaining = applicationMessagesRemaining;
        this.applicationMessagesRemainingSet = true;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        id = readUInt16Adjusted(data, 2);

        int consumedLength = MqttsnWireUtils.isLargeMessage(data) ? 6 : 4;
        if (data.length > consumedLength) {
            applicationMessagesRemaining = readUInt8Adjusted(data, 4);
            applicationMessagesRemainingSet = true;
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        int length = 4 + (applicationMessagesRemainingSet ? 1 : 0);
        byte[] data = new byte[length];
        data[0] = (byte) length;
        data[1] = (byte) getMessageType();
        data[2] = (byte) ((id >> 8) & 0xFF);
        data[3] = (byte) (id & 0xFF);
        if (applicationMessagesRemainingSet) {
            data[4] = (byte) applicationMessagesRemaining;
        }
        return data;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validatePacketIdentifier(id);
        if (applicationMessagesRemainingSet) {
            MqttsnSpecificationValidator.validateUInt8(applicationMessagesRemaining);
        }
    }

    @Override
    public String toString() {
        return "MqttsnPingresp_V2_0{" +
                "id=" + id +
                ", applicationMessagesRemaining=" + applicationMessagesRemaining +
                ", applicationMessagesRemainingSet=" + applicationMessagesRemainingSet +
                '}';
    }
}
