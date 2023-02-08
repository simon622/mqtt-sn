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

public class

MqttsnSuback extends AbstractMqttsnMessageWithFlagsField implements IMqttsnMessageValidator {

    protected int topicId;

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
        return MqttsnConstants.SUBACK;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        readFlags(data[2]);
        topicId = readUInt16Adjusted(data, 3);
        id = readUInt16Adjusted(data, 5);
        returnCode = readUInt8Adjusted(data, 7);
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        byte[] data = new byte[8];
        data[0] = (byte) data.length;
        data[1] = (byte) getMessageType();

        data[2] = writeFlags();

        data[3] = (byte) ((topicId >> 8) & 0xFF);
        data[4] = (byte) (topicId & 0xFF);

        data[5] = (byte) ((id >> 8) & 0xFF);
        data[6] = (byte) (id & 0xFF);

        data[7] = (byte) returnCode;

        return data;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttsnSuback{");
        sb.append("topicId=").append(topicId);
        sb.append(", grantedQoS=").append(getQoS());
        sb.append(", msgId=").append(id);
        if(returnCode != 0){
            sb.append(", errorReturnCode=").append(returnCode);
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateTopicAlias(topicId);
        MqttsnSpecificationValidator.validateReturnCode(returnCode);
        MqttsnSpecificationValidator.validateQoS(getQoS());
    }
}