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
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.version1_2.Mqttsn_v1_2_Codec;

public abstract class AbstractMqttsnMessage implements IMqttsnMessage {

    protected final int messageType;

    /* The MsgId field is 2-octet long and corresponds to the MQTT ‘Message ID’ parameter. It allows the sender to
    match a message with its corresponding acknowledgment. */
    protected int msgId;

    protected int returnCode;

    public AbstractMqttsnMessage() {
        messageType = getMessageType();
    }

    public int getMsgId() {
        return msgId;
    }

    public void setMsgId(int msgId) {
        if (!needsMsgId()) throw new MqttsnCodecException("unable to set msg id on message type");
        this.msgId = msgId;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public boolean isErrorMessage() {
        return returnCode != MqttsnConstants.RETURN_CODE_ACCEPTED;
    }

    /**
     * Reads the remaining body from the data allowing for a header size defined in its smallest
     * form for convenience but adjusted if the data is an extended type ie. > 255 bytes
     */
    protected byte[] readRemainingBytesFromIndexAdjusted(byte[] data, int headerSize) {
        int offset = (Mqttsn_v1_2_Codec.isExtendedMessage(data) ? headerSize + 2 : headerSize);
        return readBytesFromIndexAdjusted(data, headerSize, data.length - offset);
    }

    /**
     * Reads the number of bytes from the data allowing for a header size defined in its smallest
     * form for convenience but adjusted if the data is an extended type ie. > 255 bytes
     */
    protected byte[] readBytesFromIndexAdjusted(byte[] data, int headerSize, int length) {
        int size = Mqttsn_v1_2_Codec.readMessageLength(data);
        int offset = (Mqttsn_v1_2_Codec.isExtendedMessage(data) ? headerSize + 2 : headerSize);
        byte[] msgData = new byte[length];
        System.arraycopy(data, offset, msgData, 0, length);
        return msgData;
    }

    /**
     * Reads an 8 bit field from the array starting at the given index (which will be auto-adjusted if the
     * data is an extended type ie. > 255 bytes)
     */
    protected int read8BitAdjusted(byte[] data, int startIdx) {
        return MqttsnWireUtils.read8bit(Mqttsn_v1_2_Codec.readHeaderByteWithOffset(data, startIdx));
    }

    /**
     * Reads a 16 bit field from the array starting at the given index (which will be auto-adjusted if the
     * data is an extended type ie. > 255 bytes)
     */
    protected int read16BitAdjusted(byte[] data, int startIdx) {
        return MqttsnWireUtils.read16bit(
                Mqttsn_v1_2_Codec.readHeaderByteWithOffset(data, startIdx),
                Mqttsn_v1_2_Codec.readHeaderByteWithOffset(data, startIdx + 1));
    }

    public String getMessageName() {
        return getClass().getSimpleName();
    }

    public boolean needsMsgId() {
        return false;
    }

    public abstract int getMessageType();

    public abstract void decode(byte[] data) throws MqttsnCodecException;

    public abstract byte[] encode() throws MqttsnCodecException;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getMessageName());
        if (needsMsgId()) {
            sb.append('{').append(getMsgId()).append("}");
        }
        return sb.toString();
    }
}
