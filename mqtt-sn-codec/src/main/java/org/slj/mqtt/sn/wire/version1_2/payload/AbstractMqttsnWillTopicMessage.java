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
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.version1_2.Mqttsn_v1_2_Codec;

public abstract class AbstractMqttsnWillTopicMessage extends AbstractMqttsnMessageWithFlagsField {

    byte[] willTopicData;

    public String getWillTopicData() {
        return willTopicData != null && willTopicData.length > 0 ? new String(willTopicData, MqttsnConstants.CHARSET) : null;
    }

    public void setWillTopic(String willTopic) {
        this.willTopicData = willTopic.getBytes(MqttsnConstants.CHARSET);
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        readFlags(readHeaderByteWithOffset(data, 2));
        willTopicData = readRemainingBytesAdjusted(data, 3);
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        byte[] msg;
        int length = 3 + willTopicData.length;
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

        if (willTopicData != null && willTopicData.length > 0) {
            System.arraycopy(willTopicData, 0, msg, idx, willTopicData.length);
        }

        return msg;
    }
}
