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
 * SUBACK - wire format per OASIS mqtt-sn-v2.0 CSD01 (05 Feb 2026), section 3.8, Figure 19.
 *
 * NB: unlike v1.2, there is no dedicated QoS field - the "granted QoS" is instead expressed
 * via the Reason Code itself (Table 4: Reason Codes 0x00/0x01/0x02 ARE "Granted QoS 0/1/2").
 * {@link #getQoS()}/{@link #setQoS(int)} are kept as convenience aliases onto the Reason Code
 * for API compatibility with existing runtime call sites.
 */
public class MqttsnSuback_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected boolean topicAliasSet;
    protected boolean reasonCodeSet;
    protected int topicIdType = 0;
    protected int topicId = 0;

    @Override
    public int getMessageType() {
        return MqttsnConstants.SUBACK_V2_0;
    }

    @Override
    public boolean needsId() {
        return true;
    }

    @Override
    public boolean isErrorMessage() {
        //-- MQTT-SN 2.0 (CSD01) 2.3: Reason Codes < 0x80 indicate success (which, for SUBACK,
        //-- includes the non-zero "Granted QoS 1/2" codes) - only >= 0x80 is failure.
        return (returnCode & 0xFF) >= 0x80;
    }

    @Override
    public void setReturnCode(int returnCode) {
        reasonCodeSet = true;
        super.setReturnCode(returnCode);
    }

    public int getQoS() {
        return returnCode;
    }

    public void setQoS(int qoS) {
        setReturnCode(qoS);
    }

    public int getTopicIdType() {
        return topicIdType;
    }

    public void setTopicIdType(int topicIdType) {
        this.topicIdType = topicIdType;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
        this.topicAliasSet = true;
    }

    protected void readFlags(byte v) {
        /**
         Reserved      Topic Alias  Topic Type
         (7,6,5,4,3)   (2)          (1,0)
         **/

        if ((v & 0xF8) != 0) {
            throw new MqttsnCodecException("reserved suback flags must be set to 0");
        }

        topicAliasSet = (v & 0x04) != 0;
        topicIdType = (v & 0x03);
    }

    protected byte writeFlags() {
        byte v = 0x00;
        if (topicAliasSet) v |= 0x04;
        v |= (topicIdType & 0x03);
        return v;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        int idx = 2;
        readFlags(readHeaderByteWithOffset(data, idx++));

        id = readUInt16Adjusted(data, idx);
        idx += 2;

        if (topicAliasSet) {
            topicId = readUInt16Adjusted(data, idx);
            idx += 2;
        }

        int consumedLength = MqttsnWireUtils.isLargeMessage(data) ? idx + 2 : idx;
        if (data.length > consumedLength) {
            returnCode = readUInt8Adjusted(data, idx);
            reasonCodeSet = true;
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        //-- length(1) + type(1) + flags(1) + packetId(2)
        int length = 5;
        if (topicAliasSet) length += 2;
        if (reasonCodeSet) length += 1;

        byte[] msg = new byte[length];
        int idx = 0;
        msg[idx++] = (byte) length;
        msg[idx++] = (byte) getMessageType();
        msg[idx++] = writeFlags();

        msg[idx++] = (byte) ((id >> 8) & 0xFF);
        msg[idx++] = (byte) (id & 0xFF);

        if (topicAliasSet) {
            msg[idx++] = (byte) ((topicId >> 8) & 0xFF);
            msg[idx++] = (byte) (topicId & 0xFF);
        }

        if (reasonCodeSet) {
            msg[idx] = (byte) getReturnCode();
        }

        return msg;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validatePacketIdentifier(id);
        MqttsnSpecificationValidator.validateTopicIdType(topicIdType);
        //-- MQTT-SN 2.0 (CSD01) 3.8.2.1: Topic Type in SUBACK MUST be Predefined or Session
        //-- Topic Alias; and if no Topic Alias is returned, Topic Type MUST be Predefined.
        if (topicIdType != MqttsnConstants.TOPIC_PREDEFINED && topicIdType != MqttsnConstants.TOPIC_NORMAL) {
            throw new MqttsnCodecException("SUBACK topic type must be Predefined or Session Topic Alias");
        }
        if (!topicAliasSet && topicIdType != MqttsnConstants.TOPIC_PREDEFINED) {
            throw new MqttsnCodecException("SUBACK topic type must be Predefined Topic Alias when no Topic Alias is returned");
        }
        if (topicAliasSet) {
            MqttsnSpecificationValidator.validateTopicAlias(topicId);
        }
        if (reasonCodeSet) {
            MqttsnSpecificationValidator.validateReturnCode(returnCode);
        }
    }

    @Override
    public String toString() {
        return "MqttsnSuback_V2_0{" +
                "id=" + id +
                ", returnCode=" + returnCode +
                ", topicIdType=" + topicIdType +
                ", topicId=" + topicId +
                ", topicAliasSet=" + topicAliasSet +
                '}';
    }
}
