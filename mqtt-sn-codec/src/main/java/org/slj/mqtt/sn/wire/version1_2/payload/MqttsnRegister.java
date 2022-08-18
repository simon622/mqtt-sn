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

public class MqttsnRegister extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected int topicId;
    protected String topicName;

    public boolean needsId() {
        return true;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    @Override
    public int getMessageType() {
        return MqttsnConstants.REGISTER;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        if (isLargeMessage(data)) {
            topicId = readUInt16Adjusted(data, 4);
            id = readUInt16Adjusted(data, 6);
        } else {
            topicId = readUInt16Adjusted(data, 2);
            id = readUInt16Adjusted(data, 4);
        }

        byte[] body = readRemainingBytesAdjusted(data, 6);
        if (body.length > 0) {
            topicName = new String(body, MqttsnConstants.CHARSET);
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        byte[] topicByteArr = null;
        int length = 6 + (topicName == null ? 0 : ((topicByteArr = topicName.getBytes(MqttsnConstants.CHARSET)).length));
        byte[] msg = null;
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

        msg[idx++] = ((byte) (0xFF & (topicId >> 8)));
        msg[idx++] = ((byte) (0xFF & topicId));

        msg[idx++] = ((byte) (0xFF & (id >> 8)));
        msg[idx++] = ((byte) (0xFF & id));

        if (topicByteArr != null && topicByteArr.length > 0) {
            System.arraycopy(topicByteArr, 0, msg, idx, topicByteArr.length);
        }

        return msg;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttsnRegister{");
        sb.append("topicId=").append(topicId);
        sb.append(", topicName='").append(topicName).append('\'');
        sb.append(", id=").append(id);
        sb.append('}');
        return sb.toString();
    }


    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validatePublishPath(topicName);
        MqttsnSpecificationValidator.validateTopicAlias(topicId);
    }
}