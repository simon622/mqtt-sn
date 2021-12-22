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
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;


public abstract class AbstractMqttsnMessageWithFlagsField extends AbstractMqttsnMessage {

    /* set to “0” if message is sent for the first time; set to “1” if retransmitted (only relevant within PUBLISH messages); */
    protected boolean dupRedelivery = false;

    /* meaning as with MQTT for QoS level 0, 1, and 2; set to “0b00” for QoS level 0,
    “0b01” for QoS level1, “0b10” for QoS level 2, and “0b11” for new QoS level -1 */
    protected int QoS;

    /* same meaning as with MQTT (only relevant within PUBLISH messages); */
    protected boolean retainedPublish = false;

    /* if set, indicates that client is asking for Will topic and Will message prompting
    (only relevant within CONNECT message) */
    protected boolean will = false;

    /* same meaning as with MQTT, however extended for Will topic and Will message
    (only relevant within CONNECT message); */
    protected boolean cleanSession = false;

    /* indicates whether the field TopicId or TopicName included in this message contains a normal topic id
    (set to “0b00”), a pre-defined topic id (set to “0b01”), or a short topic name (set to “0b10”).
    The value “0b11” is reserved. */
    protected int topicType;

    public boolean isDupRedelivery() {
        return dupRedelivery;
    }

    public void setDupRedelivery(boolean dupRedelivery) {
        this.dupRedelivery = dupRedelivery;
    }

    public int getQoS() {
        return QoS == 3 ? -1 : QoS;
    }

    public void setQoS(int qoS) {
        if (qoS != MqttsnConstants.QoSM1 &&
                qoS != MqttsnConstants.QoS0 &&
                qoS != MqttsnConstants.QoS1 &&
                qoS != MqttsnConstants.QoS2) {
            throw new IllegalArgumentException("unable to set invalid QoS value on message " + qoS);
        }
        QoS = qoS;
    }

    public boolean isRetainedPublish() {
        return retainedPublish;
    }

    public void setRetainedPublish(boolean retainedPublish) {
        this.retainedPublish = retainedPublish;
    }

    public boolean isWill() {
        return will;
    }

    public void setWill(boolean will) {
        this.will = will;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public int getTopicType() {
        return topicType;
    }

    protected void setTopicType(byte topicType) {
        if (topicType != MqttsnConstants.TOPIC_PREDEFINED &&
                topicType != MqttsnConstants.TOPIC_NORMAL &&
                topicType != MqttsnConstants.TOPIC_SHORT &&
                topicType != MqttsnConstants.TOPIC_FULL) {
            throw new IllegalArgumentException("unable to set invalid topicIdType value on message " + topicType);
        }
        this.topicType = topicType;
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

        //will
        will = ((v & 0x08) >> 3 != 0);

        //clean session
        cleanSession = ((v & 0x04) >> 2 != 0);

        //topic type
        topicType = (v & 0x03);
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
        else if (QoS == MqttsnConstants.QoSM1) v |= 0x60;

        //retained publish
        if (retainedPublish) v |= 0x10;

        //will message
        if (will) v |= 0x08;

        //is clean session
        if (cleanSession) v |= 0x04;

        //topic type
        if (topicType == MqttsnConstants.TOPIC_PREDEFINED) v |= 0x01;
        else if (topicType == MqttsnConstants.TOPIC_SHORT) v |= 0x02;

        return v;
    }
}
