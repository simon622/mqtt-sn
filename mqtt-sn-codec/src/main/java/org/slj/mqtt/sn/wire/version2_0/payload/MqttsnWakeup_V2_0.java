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
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;

/**
 * WAKEUP - wire format per OASIS mqtt-sn-v2.0 CSD01 (05 Feb 2026), section 3.14, Figure 25.
 * Header-only: Server-to-Client signal that the Client should wake up. No response expected.
 */
public class MqttsnWakeup_V2_0 extends AbstractMqttsnMessage {

    @Override
    public int getMessageType() {
        return MqttsnConstants.WAKEUP_V2_0;
    }

    @Override
    public boolean needsId() {
        return false;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {
        return new byte[]{2, (byte) getMessageType()};
    }

    @Override
    public String toString() {
        return "MqttsnWakeup_V2_0{}";
    }
}
