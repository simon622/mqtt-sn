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
import org.slj.mqtt.sn.wire.version1_2.payload.AbstractMqttsnSubscribeUnsubscribe;

import java.util.Arrays;

public class MqttsnSubscribe_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected boolean noLocal = false;
    protected boolean retainAsPublished = false;
    protected int retainHandling = 0;
    protected int QoS = 0;
    protected int topicIdType = 0;
    protected byte[] topicData;

    @Override
    public int getMessageType() {
        return MqttsnConstants.SUBSCRIBE;
    }

    public boolean needsId() {
        return true;
    }

    public String getTopicName() {
        if (topicIdType == MqttsnConstants.TOPIC_PREDEFINED)
            throw new IllegalStateException("unable to parse string data from predefined topic alias");
        else if(topicIdType == MqttsnConstants.TOPIC_SHORT){
            //-- handle single char short topic names
            if(topicData.length == 2 && topicData[1] == 0x00)
                return new String(new byte[]{topicData[0]}, MqttsnConstants.CHARSET);
        }
        return new String(topicData, MqttsnConstants.CHARSET);
    }

    public void setTopicName(String topicName) {
        setTopicType(topicName != null && topicName.length() <= 2 ?
                MqttsnConstants.TOPIC_SHORT : MqttsnConstants.TOPIC_FULL);
        if(topicName.length() == 1){
            topicData = new byte[]{topicName.getBytes(MqttsnConstants.CHARSET)[0], 0x00};
        } else {
            topicData = topicName.getBytes(MqttsnConstants.CHARSET);
        }
    }

    public void setPredefinedTopicAlias(int topicAlias) {
        setTopicType(MqttsnConstants.TOPIC_PREDEFINED);
        setTopicAliasId(topicAlias);
    }

    public void setNormalTopicAlias(int topicAlias) {
        setTopicType(MqttsnConstants.TOPIC_NORMAL);
        setTopicAliasId(topicAlias);
    }

    public int readTopicDataAsInteger() {
        return MqttsnWireUtils.read16bit(topicData[0], topicData[1]);
    }

    public byte[] getTopicData() {
        return topicData;
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

    protected void setTopicData(byte[] data) {
        topicData = data;
    }

    protected void setTopicAliasId(int topicAlias) {
        topicData = new byte[2];
        topicData[0] = (byte) ((topicAlias >> 8) & 0xFF);
        topicData[1] = (byte) (topicAlias & 0xFF);
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        readFlags(readHeaderByteWithOffset(data, 2));
        id = readUInt16Adjusted(data, 3);
        topicData = readRemainingBytesAdjusted(data, 5);
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        byte[] msg;
        int length = 5 + topicData.length;
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

        msg[idx++] = (byte) ((id >> 8) & 0xFF);
        msg[idx++] = (byte) (id & 0xFF);

        if (topicData != null && topicData.length > 0) {
            System.arraycopy(topicData, 0, msg, idx, topicData.length);
        }

        return msg;
    }

    protected void readFlags(byte v) {
        /**
          7	        6	5	          4	                    3	2	       1	0
         No Local	QoS	     Retain as Published	Retain Handling	    Topic Type
         **/

        //noLocal
        noLocal = ((v & 0x80) >> 7 != 0);

        //qos
        QoS = (v & 0x60) >> 5;

        //retained publish
        retainAsPublished = ((v & 0x10) >> 4 != 0);

        //retained publish
        retainHandling = (v & 0x0C) >> 2;

        //topic type
        topicIdType = (v & 0x03);
    }

    protected byte writeFlags() {
        /**
         7	        6	5	          4	                    3	2	       1	0
         No Local	QoS	     Retain as Published	Retain Handling	    Topic Type
         **/

        /**
         DUP      QoS   Retain Will  CleanSession TopicIdType
         (bit 7) (6,5)  (4)     (3)    (2)          (1,0)
         **/

        byte v = 0x00;

        //noLocal
        if (noLocal) v |= 0x80;

        //qos
        if (QoS == MqttsnConstants.QoS1) v |= 0x20;
        else if (QoS == MqttsnConstants.QoS2) v |= 0x40;
        else if (QoS == MqttsnConstants.QoSM1) v |= 0x60;

        //retained publish
        if (retainAsPublished) v |= 0x10;

        //retainHandling
        if (retainHandling == MqttsnConstants.RETAINED_SEND_NOT_EXISTS) v |= 0x04;
        else if (retainHandling == MqttsnConstants.RETAINED_NO_SEND) v |= 0x08;

        //topic type
        if (topicIdType == MqttsnConstants.TOPIC_PREDEFINED) v |= 0x01;
        else if (topicIdType == MqttsnConstants.TOPIC_SHORT) v |= 0x02;
        else if (topicIdType == MqttsnConstants.TOPIC_FULL) v |= 0x03;

        return v;
    }

    public boolean isNoLocal() {
        return noLocal;
    }

    public void setNoLocal(boolean noLocal) {
        this.noLocal = noLocal;
    }

    public boolean isRetainAsPublished() {
        return retainAsPublished;
    }

    public void setRetainAsPublished(boolean retainAsPublished) {
        this.retainAsPublished = retainAsPublished;
    }

    public int getRetainHandling() {
        return retainHandling;
    }

    public void setRetainHandling(int retainHandling) {
        this.retainHandling = retainHandling;
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
        MqttsnSpecificationValidator.validateRetainHandling(retainHandling);
        MqttsnSpecificationValidator.validateTopicIdType(topicIdType);
        MqttsnSpecificationValidator.validateQoS(QoS);
    }

    @Override
    public String toString() {
        return "MqttsnSubscribe_V2_0{" +
                "id=" + id +
                ", returnCode=" + returnCode +
                ", noLocal=" + noLocal +
                ", retainAsPublished=" + retainAsPublished +
                ", retainHandling=" + retainHandling +
                ", QoS=" + QoS +
                ", topicIdType=" + topicIdType +
                ", topicData=" + Arrays.toString(topicData) +
                '}';
    }
}