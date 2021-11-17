/*
 * Copyright (c) 2020 Simon Johnson <simon622 AT gmail DOT com>
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
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.slj.mqtt.sn.codec.MqttsnCodecException;

public class MqttsnGwInfo extends AbstractMqttsnMessage {

    protected int gatewayId;
    protected String gatewayAddress;

    public int getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(int gatewayId) {
        this.gatewayId = gatewayId;
    }

    public String getGatewayAddress() {
        return gatewayAddress;
    }

    public void setGatewayAddress(String gatewayAddress) {
        this.gatewayAddress = gatewayAddress;
    }

    @Override
    public int getMessageType() {
        return MqttsnConstants.GWINFO;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        gatewayId = (data[2] & 0xFF);
        byte[] body = new byte[data[0] - 3];
        if (body.length > 0) {
            System.arraycopy(data, 3, body, 0, body.length);
            gatewayAddress = new String(body, MqttsnConstants.CHARSET);
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        int length = 3 + (gatewayAddress == null ? 0 : gatewayAddress.length());
        byte[] data = new byte[length];
        data[0] = (byte) length;
        data[1] = (byte) getMessageType();
        data[2] = (byte) gatewayId;
        if (gatewayAddress != null) {
            System.arraycopy(gatewayAddress.getBytes(MqttsnConstants.CHARSET), 0, data, 3, gatewayAddress.length());
        }
        return data;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttsnGwInfo{");
        sb.append("gatewayId=").append(gatewayId);
        sb.append(", gatewayAddress='").append(gatewayAddress).append('\'');
        sb.append('}');
        return sb.toString();
    }
}