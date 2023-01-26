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

package org.slj.mqtt.sn.wire.version2_0;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.codec.AbstractMqttsnMessageFactory;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnMessageFactory;
import org.slj.mqtt.sn.wire.version1_2.Mqttsn_v1_2_MessageFactory;
import org.slj.mqtt.sn.wire.version1_2.payload.*;
import org.slj.mqtt.sn.wire.version2_0.payload.*;

public class Mqttsn_v2_0_MessageFactory extends Mqttsn_v1_2_MessageFactory implements IMqttsnMessageFactory {

    //singleton
    private static volatile Mqttsn_v2_0_MessageFactory instance;


    protected Mqttsn_v2_0_MessageFactory() {
    }

    public static IMqttsnMessageFactory getInstance() {
        if (instance == null) {
            synchronized (Mqttsn_v2_0_MessageFactory.class) {
                if (instance == null) instance = new Mqttsn_v2_0_MessageFactory();
            }
        }
        return instance;
    }

    @Override
    public IMqttsnMessage createConnect(String clientId, int keepAlive, boolean willPrompt, boolean cleanSession, int maxPacketSize, int defaultAwakeMessages, long sessionExpiryInterval) throws MqttsnCodecException {

        MqttsnConnect_V2_0 msg = new MqttsnConnect_V2_0();
        msg.setClientId(clientId);
        msg.setKeepAlive(keepAlive);
        msg.setCleanStart(cleanSession);
        msg.setWill(willPrompt);
        msg.setMaxPacketSize(maxPacketSize);
        msg.setDefaultAwakeMessages(defaultAwakeMessages);
        msg.setSessionExpiryInterval(sessionExpiryInterval);
        msg.validate();
        return msg;
    }

    @Override
    public IMqttsnMessage createConnack(int returnCode) throws MqttsnCodecException {

        MqttsnConnack_V2_0 msg = new MqttsnConnack_V2_0();
        msg.setReturnCode(returnCode);
        msg.setAssignedClientId(null);
        msg.setSessionExpiryInterval(0);
        msg.validate();
        return msg;
    }

    @Override
    public IMqttsnMessage createConnack(int returnCode, boolean sessionExists, String assignedClientId, long sessionExpiryInterval)
            throws MqttsnCodecException {

        MqttsnConnack_V2_0 msg = new MqttsnConnack_V2_0();
        msg.setReturnCode(returnCode);
        msg.setAssignedClientId(assignedClientId);
        msg.setSessionExpiryInterval(sessionExpiryInterval);
        msg.setSessionPresent(sessionExists);
        msg.validate();
        return msg;
    }

    @Override
    public IMqttsnMessage createRegack(int topicAliasId, int topicAlias, int returnCode) throws MqttsnCodecException {

        MqttsnRegack_V2_0 msg = new MqttsnRegack_V2_0();
        msg.setTopicIdType(topicAliasId);
        msg.setTopicId(topicAlias);
        msg.setReturnCode(returnCode);
        msg.validate();
        return msg;
    }

    @Override
    public IMqttsnMessage createPublish(int QoS, boolean DUP, boolean retain, MqttsnConstants.TOPIC_TYPE type, int topicId, byte[] payload) throws MqttsnCodecException {

        MqttsnPublish_V2_0 msg = new MqttsnPublish_V2_0();
        msg.setQoS(QoS);
        msg.setDupRedelivery(DUP);
        msg.setRetainedPublish(retain);
        msg.setData(payload);
        switch (type) {
            case NORMAL:
                msg.setNormalTopicAlias(topicId);
                break;
            case PREDEFINED:
                msg.setPredefinedTopicAlias(topicId);
                break;
            case SHORT:
                byte[] topicData = new byte[2];
                topicData[0] = (byte) ((topicId >> 8) & 0xFF);
                topicData[1] = (byte) (topicId & 0xFF);
                msg.setTopicName(new String(topicData));
                break;
            default:
                throw new MqttsnCodecException("publish method only supports predefined and normal topic id types");
        }
        msg.validate();
        return msg;
    }

    @Override
    public IMqttsnMessage createPublish(int QoS, boolean DUP, boolean retain, String topicPath, byte[] payload) throws MqttsnCodecException {

        MqttsnSpecificationValidator.validatePublishPath(topicPath);

        MqttsnPublish_V2_0 msg = new MqttsnPublish_V2_0();
        msg.setQoS(QoS);
        msg.setDupRedelivery(DUP);
        msg.setRetainedPublish(retain);
        msg.setData(payload);
        msg.setTopicName(topicPath);
        msg.validate();
        return msg;
    }

