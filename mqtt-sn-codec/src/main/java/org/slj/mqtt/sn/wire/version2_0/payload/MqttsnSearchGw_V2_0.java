/*
 * Copyright (c) 2026 Ian Craggs
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
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.util.Arrays;

/**
 * SEARCHGW - wire format per OASIS mqtt-sn-v2.0 CSD01 (05 Feb 2026), section 3.20.2, Figure 32.
 *
 * NB: unlike v1.2's SEARCHGW (which carries a fixed 1-byte broadcast "radius"), v2.0's SEARCHGW
 * has no radius field at all - instead an optional, variable-length, network-specific
 * "Additional Network Information" field (presence inferred from packet length).
 */
public class MqttsnSearchGw_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected byte[] additionalNetworkInformation;

    @Override
    public int getMessageType() {
        return MqttsnConstants.SEARCHGW_V2_0;
    }

    @Override
    public boolean needsId() {
        return false;
    }

    public byte[] getAdditionalNetworkInformation() {
        return additionalNetworkInformation;
    }

    public void setAdditionalNetworkInformation(byte[] additionalNetworkInformation) {
        this.additionalNetworkInformation = additionalNetworkInformation;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        int consumedLength = MqttsnWireUtils.isLargeMessage(data) ? 4 : 2;
        if (data.length > consumedLength) {
            additionalNetworkInformation = readRemainingBytesAdjusted(data, 2);
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {
        int length = 2 + (additionalNetworkInformation == null ? 0 : additionalNetworkInformation.length);
        byte[] data = new byte[length];
        data[0] = (byte) length;
        data[1] = (byte) getMessageType();
        if (additionalNetworkInformation != null) {
            System.arraycopy(additionalNetworkInformation, 0, data, 2, additionalNetworkInformation.length);
        }
        return data;
    }

    @Override
    public void validate() throws MqttsnCodecException {
    }

    @Override
    public String toString() {
        return "MqttsnSearchGw_V2_0{" +
                "additionalNetworkInformation=" + Arrays.toString(additionalNetworkInformation) +
                '}';
    }
}
