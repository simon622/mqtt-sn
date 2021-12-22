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

package org.slj.mqtt.sn.wire.version2_0.payload;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.util.Arrays;

public class MqttsnSuback_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected int QoS = 0;
    protected int topicIdType = 0;
    protected int topicId = 0;

    @Override
    public int getMessageType() {
        return MqttsnConstants.SUBACK;
    }

    public boolean needsId() {
        return true;
    }

    protected void setTopicType(byte topicType) {
        if (topicType != MqttsnConstants.TOPIC_PREDEFINED &&
                topicType != MqttsnConstants.TOPIC_NORMAL &&
                topicType != MqttsnConstants.TOPIC_SHORT &&
                topicType != MqttsnConstants.TOPIC_FULL) {
            throw new IllegalArgumentException("unable to set invalid topicIdType value on message " + topicType);
        }
        this.topicIdType = topicType;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        readFlags(readHeaderByteWithOffset(data, 2));
        topicId = readUInt16Adjusted(data, 3);
        id = readUInt16Adjusted(data, 5);
        returnCode = readUInt8Adjusted(data, 7);
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        int length = 8;
        int idx = 0;
        byte[] msg = new byte[length];
        msg[idx++] = (byte) length;
        msg[idx++] = (byte) getMessageType();
        msg[idx++] = writeFlags();

        msg[idx++] = (byte) ((topicId >> 8) & 0xFF);
        msg[idx++] = (byte) (topicId & 0xFF);

        msg[idx++] = (byte) ((id >> 8) & 0xFF);
        msg[idx++] = (byte) (id & 0xFF);

        msg[idx++] = (byte) (returnCode);

        return msg;
    }

    protected void readFlags(byte v) {
        /**
          7	        6	5	          4	                    3	2	       1	0
         Reserved	Granted QoS	     Reserved	Reserved   Topic Type
         **/
        QoS = (v & 0x60) >> 5;
        topicIdType = (v & 0x03);
    }

    protected byte writeFlags() {

        byte v = 0x00;

        //qos
        if (QoS == MqttsnConstants.QoS1) v |= 0x20;
        else if (QoS == MqttsnConstants.QoS2) v |= 0x40;
        else if (QoS == MqttsnConstants.QoSM1) v |= 0x60;

        //topic type
        if (topicIdType == MqttsnConstants.TOPIC_PREDEFINED) v |= 0x01;
        else if (topicIdType == MqttsnConstants.TOPIC_SHORT) v |= 0x02;
        else if (topicIdType == MqttsnConstants.TOPIC_FULL) v |= 0x03;

        return v;
    }

    public int getQoS() {
        return QoS;
    }

    public void setQoS(int qoS) {
        QoS = qoS;
    }

    public int getTopicIdType() {
        return topicIdType;
    }

    public void setTopicIdType(int topicIdType) {
        this.topicIdType = topicIdType;
    }

    @Override
    public void validate() throws MqttsnCodecException {

        MqttsnSpecificationValidator.validatePacketIdentifier(id);
        MqttsnSpecificationValidator.validateReturnCode(returnCode);
        MqttsnSpecificationValidator.validateTopicAlias(topicId);
        MqttsnSpecificationValidator.validateTopicIdType(topicIdType);
        MqttsnSpecificationValidator.validateQoS(QoS);
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    @Override
    public String toString() {
        return "MqttsnSuback_V2_0{" +
                "id=" + id +
                ", returnCode=" + returnCode +
                ", QoS=" + QoS +
                ", topicIdType=" + topicIdType +
                ", topicId=" + topicId +
                '}';
    }
}