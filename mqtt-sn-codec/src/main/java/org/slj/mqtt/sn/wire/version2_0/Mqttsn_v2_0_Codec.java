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
import org.slj.mqtt.sn.PublishData;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.codec.MqttsnUnsupportedVersionException;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnMessageFactory;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.version1_2.Mqttsn_v1_2_Codec;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnConnect;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnDisconnect;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnPublish;
import org.slj.mqtt.sn.wire.version2_0.payload.*;

public class Mqttsn_v2_0_Codec extends Mqttsn_v1_2_Codec {

    @Override
    public boolean isDisconnect(IMqttsnMessage message) {
        return message instanceof MqttsnDisconnect_V2_0;
    }

    @Override
    public boolean isRetainedPublish(IMqttsnMessage message) {
        if(isPublish(message)){
            return ((MqttsnPublish_V2_0) message).isRetainedPublish();
        }
        throw new MqttsnCodecException("unable to read retained from non publish message");
    }

    @Override
    public String getClientId(IMqttsnMessage message) {
        if(message instanceof MqttsnConnect_V2_0){
            return ((MqttsnConnect_V2_0) message).getClientId();
        }
        throw new MqttsnCodecException("unable to read clientId from non CONNECT message");
    }

    @Override
    public boolean isCleanSession(IMqttsnMessage message) {
        if(message instanceof MqttsnConnect_V2_0){
            return ((MqttsnConnect_V2_0) message).isCleanStart();
        }
        throw new MqttsnCodecException("unable to read cleanStart from non CONNECT message");
    }

    @Override
    public long getKeepAlive(IMqttsnMessage message) {
        if(message instanceof MqttsnConnect_V2_0){
            return ((MqttsnConnect_V2_0) message).getKeepAlive();
        }
        throw new MqttsnCodecException("unable to read keepAlive from non CONNECT message");
    }

    @Override
    public long getDuration(IMqttsnMessage message) {
        if(message instanceof MqttsnDisconnect_V2_0){
            return ((MqttsnDisconnect_V2_0) message).getSessionExpiryInterval();
        }
        throw new MqttsnCodecException("unable to read duration from non DISCONNECT message");
    }

    @Override
    public boolean isPublish(IMqttsnMessage message) {
        return message instanceof MqttsnPublish_V2_0;
    }

    @Override
    public boolean isPuback(IMqttsnMessage message) { return message instanceof MqttsnPuback_V2_0; }

    @Override
    public PublishData getData(IMqttsnMessage message) {
        MqttsnPublish_V2_0 publish = (MqttsnPublish_V2_0) message ;
        return new PublishData(Math.max(publish.getQoS(), 0), publish.isRetainedPublish(), publish.getData());
    }

    @Override
    protected int getQoS(IMqttsnMessage message) {
        if(message instanceof MqttsnPublish_V2_0){
            MqttsnPublish_V2_0 publish = (MqttsnPublish_V2_0) message ;
            return publish.getQoS();
        } else if(message instanceof MqttsnSubscribe_V2_0){
            MqttsnSubscribe_V2_0 publish = (MqttsnSubscribe_V2_0) message ;
            return publish.getQoS();
        }
        throw new MqttsnCodecException("unable to read QoS from non SUBSCRIBE | PUBLISH message");
    }

    @Override
    public boolean isConnect(IMqttsnMessage message) {
        return message instanceof MqttsnConnect_V2_0;
    }

    @Override
    protected AbstractMqttsnMessage createInstance(byte[] data) throws MqttsnCodecException, MqttsnUnsupportedVersionException {

        MqttsnSpecificationValidator.validatePacketLength(data);

        AbstractMqttsnMessage msg;
        int msgType = MqttsnWireUtils.readMessageType(data);

        switch (msgType) {
            case MqttsnConstants.AUTH:
                validateLengthGreaterThanOrEquals(data, 5);
                msg = new MqttsnAuth();
                break;
            case MqttsnConstants.CONNECT:
                //-- check version
                int version = MqttsnConstants.PROTOCOL_VERSION_UNKNOWN;
                if(data[0] == 0x01){
                    version = data[5];
                } else {
                    version = data[3];
                }

                if(version != MqttsnConstants.PROTOCOL_VERSION_2_0){
                    throw new MqttsnUnsupportedVersionException("codec cannot parse ["+version+"] non 2.0 message");
                } else {
                    validateLengthGreaterThanOrEquals(data, 12);
                    msg = new MqttsnConnect_V2_0();
                }
                break;
            case MqttsnConstants.CONNACK:
                validateLengthGreaterThanOrEquals(data, 7);
                msg = new MqttsnConnack_V2_0();
                break;
            case MqttsnConstants.REGACK:
                validateLengthEquals(data, 8);
                msg = new MqttsnRegack_V2_0();
                break;
            case MqttsnConstants.PUBLISH:
            case MqttsnConstants.PUBLISH_M1:
                validateLengthGreaterThanOrEquals(data, 6);
                msg = new MqttsnPublish_V2_0();
                msg.decode(data);
                break;
            case MqttsnConstants.PUBACK:
                validateLengthEquals(data, 5);
                msg = new MqttsnPuback_V2_0();
                break;
            case MqttsnConstants.PINGREQ:
                validateLengthGreaterThanOrEquals(data, 2);
                msg = new MqttsnPingreq_V2_0();
                break;
            case MqttsnConstants.PINGRESP:
                validateLengthGreaterThanOrEquals(data, 2);
                msg = new MqttsnPingresp_V2_0();
                break;
            case MqttsnConstants.DISCONNECT:
                validateLengthGreaterThanOrEquals(data, 2);
                msg = new MqttsnDisconnect_V2_0();
                break;
            case MqttsnConstants.SUBSCRIBE:
                validateLengthGreaterThanOrEquals(data, 7);
                msg = new MqttsnSubscribe_V2_0();
                break;
            case MqttsnConstants.SUBACK:
                validateLengthEquals(data, 8);
                msg = new MqttsnSuback_V2_0();
                break;
            case MqttsnConstants.UNSUBSCRIBE:
                validateLengthGreaterThanOrEquals(data, 7);
                msg = new MqttsnUnsubscribe_V2_0();
                break;
            case MqttsnConstants.UNSUBACK:
                validateLengthEquals(data, 5);
                msg = new MqttsnUnsuback_V2_0();
                break;
            default:
                msg = super.createInstance(data);
                break;
        }
        msg.decode(data);
        return msg;
    }

    @Override
    public IMqttsnMessageFactory createMessageFactory() {
        if (messageFactory == null) {
            synchronized (this) {
                if (messageFactory == null) messageFactory = Mqttsn_v2_0_MessageFactory.getInstance();
            }
        }
        return messageFactory;
    }

    @Override
    public boolean supportsVersion(int protocolVersion) throws MqttsnCodecException {
        return protocolVersion == MqttsnConstants.PROTOCOL_VERSION_2_0;
    }

    @Override
    public int getProtocolVersion() throws MqttsnCodecException {
        return MqttsnConstants.PROTOCOL_VERSION_2_0;
    }
}