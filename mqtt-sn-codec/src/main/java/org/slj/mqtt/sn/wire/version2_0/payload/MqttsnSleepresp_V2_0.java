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
 * SLEEPRESP - wire format per OASIS mqtt-sn-v2.0 CSD01 (05 Feb 2026), section 3.16, Figure 27.
 */
public class MqttsnSleepresp_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected boolean sleepDurationSet;
    protected boolean reasonCodeSet;
    protected long sleepDuration;

    @Override
    public int getMessageType() {
        return MqttsnConstants.SLEEPRESP_V2_0;
    }

    @Override
    public boolean needsId() {
        return true;
    }

    public long getSleepDuration() {
        return sleepDuration;
    }

    public void setSleepDuration(long sleepDuration) {
        this.sleepDuration = sleepDuration;
        this.sleepDurationSet = sleepDuration > 0;
    }

    @Override
    public void setReturnCode(int returnCode) {
        reasonCodeSet = true;
        super.setReturnCode(returnCode);
    }

    protected void readFlags(byte v) {
        if ((v & 0xFE) != 0) {
            throw new MqttsnCodecException("reserved sleepresp flags must be set to 0");
        }
        sleepDurationSet = (v & 0x01) != 0;
    }

    protected byte writeFlags() {
        return (byte) (sleepDurationSet ? 0x01 : 0x00);
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        int idx = 2;
        readFlags(readHeaderByteWithOffset(data, idx++));

        id = readUInt16Adjusted(data, idx);
        idx += 2;

        if (sleepDurationSet) {
            sleepDuration = readUInt32Adjusted(data, idx);
            idx += 4;
        }

        int consumedLength = MqttsnWireUtils.isLargeMessage(data) ? idx + 2 : idx;
        if (data.length > consumedLength) {
            returnCode = readUInt8Adjusted(data, idx);
            reasonCodeSet = true;
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        int length = 5;
        if (sleepDurationSet) length += 4;
        if (reasonCodeSet) length += 1;

        byte[] data = new byte[length];
        int idx = 0;
        data[idx++] = (byte) length;
        data[idx++] = (byte) getMessageType();
        data[idx++] = writeFlags();

        data[idx++] = (byte) ((id >> 8) & 0xFF);
        data[idx++] = (byte) (id & 0xFF);

        if (sleepDurationSet) {
            writeUInt32(data, idx, sleepDuration);
            idx += 4;
        }

        if (reasonCodeSet) {
            data[idx] = (byte) getReturnCode();
        }

        return data;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validatePacketIdentifier(id);
        if (sleepDurationSet) {
            MqttsnSpecificationValidator.validateUInt32(sleepDuration);
        }
        if (reasonCodeSet) {
            MqttsnSpecificationValidator.validateReturnCode(returnCode);
        }
    }

    @Override
    public String toString() {
        return "MqttsnSleepresp_V2_0{" +
                "id=" + id +
                ", sleepDuration=" + sleepDuration +
                ", sleepDurationSet=" + sleepDurationSet +
                ", returnCode=" + returnCode +
                '}';
    }
}
