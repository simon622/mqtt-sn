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
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.PublishData;
import org.slj.mqtt.sn.codec.AbstractMqttsnCodec;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.codec.MqttsnUnsupportedVersionException;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnMessageFactory;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.version1_2.payload.*;

public class Mqttsn_v1_2_Codec extends AbstractMqttsnCodec {

    protected volatile IMqttsnMessageFactory messageFactory;

    @Override
    public PublishData getData(IMqttsnMessage message) {
        MqttsnPublish publish = (MqttsnPublish) message ;
        return new PublishData(Math.max(publish.getQoS(), 0), publish.isRetainedPublish(), publish.getData());
    }

    @Override
    protected int getQoS(IMqttsnMessage message) {
        if(message instanceof AbstractMqttsnMessageWithFlagsField){
            AbstractMqttsnMessageWithFlagsField msg = (AbstractMqttsnMessageWithFlagsField) message ;
            return msg.getQoS();
        }
        throw new MqttsnCodecException("unable to read QoS from non flagged message");
    }

    @Override
    public String getClientId(IMqttsnMessage message) {
        if(message instanceof MqttsnConnect){
            return ((MqttsnConnect) message).getClientId();
        }
        throw new MqttsnCodecException("unable to read clientId from non CONNECT message");
    }

    @Override
    public boolean isCleanSession(IMqttsnMessage message) {
        if(message instanceof MqttsnConnect){
            return ((MqttsnConnect) message).isCleanSession();
        }
        throw new MqttsnCodecException("unable to read cleanSession from non CONNECT message");
    }

    @Override
    public long getKeepAlive(IMqttsnMessage message) {
        if(message instanceof MqttsnConnect){
            return ((MqttsnConnect) message).getDuration();
        }
        throw new MqttsnCodecException("unable to read keepAlive from non CONNECT message");
    }

    @Override
    public long getDuration(IMqttsnMessage message) {
        if(message instanceof MqttsnDisconnect){
            return ((MqttsnDisconnect) message).getDuration();
        }
        throw new MqttsnCodecException("unable to read duration from non DISCONNECT message");
    }

    @Override
    public boolean isRetainedPublish(IMqttsnMessage message) {
        if(isPublish(message)){
            return ((MqttsnPublish) message).isRetainedPublish();
        }
        throw new MqttsnCodecException("unable to read retained from non publish message");
    }

    @Override
    public boolean isDisconnect(IMqttsnMessage message) {
        return message instanceof MqttsnDisconnect;
    }

    @Override
    public boolean isConnect(IMqttsnMessage message) {
        return message instanceof MqttsnConnect;
    }

    @Override
    public boolean isPublish(IMqttsnMessage message) {
        return message instanceof MqttsnPublish;
    }

    @Override
    public boolean isPuback(IMqttsnMessage message) {
        return message instanceof MqttsnPuback;
    }

    @Override
    public boolean isPubRel(IMqttsnMessage message) {
        return message instanceof MqttsnPubrel;
    }

    @Override
    public boolean isPubRec(IMqttsnMessage message) {
        return message instanceof MqttsnPubrec;
    }

    @Override
    public boolean isActiveMessage(IMqttsnMessage message) {
        return ! (message instanceof MqttsnPingreq ||
                message instanceof MqttsnDisconnect || message instanceof MqttsnPingresp);
    }

    @Override
    public int readMessageSize(byte[] data) throws MqttsnCodecException {
        MqttsnSpecificationValidator.validatePacketLength(data);
        return AbstractMqttsnMessage.readMessageLength(data);
    }



