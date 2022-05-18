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

public class MqttsnRegack_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected int topicId;
    protected int topicIdType;

    public boolean needsId() {
        return true;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    @Override
    public int getMessageType() {
        return MqttsnConstants.REGACK;
    }

    public int getTopicIdType() {
        return topicIdType;
    }

    public void setTopicIdType(int topicIdType) {
        this.topicIdType = topicIdType;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        readFlags(data[2]);

        topicId = readUInt16Adjusted(data, 3);
        id = readUInt16Adjusted(data, 5);
        returnCode = (data[7] & 0xFF);
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        byte[] data = new byte[8];
        int idx = 0;
        data[idx++] = (byte) data.length;
        data[idx++] = (byte) getMessageType();

        data[idx++] = writeFlags();

        data[idx++] = (byte) ((topicId >> 8) & 0xFF);
        data[idx++] = (byte) (topicId & 0xFF);

        data[idx++] = (byte) ((id >> 8) & 0xFF);
        data[idx++] = (byte) (id & 0xFF);

        data[idx++] = (byte) returnCode;

        return data;
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

    protected void readFlags(byte v) {
        //topic type
        topicIdType = (v & 0x03);
    }

    protected byte writeFlags() {

        byte v = 0x00;

        //topic type
        if (topicIdType == MqttsnConstants.TOPIC_PREDEFINED) v |= 0x01;
        else if (topicIdType == MqttsnConstants.TOPIC_SHORT) v |= 0x02;
        else if (topicIdType == MqttsnConstants.TOPIC_FULL) v |= 0x03;

        return v;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttsnRegack{");
        sb.append("topicId=").append(topicId);
        sb.append(", topicIdType=").append(topicIdType);
        sb.append(", id=").append(id);
        if(returnCode != 0){
            sb.append(", errorReturnCode=").append(returnCode);
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateTopicIdType(topicIdType);
        MqttsnSpecificationValidator.validateTopicAlias(topicId);
        MqttsnSpecificationValidator.validateReturnCode(returnCode);
    }
}