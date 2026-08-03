/*
 * Copyright (c) 2021-2026 Simon Johnson <simon622 AT gmail DOT com>, Ian Craggs
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
import org.slj.mqtt.sn.descriptor.ProtocolDescriptor;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnMessageFactory;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.version1_2.Mqttsn_v1_2_Codec;
import org.slj.mqtt.sn.wire.version1_2.Mqttsn_v1_2_ProtocolDescriptor;
import org.slj.mqtt.sn.wire.version2_0.payload.*;

public class Mqttsn_v2_0_Codec extends Mqttsn_v1_2_Codec {

    public Mqttsn_v2_0_Codec(boolean strict) {
        super(strict);
    }

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
            case MqttsnConstants.AUTH_V2_0:
                validateLengthGreaterThanOrEquals(data, 5);
                msg = new MqttsnAuth();
                break;
            case MqttsnConstants.CONNECT_V2_0:
                //-- check version; NB: unlike v1.2, the Protocol Version field's position is
                //-- not fixed - Connect Flags is followed by an optional 1-byte Will Flags
                //-- field (present iff Connect Flags bit 1 / Will is set) and then the 2-byte
                //-- Packet Identifier, before Protocol Version - see spec Figure 6.
                boolean isLargeConnect = data[0] == 0x01;
                int connectFlagsIdx = isLargeConnect ? 4 : 2;
                boolean connectHasWill = (data[connectFlagsIdx] & 0x02) != 0;
                int version = data[connectFlagsIdx + 3 + (connectHasWill ? 1 : 0)];

                if(version != MqttsnConstants.PROTOCOL_VERSION_2_0){
                    throw new MqttsnUnsupportedVersionException("codec version mismatch ["+version+"] found non 2.0 message");
                } else {
                    validateLengthGreaterThanOrEquals(data, 10);
                    msg = new MqttsnConnect_V2_0();
                }
                break;
            case MqttsnConstants.CONNACK_V2_0:
                validateLengthGreaterThanOrEquals(data, 6);
                msg = new MqttsnConnack_V2_0();
                break;
            case MqttsnConstants.REGACK_V2_0:
                validateLengthEquals(data, 8);
                msg = new MqttsnRegack_V2_0();
                break;
            case MqttsnConstants.PUBLISH_V2_0:
                validateLengthGreaterThanOrEquals(data, 6);
                msg = new MqttsnPublish_V2_0();
                msg.decode(data);
                break;
            case MqttsnConstants.PUBACK_V2_0:
                validateLengthGreaterThanOrEquals(data, 4);
                msg = new MqttsnPuback_V2_0();
                break;
            case MqttsnConstants.PUBREC_V2_0:
                validateLengthGreaterThanOrEquals(data, 4);
                msg = new MqttsnPubrec_V2_0();
                break;
            case MqttsnConstants.PUBREL_V2_0:
                validateLengthGreaterThanOrEquals(data, 4);
                msg = new MqttsnPubrel_V2_0();
                break;
            case MqttsnConstants.PUBCOMP_V2_0:
                validateLengthGreaterThanOrEquals(data, 4);
                msg = new MqttsnPubcomp_V2_0();
                break;
            case MqttsnConstants.PUBWOS_V2_0:
                validateLengthGreaterThanOrEquals(data, 5);
                msg = new MqttsnPubwos_V2_0();
                break;
            case MqttsnConstants.WAKEUP_V2_0:
                validateLengthGreaterThanOrEquals(data, 2);
                msg = new MqttsnWakeup_V2_0();
                break;
            case MqttsnConstants.SLEEPREQ_V2_0:
                validateLengthEquals(data, 9);
                msg = new MqttsnSleepreq_V2_0();
                break;
            case MqttsnConstants.SLEEPRESP_V2_0:
                validateLengthGreaterThanOrEquals(data, 5);
                msg = new MqttsnSleepresp_V2_0();
                break;
            case MqttsnConstants.ADVERTISE_V2_0:
                validateLengthEquals(data, 5);
                msg = new MqttsnAdvertise_V2_0();
                break;
            case MqttsnConstants.SEARCHGW_V2_0:
                validateLengthGreaterThanOrEquals(data, 2);
                msg = new MqttsnSearchGw_V2_0();
                break;
            case MqttsnConstants.GWINFO_V2_0:
                validateLengthGreaterThanOrEquals(data, 3);
                msg = new MqttsnGwInfo_V2_0();
                break;
            case MqttsnConstants.PINGREQ_V2_0:
                validateLengthEquals(data, 4);
                msg = new MqttsnPingreq_V2_0();
                break;
            case MqttsnConstants.PINGRESP_V2_0:
                validateLengthGreaterThanOrEquals(data, 4);
                msg = new MqttsnPingresp_V2_0();
                break;
            case MqttsnConstants.DISCONNECT_V2_0:
                validateLengthGreaterThanOrEquals(data, 2);
                msg = new MqttsnDisconnect_V2_0();
                break;
            case MqttsnConstants.SUBSCRIBE_V2_0:
                validateLengthGreaterThanOrEquals(data, 6);
                msg = new MqttsnSubscribe_V2_0();
                break;
            case MqttsnConstants.SUBACK_V2_0:
                validateLengthGreaterThanOrEquals(data, 5);
                msg = new MqttsnSuback_V2_0();
                break;
            case MqttsnConstants.UNSUBSCRIBE_V2_0:
                validateLengthGreaterThanOrEquals(data, 5);
                msg = new MqttsnUnsubscribe_V2_0();
                break;
            case MqttsnConstants.UNSUBACK_V2_0:
                validateLengthGreaterThanOrEquals(data, 4);
                msg = new MqttsnUnsuback_V2_0();
                break;
            case MqttsnConstants.PROTECTION_ENCAPSULATION_V2_0:
                validateLengthGreaterThanOrEquals(data, 18);
                msg = new MqttsnProtection();
                break;
            default:
                //-- NB: Forwarder/Session Encapsulation (0xFD/0xFE) are not yet implemented for
                //-- v2.0 and fall through to the v1.2 dispatch table here, which uses a
                //-- different, colliding byte-value scheme - see mqtt-sn-v2.0-gap-analysis.md.
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
                if (messageFactory == null) messageFactory = Mqttsn_v2_0_MessageFactory.getInstance(strict);
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

    public ProtocolDescriptor getProtocolDescriptor(){
        return Mqttsn_v2_0_ProtocolDescriptor.INSTANCE;
    }
}