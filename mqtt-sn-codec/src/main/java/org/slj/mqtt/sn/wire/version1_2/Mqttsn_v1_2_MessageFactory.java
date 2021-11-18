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

package org.slj.mqtt.sn.wire.version1_2;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.codec.AbstractMqttsnMessageFactory;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnMessageFactory;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.version1_2.payload.*;

public class Mqttsn_v1_2_MessageFactory extends AbstractMqttsnMessageFactory implements IMqttsnMessageFactory {

    //singleton
    private static Mqttsn_v1_2_MessageFactory instance;

    private Mqttsn_v1_2_MessageFactory() {
    }

    public static IMqttsnMessageFactory getInstance() {
        if (instance == null) {
            synchronized (Mqttsn_v1_2_MessageFactory.class) {
                if (instance == null) instance = new Mqttsn_v1_2_MessageFactory();
            }
        }
        return instance;
    }

    @Override
    public IMqttsnMessage createAdvertise(int gatewayId, int duration) throws MqttsnCodecException {

        MqttsnWireUtils.validate8Bit(gatewayId);
        MqttsnWireUtils.validate16Bit(duration);

        MqttsnAdvertise msg = new MqttsnAdvertise();
        msg.setGatewayId(gatewayId);
        msg.setDuration(duration);
        return msg;
    }

    @Override
    public IMqttsnMessage createSearchGw(int radius) throws MqttsnCodecException {

        MqttsnWireUtils.validate8Bit(radius);

        MqttsnSearchGw msg = new MqttsnSearchGw();
        msg.setRadius(radius);
        return msg;
    }

    @Override
    public IMqttsnMessage createGwinfo(int gatewayId, String gatewayAddress) throws MqttsnCodecException {

        MqttsnWireUtils.validate8Bit(gatewayId);

        MqttsnGwInfo msg = new MqttsnGwInfo();
        msg.setGatewayId(gatewayId);
        msg.setGatewayAddress(gatewayAddress);
        return msg;
    }

    @Override
    public IMqttsnMessage createConnect(String clientId, int keepAlive, boolean willPrompt, boolean cleanSession) throws MqttsnCodecException {

        MqttsnWireUtils.validate16Bit(keepAlive);

        MqttsnConnect msg = new MqttsnConnect();
        msg.setClientId(clientId);
        msg.setDuration(keepAlive);
        msg.setProtocolId(0);
        msg.setCleanSession(cleanSession);
        msg.setWill(willPrompt);
        return msg;
    }

    @Override
    public IMqttsnMessage createConnack(int returnCode) throws MqttsnCodecException {

        MqttsnWireUtils.validate8Bit(returnCode);

        MqttsnConnack msg = new MqttsnConnack();
        msg.setReturnCode(returnCode);
        return msg;
    }

    @Override
    public IMqttsnMessage createWillTopicReq() throws MqttsnCodecException {
        MqttsnWilltopicreq msg = new MqttsnWilltopicreq();
        return msg;
    }

    @Override
    public IMqttsnMessage createWillTopic(int QoS, boolean retain, String topicPath) throws MqttsnCodecException {
        MqttsnWilltopic msg = new MqttsnWilltopic();
        msg.setQoS(QoS);
        msg.setRetainedPublish(retain);
        msg.setWillTopic(topicPath);
        return msg;
    }

    @Override
    public IMqttsnMessage createWillTopicResp(int returnCode) throws MqttsnCodecException {
        MqttsnWilltopicresp msg = new MqttsnWilltopicresp();
        msg.setReturnCode(returnCode);
        return msg;
    }

    @Override
    public IMqttsnMessage createWillTopicupd(int QoS, boolean retain, String topicPath) throws MqttsnCodecException {
        MqttsnWilltopicudp msg = new MqttsnWilltopicudp();
        msg.setQoS(QoS);
        msg.setRetainedPublish(retain);
        msg.setWillTopic(topicPath);
        return msg;
    }

    @Override
    public IMqttsnMessage createWillMsgupd(byte[] payload) throws MqttsnCodecException {
        MqttsnWillmsgupd msg = new MqttsnWillmsgupd();
        msg.setMsgData(payload);
        return msg;
    }

    @Override
    public IMqttsnMessage createWillMsgReq() throws MqttsnCodecException {
        MqttsnWillmsgreq msg = new MqttsnWillmsgreq();
        return msg;
    }

    @Override
    public IMqttsnMessage createWillMsg(byte[] payload) throws MqttsnCodecException {
        MqttsnWillmsg msg = new MqttsnWillmsg();
        msg.setMsgData(payload);
        return msg;
    }

    @Override
    public IMqttsnMessage createWillMsgResp(int returnCode) throws MqttsnCodecException {

        MqttsnWireUtils.validate8Bit(returnCode);

        MqttsnWillmsgresp msg = new MqttsnWillmsgresp();
        msg.setReturnCode(returnCode);
        return msg;
    }

    @Override
    public IMqttsnMessage createRegister(int topicAlias, String topicPath) throws MqttsnCodecException {

        MqttsnWireUtils.validate16Bit(topicAlias);

        MqttsnRegister msg = new MqttsnRegister();
        msg.setTopicId(topicAlias);
        msg.setTopicName(topicPath);
        return msg;
    }

    @Override
    public IMqttsnMessage createRegister(String topicPath) throws MqttsnCodecException {
        MqttsnRegister msg = new MqttsnRegister();
        msg.setTopicName(topicPath);
        return msg;
    }

