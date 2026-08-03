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
 * SLEEPREQ - wire format per OASIS mqtt-sn-v2.0 CSD01 (05 Feb 2026), section 3.15, Figure 26.
 * Client-to-Server request to move to the Asleep state. Fixed-length: Packet Identifier and
 * Sleep Duration are always present (unlike most other v2.0 packets, neither is conditional).
 */
public class MqttsnSleepreq_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected boolean retainTopicAliases;
    protected long sleepDuration;

    @Override
    public int getMessageType() {
        return MqttsnConstants.SLEEPREQ_V2_0;
    }

    @Override
    public boolean needsId() {
        return true;
    }

    public boolean isRetainTopicAliases() {
        return retainTopicAliases;
    }

    public void setRetainTopicAliases(boolean retainTopicAliases) {
        this.retainTopicAliases = retainTopicAliases;
    }

    public long getSleepDuration() {
        return sleepDuration;
    }

    public void setSleepDuration(long sleepDuration) {
        this.sleepDuration = sleepDuration;
    }

    protected void readFlags(byte v) {
        if ((v & 0xFE) != 0) {
            throw new MqttsnCodecException("reserved sleepreq flags must be set to 0");
        }
        retainTopicAliases = (v & 0x01) != 0;
    }

    protected byte writeFlags() {
        return (byte) (retainTopicAliases ? 0x01 : 0x00);
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        readFlags(readHeaderByteWithOffset(data, 2));
        id = readUInt16Adjusted(data, 3);
        sleepDuration = readUInt32Adjusted(data, 5);
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {
        byte[] data = new byte[9];
        data[0] = (byte) data.length;
        data[1] = (byte) getMessageType();
        data[2] = writeFlags();
        data[3] = (byte) ((id >> 8) & 0xFF);
        data[4] = (byte) (id & 0xFF);
        writeUInt32(data, 5, sleepDuration);
        return data;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validatePacketIdentifier(id);
        MqttsnSpecificationValidator.validateUInt32(sleepDuration);
        if (sleepDuration <= 0) {
            throw new MqttsnCodecException("sleep duration must be greater than 0");
        }
    }

    @Override
    public String toString() {
        return "MqttsnSleepreq_V2_0{" +
                "id=" + id +
                ", retainTopicAliases=" + retainTopicAliases +
                ", sleepDuration=" + sleepDuration +
                '}';
    }
}
