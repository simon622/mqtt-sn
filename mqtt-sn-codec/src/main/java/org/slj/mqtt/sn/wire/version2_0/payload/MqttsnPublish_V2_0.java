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
import org.slj.mqtt.sn.spi.IMqttsnPublishPacket;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.util.Arrays;

public class MqttsnPublish_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator, IMqttsnPublishPacket {

    public boolean needsId() {
        return true;
    }

    protected int QoS;
    protected int topicIdType;
    protected short protocolVersion = MqttsnConstants.PROTOCOL_VERSION_2_0;
    protected int topicLength;
    protected byte[] data;
    protected byte[] topicData;
    protected boolean dupRedelivery;
    protected boolean retainedPublish;

    public void setTopicName(String topicName) {
        setTopicType(topicName != null && topicName.length() <= 2 ? MqttsnConstants.TOPIC_SHORT : MqttsnConstants.TOPIC_NORMAL);
        if(topicName.length() == 1){
            topicData = new byte[]{topicName.getBytes(MqttsnConstants.CHARSET)[0], 0x00};
        } else {
            topicData = topicName.getBytes(MqttsnConstants.CHARSET);
        }

        topicLength = topicData.length;
    }

    public void setPredefinedTopicAlias(int topicAlias) {
        setTopicType(MqttsnConstants.TOPIC_PREDEFINED);
        setTopicAliasId(topicAlias);
        topicLength = 2;
    }

