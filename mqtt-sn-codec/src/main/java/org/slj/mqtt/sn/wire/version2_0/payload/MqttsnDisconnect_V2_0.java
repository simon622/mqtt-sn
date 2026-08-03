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
import org.slj.mqtt.sn.spi.IMqttsnDisconnectPacket;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

/**
 * DISCONNECT - wire format per OASIS mqtt-sn-v2.0 CSD01 (05 Feb 2026), section 3.13, Figure 24.
 *
 * NB: unlike earlier drafts, DISCONNECT no longer carries the "going to sleep" duration or a
 * retain-registrations flag - that responsibility moved to the (not yet implemented) SLEEPREQ /
 * SLEEPRESP packets, see mqtt-sn-v2.0-gap-analysis.md.
 */
public class MqttsnDisconnect_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator, IMqttsnDisconnectPacket {

    //-- Disconnect Flags (byte 3)
    protected boolean packetIdSet;
    protected boolean sessionExpirySet;
    protected boolean reasonCodeSet;

    protected long sessionExpiryInterval;
    protected String reasonString;

    @Override
    public int getMessageType() {
        return MqttsnConstants.DISCONNECT_V2_0;
    }

    @Override
    public boolean needsId() {
        //-- the Packet Identifier is OPTIONAL on DISCONNECT (Table 3) and is not used to drive
        //-- inflight/confirmation state tracking - see getPacketIdentifier/setPacketIdentifier.
        return false;
    }

    public int getPacketIdentifier() {
        return id;
    }

    public void setPacketIdentifier(int packetIdentifier) {
        this.id = packetIdentifier;
        this.packetIdSet = true;
    }

    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    public void setSessionExpiryInterval(long sessionExpiryInterval) {
        this.sessionExpiryInterval = sessionExpiryInterval;
        this.sessionExpirySet = sessionExpiryInterval > 0;
    }

    public String getReasonString() {
        return reasonString;
    }

    public void setReasonString(String reasonString) {
        this.reasonString = reasonString;
    }

    @Override
    public void setReturnCode(int returnCode) {
        reasonCodeSet = true;
        super.setReturnCode(returnCode);
    }

    protected void readFlags(byte v) {
        /**
         Reserved       Reason C     Sess Exp     PacketId
         (7,6,5,4,3)    (2)          (1)          (0)
         **/

        if ((v & 0xF8) != 0) {
            throw new MqttsnCodecException("reserved disconnect flags must be set to 0");
        }

        reasonCodeSet = (v & 0x04) != 0;
        sessionExpirySet = (v & 0x02) != 0;
        packetIdSet = (v & 0x01) != 0;
    }

    protected byte writeFlags() {
        byte v = 0x00;
        if (reasonCodeSet) v |= 0x04;
        if (sessionExpirySet) v |= 0x02;
        if (packetIdSet) v |= 0x01;
        return v;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        if (data.length > 2) {
            int idx = 2;
            readFlags(readHeaderByteWithOffset(data, idx++));

            if (packetIdSet) {
                id = readUInt16Adjusted(data, idx);
                idx += 2;
            }

            if (sessionExpirySet) {
                sessionExpiryInterval = readUInt32Adjusted(data, idx);
                idx += 4;
            }

            if (reasonCodeSet) {
                returnCode = readUInt8Adjusted(data, idx++);
            }

            //-- reasonString is optional, its presence is inferred from any remaining bytes
            int consumedLength = MqttsnWireUtils.isLargeMessage(data) ? idx + 2 : idx;
            if (data.length > consumedLength) {
                reasonString = readRemainingUTF8EncodedAdjustedNoLength(data, idx);
            }
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        byte[] msg;

        byte[] reasonStringBytes = reasonString == null ? null : reasonString.getBytes(MqttsnConstants.CHARSET);

        //-- length(1) + type(1) + flags(1)
        int length = 3;

        if (packetIdSet) length += 2;
        if (sessionExpirySet) length += 4;
        if (reasonCodeSet) length += 1;
        if (reasonStringBytes != null) length += reasonStringBytes.length;

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

        if (packetIdSet) {
            msg[idx++] = (byte) ((id >> 8) & 0xFF);
            msg[idx++] = (byte) (id & 0xFF);
        }

        if (sessionExpirySet) {
            writeUInt32(msg, idx, sessionExpiryInterval);
            idx += 4;
        }

        if (reasonCodeSet) {
            msg[idx++] = (byte) getReturnCode();
        }

        if (reasonStringBytes != null) {
            writeUTF8EncodedStringDataNoLength(msg, idx, reasonString);
        }

        return msg;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateReturnCode(returnCode);
        if (packetIdSet) {
            MqttsnSpecificationValidator.validatePacketIdentifier(id);
        }
        if (sessionExpirySet) {
            MqttsnSpecificationValidator.validateUInt32(sessionExpiryInterval);
        }
        MqttsnSpecificationValidator.validateStringData(reasonString, true);
    }

    @Override
    public String toString() {
        return "MqttsnDisconnect_V2_0{" +
                "reasonCode=" + returnCode +
                ", packetIdSet=" + packetIdSet +
                ", id=" + id +
                ", sessionExpiryInterval=" + sessionExpiryInterval +
                ", reasonString='" + reasonString + '\'' +
                '}';
    }
}
