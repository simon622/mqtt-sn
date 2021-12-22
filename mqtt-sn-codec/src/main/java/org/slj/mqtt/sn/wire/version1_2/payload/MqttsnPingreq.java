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

package org.slj.mqtt.sn.wire.version1_2.payload;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnIdentificationPacket;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;

public class MqttsnPingreq extends AbstractMqttsnMessage
        implements IMqttsnIdentificationPacket, IMqttsnMessageValidator {

    protected String clientId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public int getMessageType() {
        return MqttsnConstants.PINGREQ;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        byte[] body = readRemainingBytesAdjusted(data, 2);
        if (body.length > 0) {
            clientId = new String(body, MqttsnConstants.CHARSET);
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        int length = 2 + (clientId == null ? 0 : clientId.length());
        byte[] msg = null;
        int idx = 0;
        if (length > 0xFF) {
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

        if (clientId != null) {
            System.arraycopy(clientId.getBytes(MqttsnConstants.CHARSET), 0, msg, idx, clientId.length());
        }
        return msg;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttsnPingreq{");
        sb.append("clientId='").append(clientId).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void validate() throws MqttsnCodecException {
        if(clientId != null) MqttsnSpecificationValidator.validateClientId(clientId);
    }
}