    protected AbstractMqttsnMessage createInstance(byte[] data)
            throws MqttsnCodecException, MqttsnUnsupportedVersionException {

        MqttsnSpecificationValidator.validatePacketLength(data);

        AbstractMqttsnMessage msg = null;
        int msgType = MqttsnWireUtils.readMessageType(data);

        switch (msgType) {
            case MqttsnConstants.ADVERTISE:
                validateLengthGreaterThanOrEquals(data, 3);
                msg = new MqttsnAdvertise();
                break;
            case MqttsnConstants.SEARCHGW:
                validateLengthEquals(data, 3);
                msg = new MqttsnSearchGw();
                break;
            case MqttsnConstants.GWINFO:
                validateLengthGreaterThanOrEquals(data, 3);
                msg = new MqttsnGwInfo();
                break;
            case MqttsnConstants.CONNECT:

                //-- check version - version 1.2 should allow 0 in as it seems most clients send 0
                if(data[3] != MqttsnConstants.PROTOCOL_VERSION_1_2 && data[3] != 0){
                    throw new MqttsnUnsupportedVersionException("codec cannot parse ["+data[3]+"] non 2.0 message");
                } else {
                    validateLengthGreaterThanOrEquals(data, 6);
                    msg = new MqttsnConnect();
                }
                break;
            case MqttsnConstants.CONNACK:
                validateLengthEquals(data, 3);
                msg = new MqttsnConnack();
                break;
            case MqttsnConstants.REGISTER:
                validateLengthGreaterThanOrEquals(data, 7);
                msg = new MqttsnRegister();
                break;
            case MqttsnConstants.REGACK:
                validateLengthEquals(data, 7);
                msg = new MqttsnRegack();
                break;
            case MqttsnConstants.PUBLISH:
                validateLengthGreaterThanOrEquals(data, 7);
                msg = new MqttsnPublish();
                msg.decode(data);
                break;
            case MqttsnConstants.PUBACK:
                validateLengthEquals(data, 7);
                msg = new MqttsnPuback();
                break;
            case MqttsnConstants.PUBCOMP:
                validateLengthEquals(data, 4);
                msg = new MqttsnPubcomp();
                break;
            case MqttsnConstants.PUBREC:
                validateLengthEquals(data, 4);
                msg = new MqttsnPubrec();
                break;
            case MqttsnConstants.PUBREL:
                validateLengthEquals(data, 4);
                msg = new MqttsnPubrel();
                break;
            case MqttsnConstants.PINGREQ:
                validateLengthGreaterThanOrEquals(data, 2);
                msg = new MqttsnPingreq();
                break;
            case MqttsnConstants.PINGRESP:
                validateLengthEquals(data, 2);
                msg = new MqttsnPingresp();
                break;
            case MqttsnConstants.DISCONNECT:
                validateLengthGreaterThanOrEquals(data, 2);
                msg = new MqttsnDisconnect();
                break;
            case MqttsnConstants.SUBSCRIBE:
                validateLengthGreaterThanOrEquals(data, 6);
                msg = new MqttsnSubscribe();
                break;
            case MqttsnConstants.SUBACK:
                validateLengthEquals(data, 8);
                msg = new MqttsnSuback();
                break;
            case MqttsnConstants.UNSUBSCRIBE:
                validateLengthGreaterThanOrEquals(data, 6);
                msg = new MqttsnUnsubscribe();
                break;
            case MqttsnConstants.UNSUBACK:
                validateLengthEquals(data, 4);
                msg = new MqttsnUnsuback();
                break;
            case MqttsnConstants.WILLTOPICREQ:
                validateLengthEquals(data, 2);
                msg = new MqttsnWilltopicreq();
                break;
            case MqttsnConstants.WILLTOPIC:
                validateLengthGreaterThanOrEquals(data, 3);
                msg = new MqttsnWilltopic();
                break;
            case MqttsnConstants.WILLMSGREQ:
                validateLengthEquals(data, 2);
                msg = new MqttsnWillmsgreq();
                break;
            case MqttsnConstants.WILLMSG:
                validateLengthGreaterThanOrEquals(data, 2);
                msg = new MqttsnWillmsg();
                break;
            case MqttsnConstants.WILLTOPICUPD:
                validateLengthGreaterThanOrEquals(data, 3);
                msg = new MqttsnWilltopicudp();
                break;
            case MqttsnConstants.WILLTOPICRESP:
                validateLengthEquals(data, 3);
                msg = new MqttsnWilltopicresp();
                break;
            case MqttsnConstants.WILLMSGUPD:
                validateLengthGreaterThanOrEquals(data, 2);
                msg = new MqttsnWillmsgupd();
                break;
            case MqttsnConstants.WILLMSGRESP:
                validateLengthEquals(data, 3);
                msg = new MqttsnWillmsgresp();
                break;
            case MqttsnConstants.HELO:
                validateLengthGreaterThanOrEquals(data, 2);
                msg = new MqttsnHelo();
                break;
            case MqttsnConstants.ENCAPSMSG:
                validateLengthGreaterThanOrEquals(data, 5);
                msg = new MqttsnEncapsmsg();
                break;
            default:
                throw new MqttsnCodecException(String.format("unknown message type [%s]", msgType));
        }
        msg.decode(data);
        return msg;
    }



    @Override
    public IMqttsnMessageFactory createMessageFactory() {
        if (messageFactory == null) {
            synchronized (this) {
                if (messageFactory == null) messageFactory = Mqttsn_v1_2_MessageFactory.getInstance();
            }
        }
        return messageFactory;
    }

    @Override
    public void validate(IMqttsnMessage message) throws MqttsnCodecException {
        if(message instanceof IMqttsnMessageValidator){
            IMqttsnMessageValidator v = (IMqttsnMessageValidator) message;
            v.validate();
        }
    }

    @Override
    public boolean supportsVersion(int protocolVersion) throws MqttsnCodecException {
        return protocolVersion == MqttsnConstants.PROTOCOL_VERSION_1_2 || protocolVersion == 0;
    }

    @Override
    public int getProtocolVersion() throws MqttsnCodecException {
        return MqttsnConstants.PROTOCOL_VERSION_1_2;
    }
}