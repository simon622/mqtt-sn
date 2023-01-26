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
import org.slj.mqtt.sn.spi.IMqttsnConnectPacket;
import org.slj.mqtt.sn.spi.IMqttsnIdentificationPacket;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.spi.IMqttsnProtocolVersionPacket;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnConnect;

public class MqttsnConnect_V2_0 extends AbstractMqttsnMessage
        implements IMqttsnMessageValidator, IMqttsnIdentificationPacket, IMqttsnProtocolVersionPacket, IMqttsnConnectPacket {

    protected short protocolVersion = MqttsnConstants.PROTOCOL_VERSION_2_0;
    protected boolean auth;
    protected boolean will;
    protected boolean cleanStart;
    protected int keepAlive;
    protected long sessionExpiryInterval;
    protected int maxPacketSize;
    protected int defaultAwakeMessages;
    protected String clientId;

    @Override
    public int getMessageType() {
        return MqttsnConstants.CONNECT;
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
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        if (isLargeMessage(data)) {
            readFlags(data[4]);
        } else {
            readFlags(data[2]);
        }

        //all mandatory
        protocolVersion = readUInt8Adjusted(data, 3);
        keepAlive = readUInt16Adjusted(data, 4);
        sessionExpiryInterval = readUInt32Adjusted(data, 6);
        maxPacketSize = readUInt16Adjusted(data, 10);

        //clientId is optional
        if(data.length >= 13){
            clientId = readRemainingUTF8EncodedAdjustedNoLength(data, 12);
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        int length = 12 + (clientId == null ? 0 : clientId.length());
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
        msg[idx++] = (byte) protocolVersion;

        msg[idx++] = (byte) ((keepAlive >> 8) & 0xFF);
        msg[idx++] = (byte) (keepAlive & 0xFF);

        writeUInt32(msg, idx, sessionExpiryInterval);
        idx += 4;

        msg[idx++] = (byte) ((maxPacketSize >> 8) & 0xFF);
        msg[idx++] = (byte) (maxPacketSize & 0xFF);

        if (clientId != null) {
            writeUTF8EncodedStringDataNoLength(msg, idx, clientId);
        }

        return msg;
    }

    protected void readFlags(byte v) {
        /**
         Reserved     DefaultAwakeMessage      Auth  Will CleanSession
         (bit 7), (6,5,4,3)                     (2)    (1)          (0)
         **/

        //check all reserved flags
        if((v & 0x80) != 0){
            throw new MqttsnCodecException("reserved flags must be set to 0");
        }

        //default awake messages
        defaultAwakeMessages = (v & 0x78) >> 3;

        //auth
        auth = (v & 0x04) >> 2 != 0;

        //will
        will = ((v & 0x02) >> 1 != 0);

        //clean session
        cleanStart = ((v & 0x01) != 0);
    }

    protected byte writeFlags() {
        /**
         Reserved     DefaultAwakeMessage      Auth  Will CleanSession
         (bit 7), (6,5,4,3)                     (2)    (1)          (0)
         **/

        byte v = 0x00;

        v |= defaultAwakeMessages << 3;

        //dup redelivery
        if (auth) v |= 0x04;

        //will message
        if (will) v |= 0x02;

        //is clean session
        if (cleanStart) v |= 0x01;

        return v;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateDefaultAwakeMessages(defaultAwakeMessages);
        MqttsnSpecificationValidator.validateProtocolId(protocolVersion);
        MqttsnSpecificationValidator.validateKeepAlive(keepAlive);
        MqttsnSpecificationValidator.validateSessionExpiry(sessionExpiryInterval);
        MqttsnSpecificationValidator.validateMaxPacketSize(maxPacketSize);
        if(clientId != null) MqttsnSpecificationValidator.validateClientId(clientId);
    }

    @Override
    public String toString() {
        return "MqttsnConnect_V2_0{" +
                "protocolVersion=" + protocolVersion +
                ", auth=" + auth +
                ", will=" + will +
                ", cleanStart=" + cleanStart +
                ", keepAlive=" + keepAlive +
                ", sessionExpiryInterval=" + sessionExpiryInterval +
                ", maxPacketSize=" + maxPacketSize +
                ", defaultAwakeMessages=" + defaultAwakeMessages +
                ", clientId='" + (clientId == null ? "" : "") + '\'' +
                '}';
    }
}