    @Override
    public IMqttsnMessage createRegack(int topicAlias, int returnCode) throws MqttsnCodecException {

        MqttsnWireUtils.validate8Bit(returnCode);
        MqttsnWireUtils.validate16Bit(topicAlias);

        MqttsnRegack msg = new MqttsnRegack();
        msg.setTopicId(topicAlias);
        msg.setReturnCode(returnCode);
        return msg;
    }

    @Override
    public IMqttsnMessage createPublish(int QoS, boolean DUP, boolean retain, MqttsnConstants.TOPIC_TYPE type, int topicId, byte[] payload) throws MqttsnCodecException {

        MqttsnWireUtils.validate16Bit(topicId);

        MqttsnPublish msg = new MqttsnPublish();
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
        return msg;
    }

    @Override
    public IMqttsnMessage createPublish(int QoS, boolean DUP, boolean retain, String topicPath, byte[] payload) throws MqttsnCodecException {

        int length = topicPath.getBytes(MqttsnConstants.CHARSET).length;
        if (length > 2)
            throw new MqttsnCodecException(String.format("invalid short topic supplied [%s] > 2", length));
        MqttsnPublish msg = new MqttsnPublish();
        msg.setQoS(QoS);
        msg.setDupRedelivery(DUP);
        msg.setRetainedPublish(retain);
        msg.setData(payload);
        msg.setTopicName(topicPath);
        return msg;
    }

    @Override
    public IMqttsnMessage createPuback(int topicId, int returnCode) throws MqttsnCodecException {

        MqttsnWireUtils.validate8Bit(returnCode);
        MqttsnWireUtils.validate16Bit(topicId);

        MqttsnPuback msg = new MqttsnPuback();
        msg.setTopicId(topicId);
        msg.setReturnCode(returnCode);
        return msg;
    }

    @Override
    public IMqttsnMessage createPubrec() throws MqttsnCodecException {
        MqttsnPubrec msg = new MqttsnPubrec();
        return msg;
    }

    @Override
    public IMqttsnMessage createPubrel() throws MqttsnCodecException {
        MqttsnPubrel msg = new MqttsnPubrel();
        return msg;
    }

    @Override
    public IMqttsnMessage createPubcomp() throws MqttsnCodecException {
        MqttsnPubcomp msg = new MqttsnPubcomp();
        return msg;
    }

    @Override
    public IMqttsnMessage createSubscribe(int QoS, MqttsnConstants.TOPIC_TYPE type, int topicId) throws MqttsnCodecException {

        MqttsnWireUtils.validateQoS(QoS);
        MqttsnWireUtils.validate16Bit(topicId);

        MqttsnSubscribe msg = new MqttsnSubscribe();
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
        return msg;
    }

    @Override
    public IMqttsnMessage createSubscribe(int QoS, String topicName) throws MqttsnCodecException {

        MqttsnWireUtils.validateQoS(QoS);
        MqttsnSubscribe msg = new MqttsnSubscribe();
        msg.setQoS(QoS);
        msg.setTopicName(topicName);
        return msg;
    }

    @Override
    public IMqttsnMessage createSuback(int grantedQoS, int topicId, int returnCode) throws MqttsnCodecException {

        MqttsnWireUtils.validateQoS(grantedQoS);
        MqttsnWireUtils.validate16Bit(topicId);
        MqttsnWireUtils.validate8Bit(returnCode);

        MqttsnSuback msg = new MqttsnSuback();
        msg.setQoS(grantedQoS);
        msg.setTopicId(topicId);
        msg.setReturnCode(returnCode);
        return msg;
    }

    @Override
    public IMqttsnMessage createUnsubscribe(MqttsnConstants.TOPIC_TYPE type, int topicId) throws MqttsnCodecException {

        MqttsnWireUtils.validate16Bit(topicId);
        MqttsnUnsubscribe msg = new MqttsnUnsubscribe();
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
        MqttsnUnsubscribe msg = new MqttsnUnsubscribe();
        msg.setTopicName(topicName);
        return msg;
    }

    @Override
    public IMqttsnMessage createUnsuback() throws MqttsnCodecException {
        MqttsnUnsuback msg = new MqttsnUnsuback();
        return msg;
    }

    @Override
    public IMqttsnMessage createPingreq(String clientId) throws MqttsnCodecException {
        MqttsnPingreq msg = new MqttsnPingreq();
        msg.setClientId(clientId);
        return msg;
    }

    @Override
    public IMqttsnMessage createPingresp() throws MqttsnCodecException {
        MqttsnPingresp msg = new MqttsnPingresp();
        return msg;
    }

    @Override
    public IMqttsnMessage createDisconnect() throws MqttsnCodecException {
        MqttsnDisconnect msg = new MqttsnDisconnect();
        return msg;
    }

    @Override
    public IMqttsnMessage createDisconnect(int duration) throws MqttsnCodecException {

        MqttsnWireUtils.validate16Bit(duration);
        MqttsnDisconnect msg = new MqttsnDisconnect();
        msg.setDuration(duration);
        return msg;
    }

    @Override
    public IMqttsnMessage createEncapsulatedMessage(String wirelessNodeId, int radius, byte[] messageData) throws MqttsnCodecException {

        MqttsnWireUtils.validate8Bit(radius);
        MqttsnEncapsmsg msg = new MqttsnEncapsmsg();
        msg.setEncapsulatedMsg(messageData);
        msg.setRadius(radius);
        msg.setWirelessNodeId(wirelessNodeId);
        return msg;
    }
}