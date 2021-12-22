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

public class MqttsnAuth extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected int authMethodLength;
    protected String authMethod;
    protected byte[] authData;

    @Override
    public int getMessageType() {
        return MqttsnConstants.AUTH;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        returnCode = readUInt8Adjusted(data, 2);
        authMethodLength = readUInt8Adjusted(data, 3);
        authMethod = new String(
                readBytesAdjusted(data, 4, authMethodLength), MqttsnConstants.CHARSET);
        authData = readRemainingBytesAdjusted(data, 4 + authMethodLength);
    }

    public int getAuthMethodLength() {
        return authMethodLength;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
        this.authMethodLength = authMethod == null ? 0 : authMethod.length();
    }

    public byte[] getAuthData() {
        return authData;
    }

    public void setAuthData(byte[] authData) {
        this.authData = authData;
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        int length = 4 + authMethodLength;
        length += authData == null ? 0 : authData.length;

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
        msg[idx++] = (byte) getReturnCode();
        msg[idx++] = (byte) authMethodLength;

        System.arraycopy(authMethod.getBytes(MqttsnConstants.CHARSET), 0, msg, idx, authMethodLength);
        idx += authMethodLength;
        System.arraycopy(authData, 0, msg, idx, authData.length);
        return msg;
    }


    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateAuthReasonCode(returnCode);
        MqttsnSpecificationValidator.validateUInt8(authMethodLength);
        MqttsnSpecificationValidator.validateStringData(authMethod, false);
        if(authMethodLength != authMethod.length()){
            throw new MqttsnCodecException("auth method does not match auth method length");
        }
    }
}
