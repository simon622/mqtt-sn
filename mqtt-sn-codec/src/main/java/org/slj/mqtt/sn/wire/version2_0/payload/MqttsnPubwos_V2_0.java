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
import org.slj.mqtt.sn.spi.IMqttsnPublishPacket;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.util.Arrays;

/**
 * PUBWOS (Publish Without Session) - wire format per OASIS mqtt-sn-v2.0 CSD01 (05 Feb 2026),
 * section 3.6.1, Figure 11. No Session/Virtual Connection required, no Packet Identifier, no
 * QoS field (always treated as QoS 0), no DUP.
 */
public class MqttsnPubwos_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator, IMqttsnPublishPacket {

    protected boolean retainedPublish;
    protected int topicIdType;
    protected byte[] topicData;
    protected byte[] data;

    @Override
    public int getMessageType() {
        return MqttsnConstants.PUBWOS_V2_0;
    }

    @Override
    public boolean needsId() {
        return false;
    }

    @Override
    public int getQoS() {
        //-- PUBWOS has no QoS field on the wire; MUST be treated as if QoS were 0 (3.6.1.6).
        return MqttsnConstants.QoS0;
    }

    public boolean isRetainedPublish() {
        return retainedPublish;
    }

    public void setRetainedPublish(boolean retainedPublish) {
        this.retainedPublish = retainedPublish;
    }

    public int getTopicIdType() {
        return topicIdType;
    }

    public void setTopicIdType(int topicIdType) {
        this.topicIdType = topicIdType;
    }

    public void setTopicName(String topicName) {
        this.topicIdType = MqttsnConstants.TOPIC_FULL;
        this.topicData = topicName.getBytes(MqttsnConstants.CHARSET);
    }

    public void setPredefinedTopicAlias(int topicAlias) {
        this.topicIdType = MqttsnConstants.TOPIC_PREDEFINED;
        this.topicData = new byte[]{(byte) ((topicAlias >> 8) & 0xFF), (byte) (topicAlias & 0xFF)};
    }

    public int readTopicDataAsInteger() {
        return MqttsnWireUtils.read16bit(topicData[0], topicData[1]);
    }

    public byte[] getTopicData() {
        return topicData;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    protected void readFlags(byte v) {
        /**
         Reserved  Retain  Reserved  TopicType
         (7,6,5)   (4)     (3,2)     (1,0)
         **/

        if ((v & 0xEC) != 0) {
            throw new MqttsnCodecException("reserved pubwos flags must be set to 0");
        }

        retainedPublish = (v & 0x10) != 0;
        topicIdType = (v & 0x03);
    }

    protected byte writeFlags() {
        byte v = 0x00;
        if (retainedPublish) v |= 0x10;
        v |= (topicIdType & 0x03);
        return v;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        readFlags(readHeaderByteWithOffset(data, 2));

        int topicAliasOrNameLength = readUInt16Adjusted(data, 3);
        if (topicIdType == MqttsnConstants.TOPIC_FULL) {
            topicData = readBytesAdjusted(data, 5, topicAliasOrNameLength);
            this.data = readRemainingBytesAdjusted(data, 5 + topicAliasOrNameLength);
        } else {
            topicData = new byte[]{
                    (byte) ((topicAliasOrNameLength >> 8) & 0xFF),
                    (byte) (topicAliasOrNameLength & 0xFF)};
            this.data = readRemainingBytesAdjusted(data, 5);
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        int topicAliasOrNameLength = topicIdType == MqttsnConstants.TOPIC_FULL ?
                topicData.length : readTopicDataAsInteger();

        int length = 5 + (topicIdType == MqttsnConstants.TOPIC_FULL ? topicData.length : 0) +
                (data == null ? 0 : data.length);

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
        msg[idx++] = writeFlags();

        msg[idx++] = (byte) ((topicAliasOrNameLength >> 8) & 0xFF);
        msg[idx++] = (byte) (topicAliasOrNameLength & 0xFF);

        if (topicIdType == MqttsnConstants.TOPIC_FULL) {
            System.arraycopy(topicData, 0, msg, idx, topicData.length);
            idx += topicData.length;
        }

        if (data != null) {
            System.arraycopy(data, 0, msg, idx, data.length);
        }

        return msg;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateTopicIdType(topicIdType);
        //-- MQTT-SN 2.0 (CSD01) 3.6.1.2.1: Topic Type in PUBWOS MUST be Predefined Topic Alias
        //-- or Topic Name - Session Topic Alias makes no sense without a Session, and Reserved
        //-- (SHORT) is not a valid v2.0 topic type at all.
        if (topicIdType != MqttsnConstants.TOPIC_PREDEFINED && topicIdType != MqttsnConstants.TOPIC_FULL) {
            throw new MqttsnCodecException("PUBWOS topic type must be Predefined Topic Alias or Topic Name");
        }
        if (data == null) {
            throw new MqttsnCodecException("publish data cannot be null");
        }
    }

    @Override
    public String toString() {
        return "MqttsnPubwos_V2_0{" +
                "retainedPublish=" + retainedPublish +
                ", topicIdType=" + topicIdType +
                ", topicData=" + Arrays.toString(topicData) +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
