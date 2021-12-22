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

public class MqttsnPingreq_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected int maxMessages = 0;
    protected String clientId;

    @Override
    public int getMessageType() {
        return MqttsnConstants.PINGREQ;
    }

    public boolean needsId() {
        return false;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        if(data.length > 2){
            maxMessages = readUInt8Adjusted(data, 2);
        }
        if(data.length > 3){
            clientId = readUTF8EncodedStringAdjusted(data,3);
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        byte[] msg;
        int length = 3 + (clientId == null ? 0 : (clientId.length() + 2));
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
        msg[idx++] = (byte) getMaxMessages();

        if (clientId != null) {
            writeUTF8EncodedStringData(msg, idx, clientId);
        }

        return msg;
    }


    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateUInt8(maxMessages);
        if(clientId != null) MqttsnSpecificationValidator.validateClientId(clientId);
    }

    @Override
    public String toString() {
        return "MqttsnPingreq_V2_0{" +
                "maxMessages=" + maxMessages +
                ", clientId='" + clientId + '\'' +
                '}';
    }
}