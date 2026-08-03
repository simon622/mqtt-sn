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
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

/**
 * CONNACK - wire format per OASIS mqtt-sn-v2.0 CSD01 (05 Feb 2026), section 3.2, Figure 7.
 */
public class MqttsnConnack_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    //-- Connack Flags (byte 3)
    protected boolean sessionPresent;
    protected boolean sessionExpirySet;
    protected boolean serverKeepAliveSet;
    protected boolean auth;

    protected long sessionExpiryInterval;
    protected int serverKeepAlive;

    //-- Authentication fields (only present when auth == true)
    protected String authMethod;
    protected byte[] authData;

    protected String assignedClientId;

    @Override
    public int getMessageType() {
        return MqttsnConstants.CONNACK_V2_0;
    }

    @Override
    public boolean needsId() {
        return true;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        int idx = 2;
        readFlags(readHeaderByteWithOffset(data, idx++));

        id = readUInt16Adjusted(data, idx);
        idx += 2;

        returnCode = readUInt8Adjusted(data, idx++);

        if (sessionExpirySet) {
            sessionExpiryInterval = readUInt32Adjusted(data, idx);
            idx += 4;
        }

        if (serverKeepAliveSet) {
            serverKeepAlive = readUInt16Adjusted(data, idx);
            idx += 2;
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

        //-- assignedClientId is optional, its presence is inferred from any remaining bytes
        int consumedLength = MqttsnWireUtils.isLargeMessage(data) ? idx + 2 : idx;
        if (data.length > consumedLength) {
            assignedClientId = readRemainingUTF8EncodedAdjustedNoLength(data, idx);
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        byte[] authMethodBytes = (auth && authMethod != null) ? authMethod.getBytes(MqttsnConstants.CHARSET) : null;

        //-- length(1) + type(1) + flags(1) + packetId(2) + reasonCode(1)
        int length = 6;

        if (sessionExpirySet) length += 4;
        if (serverKeepAliveSet) length += 2;

        if (auth) {
            length += 1; //-- auth method length
            length += authMethodBytes == null ? 0 : authMethodBytes.length;
            length += 2; //-- auth data length
            length += authData == null ? 0 : authData.length;
        }

        length += assignedClientId == null ? 0 : assignedClientId.getBytes(MqttsnConstants.CHARSET).length;

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

        msg[idx++] = (byte) ((id >> 8) & 0xFF);
        msg[idx++] = (byte) (id & 0xFF);

        msg[idx++] = (byte) getReturnCode();

        if (sessionExpirySet) {
            writeUInt32(msg, idx, sessionExpiryInterval);
            idx += 4;
        }

        if (serverKeepAliveSet) {
            msg[idx++] = (byte) ((serverKeepAlive >> 8) & 0xFF);
            msg[idx++] = (byte) (serverKeepAlive & 0xFF);
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

        if (assignedClientId != null) {
            writeUTF8EncodedStringDataNoLength(msg, idx, assignedClientId);
        }

        return msg;
    }

    protected void readFlags(byte v) {
        /**
         Reserved      Auth  Server KA  Sess Exp  Sess Pres
         (7,6,5,4)     (3)   (2)        (1)       (0)
         **/

        if ((v & 0xF0) != 0) {
            throw new MqttsnCodecException("reserved flags must be set to 0");
        }

        auth = (v & 0x08) != 0;
        serverKeepAliveSet = (v & 0x04) != 0;
        sessionExpirySet = (v & 0x02) != 0;
        sessionPresent = (v & 0x01) != 0;
    }

    protected byte writeFlags() {
        byte v = 0x00;
        if (auth) v |= 0x08;
        if (serverKeepAliveSet) v |= 0x04;
        if (sessionExpirySet) v |= 0x02;
        if (sessionPresent) v |= 0x01;
        return v;
    }

    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    public void setSessionExpiryInterval(long sessionExpiryInterval) {
        this.sessionExpiryInterval = sessionExpiryInterval;
        this.sessionExpirySet = sessionExpiryInterval > 0;
    }

    public int getServerKeepAlive() {
        return serverKeepAlive;
    }

    public void setServerKeepAlive(int serverKeepAlive) {
        this.serverKeepAlive = serverKeepAlive;
        this.serverKeepAliveSet = serverKeepAlive > 0;
    }

    public boolean isAuth() {
        return auth;
    }

    public void setAuth(boolean auth) {
        this.auth = auth;
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

    public String getAssignedClientId() {
        return assignedClientId;
    }

    public void setAssignedClientId(String assignedClientId) {
        this.assignedClientId = assignedClientId;
    }

    public boolean isSessionPresent() {
        return sessionPresent;
    }

    public void setSessionPresent(boolean sessionPresent) {
        this.sessionPresent = sessionPresent;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateReturnCode(returnCode);
        if (sessionExpirySet) {
            MqttsnSpecificationValidator.validateSessionExpiry(sessionExpiryInterval);
        }
        if (serverKeepAliveSet) {
            MqttsnSpecificationValidator.validateKeepAlive(serverKeepAlive);
        }
        if (assignedClientId != null) {
            MqttsnSpecificationValidator.validateClientId(assignedClientId,
                    MqttsnConstants.UNSIGNED_MAX_16);
        }
    }

    @Override
    public String toString() {
        return "MqttsnConnack_V2_0{" +
                "returnCode=" + returnCode +
                ", sessionPresent=" + sessionPresent +
                ", sessionExpiryInterval=" + sessionExpiryInterval +
                ", serverKeepAlive=" + serverKeepAlive +
                ", auth=" + auth +
                ", assignedClientId='" + assignedClientId + '\'' +
                '}';
    }
}
