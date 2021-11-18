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

package org.slj.mqtt.sn.codec;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnMessageFactory;

public abstract class AbstractMqttsnMessageFactory implements IMqttsnMessageFactory  {

    @Override
    public IMqttsnMessage createAdvertise(int gatewayId, int duration) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createSearchGw(int radius) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createGwinfo(int gatewayId, String gatewayAddress) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createConnect(String clientId, int keepAlive, boolean willPrompt, boolean cleanSession) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createConnack(int returnCode) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createWillTopicReq() throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createWillTopic(int QoS, boolean retain, String topicPath) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createWillTopicResp(int returnCode) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createWillTopicupd(int QoS, boolean retain, String topicPath) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createWillMsgupd(byte[] payload) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createWillMsgReq() throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createWillMsg(byte[] payload) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createWillMsgResp(int returnCode) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createRegister(String topicPath) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createRegister(int topicAlias, String topicPath) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createRegack(int topicAlias, int returnCode) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createPublish(int QoS, boolean DUP, boolean retain, MqttsnConstants.TOPIC_TYPE type, int topicId, byte[] payload) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createPublish(int QoS, boolean DUP, boolean retain, String topicPath, byte[] payload) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createPuback(int topicId, int returnCode) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createPubrec() throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createPubrel() throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createPubcomp() throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createSubscribe(int QoS, MqttsnConstants.TOPIC_TYPE type, int topicId) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createSubscribe(int QoS, String topicName) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createSuback(int grantedQoS, int topicId, int returnCode) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createUnsubscribe(MqttsnConstants.TOPIC_TYPE type, int topicId) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createUnsubscribe(String topicName) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createUnsuback() throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createPingreq(String clientId) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createPingresp() throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createDisconnect() throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createDisconnect(int duration) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }

    @Override
    public IMqttsnMessage createEncapsulatedMessage(String wirelessNodeId, int radius, byte[] messageData) throws MqttsnCodecException {
        throw new UnsupportedOperationException("message not supported by codec");
    }
}
