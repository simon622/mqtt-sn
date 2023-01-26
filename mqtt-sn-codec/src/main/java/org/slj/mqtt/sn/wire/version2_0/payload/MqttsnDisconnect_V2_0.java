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
import org.slj.mqtt.sn.spi.IMqttsnDisconnectPacket;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.nio.charset.StandardCharsets;

public class MqttsnDisconnect_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator, IMqttsnDisconnectPacket {

    protected long sessionExpiryInterval;
    protected String reasonString;
    protected boolean retainRegistrations;
    protected boolean reasonStringExists;
    protected boolean sessionExpiryIntervalSet;
    protected boolean reasonCodeExists;

    @Override
    public int getMessageType() {
        return MqttsnConstants.DISCONNECT;
    }

    public boolean needsId() {
        return false;
    }

    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    public void setSessionExpiryInterval(long sessionExpiryInterval) {
        sessionExpiryIntervalSet = true;
        this.sessionExpiryInterval = sessionExpiryInterval;
    }

    public String getReasonString() {
        return reasonString;
    }

    public boolean isRetainRegistrations() {
        return retainRegistrations;
    }

    public void setRetainRegistrations(boolean retainRegistrations) {
        this.retainRegistrations = retainRegistrations;
    }

    public void setReasonString(String reasonString) {
        if(reasonString !=null ) reasonStringExists = true;
        this.reasonString = reasonString;
    }

    @Override
    public void setReturnCode(int returnCode) {
        reasonCodeExists = true;
        super.setReturnCode(returnCode);
    }

    protected void readFlags(byte v) {
        /**
         Reserved       ReasonCodePresent       Session Exp Present     ReasonString Present RetainRegistrations
         (7,6,5,4)     (3)                          (2)                     (1)                     (0)
         **/

        reasonCodeExists = (v & 0x08) != 0;
        sessionExpiryIntervalSet = (v & 0x04) != 0;
        reasonStringExists = (v & 0x02) != 0;
        retainRegistrations = (v & 0x01) != 0;
    }

    protected byte writeFlags() {
        /**
         Reserved       ReasonCodePresent       Session Exp Present     ReasonString Present RetainRegistrations
         (7,6,5,4)     (3)                          (2)                     (1)                     (0)
         **/

        byte v = 0x00;
        if(getReturnCode() > 0) v |= 0x08;
        if(sessionExpiryInterval > 0)  v |= 0x04;
        if(reasonString != null)  v |= 0x02;
        if(retainRegistrations)  v |= 0x01;
        return v;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        if(data.length > 2){
            readFlags(readHeaderByteWithOffset(data, 2));
            int idx = 3;
            if(reasonCodeExists){
                returnCode = readUInt8Adjusted(data, idx++);
            }
            if(sessionExpiryIntervalSet){
                sessionExpiryInterval = readUInt32Adjusted(data, idx);
                idx += 4;
            }
            if(reasonStringExists){
                reasonString = readRemainingUTF8EncodedAdjustedNoLength(data, idx);
            }
        }
    }


    @Override
    public byte[] encode() throws MqttsnCodecException {

        byte[] msg;

        int length = 3;

        if(returnCode > 0){
            length += 1;
        }
        if(sessionExpiryInterval > 0){
            length += 4;
        }
        if(reasonString != null){
            length += reasonString.length();
        }

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

        if(returnCode > 0){
            msg[idx++] = (byte) getReturnCode();
        }

        if(sessionExpiryInterval > 0){
            writeUInt32(msg, idx, sessionExpiryInterval);
            idx += 4;
        }

        if(reasonString != null){
            writeUTF8EncodedStringDataNoLength(msg, idx, reasonString);
        }

        return msg;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateReturnCode(returnCode);
        if(sessionExpiryInterval > 0) MqttsnSpecificationValidator.validateUInt32(sessionExpiryInterval);
        MqttsnSpecificationValidator.validStringData(reasonString, true);
    }

    @Override
    public String toString() {
        return "MqttsnDisconnect_V2_0{" +
                "reasonCode=" + returnCode +
                ", sessionExpiryInterval=" + sessionExpiryInterval +
                ", reasonString='" + reasonString + '\'' +
                ", retainRegistrations=" + retainRegistrations +
                ", reasonStringExists=" + reasonStringExists +
                ", sessionExpiryIntervalSet=" + sessionExpiryIntervalSet +
                ", reasonCodeExists=" + reasonCodeExists +
                '}';
    }
}