    public void setNormalTopicAlias(int topicAlias) {
        setTopicType(MqttsnConstants.TOPIC_NORMAL);
        setTopicAliasId(topicAlias);
        topicLength = 2;
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

    public int readTopicDataAsInteger() {
        return MqttsnWireUtils.read16bit(topicData[0], topicData[1]);
    }

    public byte[] getTopicData() {
        return topicData;
    }

    protected void setTopicData(byte[] data) {
        topicData = data;
    }

    protected void setTopicAliasId(int topicAlias) {
        topicData = new byte[2];
        topicData[0] = (byte) ((topicAlias >> 8) & 0xFF);
        topicData[1] = (byte) (topicAlias & 0xFF);
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getQoS() {
        return QoS == 3 ? -1 : QoS;
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

    public int getTopicLength() {
        return topicLength;
    }

    public void setTopicLength(int topicLength) {
        this.topicLength = topicLength;
    }

    public boolean isDupRedelivery() {
        return dupRedelivery;
    }

    public void setDupRedelivery(boolean dupRedelivery) {
        this.dupRedelivery = dupRedelivery;
    }

    public boolean isRetainedPublish() {
        return retainedPublish;
    }

    public void setRetainedPublish(boolean retainedPublish) {
        this.retainedPublish = retainedPublish;
    }

    @Override
    public int getMessageType() {
        return getQoS() == MqttsnConstants.QoSM1 ? MqttsnConstants.PUBLISH_M1 : MqttsnConstants.PUBLISH;
    }

    public short getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public void decode(byte[] arr) throws MqttsnCodecException {

        boolean isPublishM1 = MqttsnWireUtils.readMessageType(arr) == MqttsnConstants.PUBLISH_M1;
        readFlags(readHeaderByteWithOffset(arr, isPublishM1 ? 3 : 2));
        int qos = getQoS();
        if(isPublishM1 && qos != MqttsnConstants.QoSM1){
            throw new MqttsnCodecException("invalid QoS detected ("+qos+") for PUBLISH_M1 packet type");
        }

        //-- limited format
        if(qos == MqttsnConstants.QoSM1){
            protocolVersion = readUInt8Adjusted(arr, 2);
            if(topicIdType == MqttsnConstants.TOPIC_FULL){
                //first 2 bytes of payload are topic length
                topicLength =  readUInt16Adjusted(arr, 4);
                setTopicData(readBytesAdjusted(arr, 6, topicLength));
                data = readRemainingBytesAdjusted(arr,  6 + topicLength);
            } else {
                topicLength = 2;
                setTopicData(readBytesAdjusted(arr, 4, 2));
                data = readRemainingBytesAdjusted(arr,  6);
            }
        }
        else if(qos == MqttsnConstants.QoS0){
            if(topicIdType == MqttsnConstants.TOPIC_FULL){
                //first 2 bytes of payload are topic length
                topicLength =  readUInt16Adjusted(arr, 3);
                setTopicData(readBytesAdjusted(arr, 5, topicLength));
                data = readRemainingBytesAdjusted(arr,  5 + topicLength);
            } else {
                topicLength = 2;
                setTopicData(readBytesAdjusted(arr, 3, 2));
                data = readRemainingBytesAdjusted(arr,  5);
            }
        } else {

            //-- packet id format
            id = readUInt16Adjusted(arr, 3);

            if(topicIdType == MqttsnConstants.TOPIC_FULL){
                //first 2 bytes of payload are topic length

                topicLength =  readUInt16Adjusted(arr, 5);
                setTopicData(readBytesAdjusted(arr, 7, topicLength));
                data = readRemainingBytesAdjusted(arr,  7 + topicLength);
            } else {
                topicLength = 2;
                setTopicData(readBytesAdjusted(arr, 5, topicLength));
                data = readRemainingBytesAdjusted(arr,  5 + topicLength);
            }
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        byte[] msg;
//        int length = data.length + (topicLength - 2) + (getQoS() <= 0 ? 5 : 7);

        int qos = getQoS();
        int length = data.length + topicLength - 2;
        if(qos == MqttsnConstants.QoSM1){
            length += 6;
        } else if (qos == MqttsnConstants.QoS0) {
            length += 5;
        } else {
            length += 7;
        }

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

        if(qos == MqttsnConstants.QoSM1){
            //write the protocolVersion
            msg[idx++] = (byte) protocolVersion;
        }

        msg[idx++] = writeFlags();

        //-- encode the packetid for varient 2 packet types
        if(getQoS() >= 1){
            msg[idx++] = (byte) ((id >> 8) & 0xFF);
            msg[idx++] = (byte) (id & 0xFF);
        }

        topicLength = topicLength == 0 ? topicIdType == MqttsnConstants.TOPIC_FULL ? topicData.length : 2 : 2;

        if(topicIdType == MqttsnConstants.TOPIC_FULL){
            msg[idx++] = (byte) ((topicLength >> 8) & 0xFF);
            msg[idx++] = (byte) (topicLength & 0xFF);
        } else {
            System.arraycopy(topicData, 0, msg, idx, topicData.length);
        }


        System.arraycopy(data, 0, msg, msg.length - (data.length), data.length);
        return msg;
    }

    protected void readFlags(byte v) {
        /**
         DUP      QoS   Retain Will  CleanSession TopicIdType
         (bit 7) (6,5)  (4)     (3)    (2)          (1,0)
         **/

        //error redelivery
        dupRedelivery = ((v & 0x80) >> 7 != 0);

        //qos
        QoS = (v & 0x60) >> 5;

        //retained publish
        retainedPublish = ((v & 0x10) >> 4 != 0);

        //topic type
        topicIdType = (v & 0x03);
    }

    protected byte writeFlags() {
        /**
         DUP      QoS   Retain Will  CleanSession TopicIdType
         (bit 7) (6,5)  (4)     (3)    (2)          (1,0)
         **/

        byte v = 0x00;

        //dup redelivery
        if (dupRedelivery) v |= 0x80;

        //qos
        if (QoS == MqttsnConstants.QoS1) v |= 0x20;
        else if (QoS == MqttsnConstants.QoS2) v |= 0x40;
        else if (QoS == MqttsnConstants.QoSM1 || QoS == 3) v |= 0x60;
        else if (QoS == MqttsnConstants.QoS0);
        else throw new MqttsnCodecException("unable to write invalid QoS to flags");

        //retained publish
        if (retainedPublish) v |= 0x10;

        //topic type
        if (topicIdType == MqttsnConstants.TOPIC_PREDEFINED) v |= 0x01;
        else if (topicIdType == MqttsnConstants.TOPIC_SHORT) v |= 0x02;
        else if (topicIdType == MqttsnConstants.TOPIC_FULL) v |= 0x03;

        return v;
    }

    @Override
    public String toString() {
        return "MqttsnPublish_V2_0{" +
                "QoS=" + getQoS() +
                ", topicIdType=" + topicIdType +
                ", protocolVersion=" + protocolVersion +
                ", topicLength=" + topicLength +
                ", data=" + Arrays.toString(data) +
                ", topicData=" + Arrays.toString(topicData) +
                ", dupRedelivery=" + dupRedelivery +
                ", retainedPublish=" + retainedPublish +
                '}';
    }

    @Override
    public void validate() throws MqttsnCodecException {

        MqttsnSpecificationValidator.validatePacketIdentifier(id);
        MqttsnSpecificationValidator.validateUInt16(topicLength);
        MqttsnSpecificationValidator.validateTopicIdType(topicIdType);
        MqttsnSpecificationValidator.validateQoS(getQoS());
        MqttsnSpecificationValidator.validatePublishData(data);
        MqttsnSpecificationValidator.validateProtocolId(protocolVersion);

        //confirm that when the QoS is M1 we have the correct topicIdTypes sets
        if(getQoS() == MqttsnConstants.QoSM1){
            if(topicIdType == MqttsnConstants.TOPIC_NORMAL) {
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
