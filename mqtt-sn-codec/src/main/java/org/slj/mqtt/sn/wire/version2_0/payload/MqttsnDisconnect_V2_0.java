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
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;

import java.nio.charset.StandardCharsets;

public class MqttsnDisconnect_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected long sessionExpiryInterval;
    protected String reasonString;

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
        this.sessionExpiryInterval = sessionExpiryInterval;
    }

    public String getReasonString() {
        return reasonString;
    }

    public void setReasonString(String reasonString) {
        this.reasonString = reasonString;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        returnCode = readUInt8Adjusted(data, 2);
        if(data.length > 3){
            sessionExpiryInterval = readUInt32Adjusted(data, 3);
            if(data.length > 7){
                reasonString = new String(readRemainingBytesAdjusted(data, 7), MqttsnConstants.CHARSET);
            }
        }
    }


    @Override
    public byte[] encode() throws MqttsnCodecException {

        byte[] msg;

        int length = 3;
        if(sessionExpiryInterval > 0 || reasonString != null){
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
        msg[idx++] = (byte) getReturnCode();


        if(sessionExpiryInterval > 0 || reasonString != null){
            writeUInt32(msg, idx, sessionExpiryInterval);
            idx += 4;
            if(reasonString != null){
                byte[] reasonBytes = reasonString.getBytes(MqttsnConstants.CHARSET);
                System.arraycopy(reasonBytes, 0, msg, idx, reasonBytes.length);
            }
        }

        return msg;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateReturnCode(returnCode);
        if(sessionExpiryInterval > 0) MqttsnSpecificationValidator.validateUInt32(sessionExpiryInterval);
    }

    @Override
    public String toString() {
        return "MqttsnDisconnect_V2_0{" +
                "returnCode=" + returnCode +
                ", sessionExpiryInterval=" + sessionExpiryInterval +
                ", reasonString='" + reasonString + '\'' +
                '}';
    }
}