    @Override
    public IMqttsnMessage createPuback(int topicId, int returnCode) throws MqttsnCodecException {
        MqttsnPuback_V2_0 msg = new MqttsnPuback_V2_0();
        msg.setReturnCode(returnCode);
        msg.validate();
        return msg;
    }


    @Override
    public IMqttsnMessage createSubscribe(int QoS, MqttsnConstants.TOPIC_TYPE type, int topicId) throws MqttsnCodecException {

        MqttsnSpecificationValidator.validateTopicAlias(topicId);
        MqttsnSubscribe_V2_0 msg = new MqttsnSubscribe_V2_0();
        msg.setQoS(QoS);
        switch (type) {
            case NORMAL:
                msg.setNormalTopicAlias(topicId);
                break;
            case PREDEFINED:
                msg.setPredefinedTopicAlias(topicId);
                break;
//            case SHORT:
//                msg.setTopicName(topicId);
//                break;
            default:
                throw new MqttsnCodecException("subscribe method only supports predefined and normal topic id types");
        }
        msg.validate();
        return msg;
    }

    @Override
    public IMqttsnMessage createSubscribe(int QoS, String topicName) throws MqttsnCodecException {

        MqttsnSpecificationValidator.validateSubscribePath(topicName);
        MqttsnSubscribe_V2_0 msg = new MqttsnSubscribe_V2_0();
        msg.setQoS(QoS);
        msg.setTopicName(topicName);
        msg.setRetainHandling(MqttsnConstants.RETAINED_SEND);
        msg.setNoLocal(false);
        msg.setRetainAsPublished(false);
        msg.validate();
        return msg;
    }

    @Override
    public IMqttsnMessage createSuback(int grantedQoS, int topicId, int returnCode) throws MqttsnCodecException {

        MqttsnSuback_V2_0 msg = new MqttsnSuback_V2_0();
        msg.setQoS(grantedQoS);
        msg.setTopicIdType(MqttsnConstants.TOPIC_NORMAL);
        msg.setTopicId(topicId);
        msg.setReturnCode(returnCode);
        msg.validate();
        return msg;
    }

    @Override
    public IMqttsnMessage createUnsubscribe(MqttsnConstants.TOPIC_TYPE type, int topicId) throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateTopicAlias(topicId);
        MqttsnUnsubscribe_V2_0 msg = new MqttsnUnsubscribe_V2_0();
        switch (type) {
            case NORMAL:
                msg.setNormalTopicAlias(topicId);
                break;
            case PREDEFINED:
                msg.setPredefinedTopicAlias(topicId);
                break;
            default:
                throw new MqttsnCodecException("subscribe method only supports predefined and normal topic id types");
        }
        return msg;
    }

    @Override
    public IMqttsnMessage createUnsubscribe(String topicName) throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateSubscribePath(topicName);
        MqttsnUnsubscribe_V2_0 msg = new MqttsnUnsubscribe_V2_0();
        msg.setTopicName(topicName);
        return msg;
    }

    @Override
    public IMqttsnMessage createUnsuback(int reasonCode) throws MqttsnCodecException {
        MqttsnUnsuback_V2_0 msg = new MqttsnUnsuback_V2_0();
        msg.setReturnCode(reasonCode);
        return msg;
    }

    @Override
    public IMqttsnMessage createPingreq(String clientId) throws MqttsnCodecException {
        MqttsnPingreq_V2_0 msg = new MqttsnPingreq_V2_0();
        msg.setClientId(clientId);
        msg.setMaxMessages(0);
        msg.validate();
        return msg;
    }

    @Override
    public IMqttsnMessage createPingresp() throws MqttsnCodecException {
        MqttsnPingresp_V2_0 msg = new MqttsnPingresp_V2_0();
        msg.setMessagesRemaining(0);
        return msg;
    }

    @Override
    public IMqttsnMessage createDisconnect() throws MqttsnCodecException {
        MqttsnDisconnect_V2_0 msg = new MqttsnDisconnect_V2_0();
        msg.validate();
        return msg;
    }

    @Override
    public IMqttsnMessage createDisconnect(long sessionExpiry, boolean retainRegistrations) throws MqttsnCodecException {

        MqttsnDisconnect_V2_0 msg = new MqttsnDisconnect_V2_0();
        msg.setSessionExpiryInterval(sessionExpiry);
        msg.setRetainRegistrations(retainRegistrations);
        msg.setReasonString(null);
        msg.validate();
        return msg;
    }

    @Override
    public IMqttsnMessage createDisconnect(int returnCode, String reasonString) throws MqttsnCodecException {

        MqttsnDisconnect_V2_0 msg = new MqttsnDisconnect_V2_0();
        msg.setReturnCode(returnCode);
        msg.setReasonString(reasonString);
        msg.validate();
        return msg;
    }
}