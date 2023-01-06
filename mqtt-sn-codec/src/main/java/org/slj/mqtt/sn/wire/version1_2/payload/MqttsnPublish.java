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
import org.slj.mqtt.sn.spi.IMqttsnPublishPacket;

import java.util.Arrays;

public class MqttsnPublish extends AbstractMqttsnMessageWithTopicData implements IMqttsnMessageValidator, IMqttsnPublishPacket {

    public boolean needsId() {
        return true;
    }

    protected byte[] data;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public int getMessageType() {
        return MqttsnConstants.PUBLISH;
    }

    @Override
    public void decode(byte[] arr) throws MqttsnCodecException {
        readFlags(readHeaderByteWithOffset(arr, 2));
        setTopicData(readBytesAdjusted(arr, 3, 2));
        id = readUInt16Adjusted(arr, 5);
        data = readRemainingBytesAdjusted(arr, 7);
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        byte[] msg;
        int length = data.length + 7;
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

        //-- copy in the topic data
        System.arraycopy(topicData, 0, msg, idx, topicData.length);
        idx += topicData.length;

        msg[idx++] = (byte) ((id >> 8) & 0xFF);
        msg[idx++] = (byte) (id & 0xFF);

        System.arraycopy(data, 0, msg, msg.length - (data.length), data.length);
        return msg;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttsnPublish{");
        sb.append("topicData=").append(Arrays.toString(topicData));
        sb.append(", dup=").append(dupRedelivery);
        sb.append(", QoS=").append(QoS);
        sb.append(", retain=").append(retainedPublish);
        sb.append(", topicIdType=").append(topicType);
        sb.append(", msgId=").append(id);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateQoS(getQoS());
        MqttsnSpecificationValidator.validatePublishData(data);
        MqttsnSpecificationValidator.validateTopicIdType(topicType);

        //confirm that when the QoS is M1 we have the correct topicIdTypes sets
        if(getQoS() == MqttsnConstants.QoSM1){
            if(topicType == MqttsnConstants.TOPIC_NORMAL) {
                throw new MqttsnCodecException("invalid topic type defined for QoS -1, must be short, pre or full");
            }
        }

        if(getQoS() <= 0){
            //confirm the msgId is coded 0x0000
            if(id != 0){
                throw new MqttsnCodecException("msgId should not be set for QoS -1 or 0 packets");
            }
        }
    }
}
