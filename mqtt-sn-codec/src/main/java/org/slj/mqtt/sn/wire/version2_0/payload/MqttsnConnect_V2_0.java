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

package org.slj.mqtt.sn.wire.version2_0.payload;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnConnectPacket;
import org.slj.mqtt.sn.spi.IMqttsnIdentificationPacket;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.spi.IMqttsnProtocolVersionPacket;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

/**
 * CONNECT - wire format per OASIS mqtt-sn-v2.0 CSD01 (05 Feb 2026), section 3.1, Figure 6.
 */
public class MqttsnConnect_V2_0 extends AbstractMqttsnMessage
        implements IMqttsnMessageValidator, IMqttsnIdentificationPacket, IMqttsnProtocolVersionPacket, IMqttsnConnectPacket {

    protected short protocolVersion = MqttsnConstants.PROTOCOL_VERSION_2_0;

    //-- Connect Flags (byte 3)
    protected boolean cleanStart;
    protected boolean will;
    protected boolean auth;
    protected boolean sessionExpirySet;
    protected boolean defaultAwakeMessagesSet;
    protected boolean allowNetworkAddressChanges;
    protected boolean allowServerSuggestedValues;

    //-- Will Flags (only present when will == true)
    protected boolean willRetain;
    protected int willQoS;
    protected int willTopicType;

    protected int keepAlive;
    protected long sessionExpiryInterval;
    protected int maxPacketSize;
    protected int defaultAwakeMessages;

    //-- Will fields (only present when will == true)
    protected int willTopicAlias;
    protected String willTopicName;
    protected byte[] willPayload;

    //-- Authentication fields (only present when auth == true)
    protected String authMethod;
    protected byte[] authData;

    protected String clientId;

    @Override
    public int getMessageType() {
        return MqttsnConstants.CONNECT_V2_0;
    }

    @Override
    public boolean needsId() {
        return true;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public boolean isAuth() {
        return auth;
    }

    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    public boolean isWill() {
        return will;
    }

    public void setWill(boolean will) {
        this.will = will;
    }

    public boolean isCleanStart() {
        return cleanStart;
    }

    public void setCleanStart(boolean cleanStart) {
        this.cleanStart = cleanStart;
    }

    public boolean isAllowNetworkAddressChanges() {
        return allowNetworkAddressChanges;
    }

    public void setAllowNetworkAddressChanges(boolean allowNetworkAddressChanges) {
        this.allowNetworkAddressChanges = allowNetworkAddressChanges;
    }

    public boolean isAllowServerSuggestedValues() {
        return allowServerSuggestedValues;
    }

    public void setAllowServerSuggestedValues(boolean allowServerSuggestedValues) {
        this.allowServerSuggestedValues = allowServerSuggestedValues;
    }

    public boolean isWillRetain() {
        return willRetain;
    }

    public void setWillRetain(boolean willRetain) {
        this.willRetain = willRetain;
    }

    public int getWillQoS() {
        return willQoS;
    }

    public void setWillQoS(int willQoS) {
        this.willQoS = willQoS;
    }

    public int getWillTopicType() {
        return willTopicType;
    }

    public void setWillTopicType(int willTopicType) {
        this.willTopicType = willTopicType;
    }

    public int getWillTopicAlias() {
        return willTopicAlias;
    }

    public void setWillTopicAlias(int willTopicAlias) {
        this.willTopicAlias = willTopicAlias;
    }

    public String getWillTopicName() {
        return willTopicName;
    }

    public void setWillTopicName(String willTopicName) {
        this.willTopicName = willTopicName;
    }

    public byte[] getWillPayload() {
        return willPayload;
    }

    public void setWillPayload(byte[] willPayload) {
        this.willPayload = willPayload;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public byte[] getAuthData() {
        return authData;
    }

    public void setAuthData(byte[] authData) {
        this.authData = authData;
    }

    public int getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    public void setSessionExpiryInterval(long sessionExpiryInterval) {
        this.sessionExpiryInterval = sessionExpiryInterval;
        this.sessionExpirySet = sessionExpiryInterval > 0;
    }

    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public void setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getDefaultAwakeMessages() {
        return defaultAwakeMessages;
    }

    public void setDefaultAwakeMessages(int defaultAwakeMessages) {
        this.defaultAwakeMessages = defaultAwakeMessages;
        this.defaultAwakeMessagesSet = defaultAwakeMessages > 0;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        int idx = 2;
        readFlags(readHeaderByteWithOffset(data, idx++));

        if (will) {
            readWillFlags(readHeaderByteWithOffset(data, idx++));
        }

        id = readUInt16Adjusted(data, idx);
        idx += 2;

        protocolVersion = readUInt8Adjusted(data, idx++);

        keepAlive = readUInt16Adjusted(data, idx);
        idx += 2;

        maxPacketSize = readUInt16Adjusted(data, idx);
        idx += 2;

        if (defaultAwakeMessagesSet) {
            defaultAwakeMessages = readUInt8Adjusted(data, idx++);
        }

        if (sessionExpirySet) {
            sessionExpiryInterval = readUInt32Adjusted(data, idx);
            idx += 4;
        }

        if (will) {
            int willTopicAliasOrNameLength = readUInt16Adjusted(data, idx);
            idx += 2;
            if (willTopicType == MqttsnConstants.TOPIC_FULL) {
                willTopicName = new String(readBytesAdjusted(data, idx, willTopicAliasOrNameLength), MqttsnConstants.CHARSET);
                idx += willTopicAliasOrNameLength;
            } else {
                willTopicAlias = willTopicAliasOrNameLength;
            }

            int willPayloadLength = readUInt16Adjusted(data, idx);
            idx += 2;
            willPayload = readBytesAdjusted(data, idx, willPayloadLength);
            idx += willPayloadLength;
        }

        if (auth) {
            int authMethodLength = readUInt8Adjusted(data, idx++);
            authMethod = new String(readBytesAdjusted(data, idx, authMethodLength), MqttsnConstants.CHARSET);
            idx += authMethodLength;

            int authDataLength = readUInt16Adjusted(data, idx);
            idx += 2;
            authData = readBytesAdjusted(data, idx, authDataLength);
            idx += authDataLength;
        }

        //-- clientId is optional, its presence is inferred from any remaining bytes
        int consumedLength = MqttsnWireUtils.isLargeMessage(data) ? idx + 2 : idx;
        if (data.length > consumedLength) {
            clientId = readRemainingUTF8EncodedAdjustedNoLength(data, idx);
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        byte[] willTopicNameBytes = (will && willTopicType == MqttsnConstants.TOPIC_FULL && willTopicName != null) ?
                willTopicName.getBytes(MqttsnConstants.CHARSET) : null;
        byte[] authMethodBytes = (auth && authMethod != null) ? authMethod.getBytes(MqttsnConstants.CHARSET) : null;

        //-- length(1) + type(1) + flags(1) + packetId(2) + protocolVersion(1) + keepAlive(2) + maxPacketSize(2)
        int length = 10;

        if (will) length += 1;
        if (defaultAwakeMessagesSet) length += 1;
        if (sessionExpirySet) length += 4;

        if (will) {
            length += 2; //-- will topic alias or will topic name length
            if (willTopicType == MqttsnConstants.TOPIC_FULL) {
                length += willTopicNameBytes == null ? 0 : willTopicNameBytes.length;
            }
            length += 2; //-- will payload length
            length += willPayload == null ? 0 : willPayload.length;
        }

        if (auth) {
            length += 1; //-- auth method length
            length += authMethodBytes == null ? 0 : authMethodBytes.length;
            length += 2; //-- auth data length
            length += authData == null ? 0 : authData.length;
        }

        length += clientId == null ? 0 : clientId.getBytes(MqttsnConstants.CHARSET).length;

        byte[] msg;
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

        if (will) {
            msg[idx++] = writeWillFlags();
        }

        msg[idx++] = (byte) ((id >> 8) & 0xFF);
        msg[idx++] = (byte) (id & 0xFF);

        msg[idx++] = (byte) protocolVersion;

        msg[idx++] = (byte) ((keepAlive >> 8) & 0xFF);
        msg[idx++] = (byte) (keepAlive & 0xFF);

        msg[idx++] = (byte) ((maxPacketSize >> 8) & 0xFF);
        msg[idx++] = (byte) (maxPacketSize & 0xFF);

        if (defaultAwakeMessagesSet) {
            msg[idx++] = (byte) defaultAwakeMessages;
        }

        if (sessionExpirySet) {
            writeUInt32(msg, idx, sessionExpiryInterval);
            idx += 4;
        }

        if (will) {
            int willTopicAliasOrNameLength = willTopicType == MqttsnConstants.TOPIC_FULL ?
                    (willTopicNameBytes == null ? 0 : willTopicNameBytes.length) : willTopicAlias;

            msg[idx++] = (byte) ((willTopicAliasOrNameLength >> 8) & 0xFF);
            msg[idx++] = (byte) (willTopicAliasOrNameLength & 0xFF);

            if (willTopicType == MqttsnConstants.TOPIC_FULL && willTopicNameBytes != null) {
                System.arraycopy(willTopicNameBytes, 0, msg, idx, willTopicNameBytes.length);
                idx += willTopicNameBytes.length;
            }

            int willPayloadLength = willPayload == null ? 0 : willPayload.length;
            msg[idx++] = (byte) ((willPayloadLength >> 8) & 0xFF);
            msg[idx++] = (byte) (willPayloadLength & 0xFF);
            if (willPayload != null) {
                System.arraycopy(willPayload, 0, msg, idx, willPayload.length);
                idx += willPayload.length;
            }
        }

        if (auth) {
            int authMethodLength = authMethodBytes == null ? 0 : authMethodBytes.length;
            msg[idx++] = (byte) authMethodLength;
            if (authMethodBytes != null) {
                System.arraycopy(authMethodBytes, 0, msg, idx, authMethodBytes.length);
                idx += authMethodBytes.length;
            }

            int authDataLength = authData == null ? 0 : authData.length;
            msg[idx++] = (byte) ((authDataLength >> 8) & 0xFF);
            msg[idx++] = (byte) (authDataLength & 0xFF);
            if (authData != null) {
                System.arraycopy(authData, 0, msg, idx, authData.length);
                idx += authData.length;
            }
        }

        if (clientId != null) {
            writeUTF8EncodedStringDataNoLength(msg, idx, clientId);
        }

        return msg;
    }

    protected void readFlags(byte v) {
        /**
         Reserved  SrvSugg  NetAddr  DAM   SessExp  Auth  Will  CleanStart
         (bit 7)   (6)      (5)      (4)   (3)      (2)   (1)   (0)
         **/

        if ((v & 0x80) != 0) {
            throw new MqttsnCodecException("reserved flags must be set to 0");
        }

        allowServerSuggestedValues = (v & 0x40) != 0;
        allowNetworkAddressChanges = (v & 0x20) != 0;
        defaultAwakeMessagesSet = (v & 0x10) != 0;
        sessionExpirySet = (v & 0x08) != 0;
        auth = (v & 0x04) != 0;
        will = (v & 0x02) != 0;
        cleanStart = (v & 0x01) != 0;
    }

    protected byte writeFlags() {
        byte v = 0x00;
        if (allowServerSuggestedValues) v |= 0x40;
        if (allowNetworkAddressChanges) v |= 0x20;
        if (defaultAwakeMessagesSet) v |= 0x10;
        if (sessionExpirySet) v |= 0x08;
        if (auth) v |= 0x04;
        if (will) v |= 0x02;
        if (cleanStart) v |= 0x01;
        return v;
    }

    protected void readWillFlags(byte v) {
        /**
         Reserved     WillRetain  WillQoS  WillTopicType
         (7,6,5)      (4)         (3,2)    (1,0)
         **/

        if ((v & 0xE0) != 0) {
            throw new MqttsnCodecException("reserved will flags must be set to 0");
        }

        willRetain = (v & 0x10) != 0;
        willQoS = (v & 0x0C) >> 2;
        willTopicType = (v & 0x03);
    }

    protected byte writeWillFlags() {
        byte v = 0x00;
        if (willRetain) v |= 0x10;
        v |= (willQoS << 2) & 0x0C;
        v |= (willTopicType & 0x03);
        return v;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateProtocolId(protocolVersion);
        MqttsnSpecificationValidator.validateKeepAlive(keepAlive);
        MqttsnSpecificationValidator.validateMaxPacketSize(maxPacketSize);

        if (defaultAwakeMessagesSet) {
            MqttsnSpecificationValidator.validateDefaultAwakeMessages(defaultAwakeMessages);
        }
        if (sessionExpirySet) {
            MqttsnSpecificationValidator.validateSessionExpiry(sessionExpiryInterval);
        }
        if (clientId != null) {
            MqttsnSpecificationValidator.validateClientId(clientId, MqttsnConstants.MAX_CLIENT_ID_LENGTH_v2);
        }
        if (will) {
            if (willQoS != MqttsnConstants.QoS0 && willQoS != MqttsnConstants.QoS1 && willQoS != MqttsnConstants.QoS2) {
                throw new MqttsnCodecException("invalid will QoS - " + willQoS);
            }
            if (willTopicType == MqttsnConstants.TOPIC_SHORT) {
                throw new MqttsnCodecException("will topic type SHORT is Reserved in MQTT-SN 2.0 (no Short Topic Name support)");
            }
            MqttsnSpecificationValidator.validateTopicIdType(willTopicType);
        }
    }

    @Override
    public String toString() {
        return "MqttsnConnect_V2_0{" +
                "protocolVersion=" + protocolVersion +
                ", cleanStart=" + cleanStart +
                ", will=" + will +
                ", auth=" + auth +
                ", sessionExpirySet=" + sessionExpirySet +
                ", allowNetworkAddressChanges=" + allowNetworkAddressChanges +
                ", allowServerSuggestedValues=" + allowServerSuggestedValues +
                ", keepAlive=" + keepAlive +
                ", sessionExpiryInterval=" + sessionExpiryInterval +
                ", maxPacketSize=" + maxPacketSize +
                ", defaultAwakeMessages=" + defaultAwakeMessages +
                ", clientId='" + (clientId == null ? "" : clientId) + '\'' +
                '}';
    }
}
