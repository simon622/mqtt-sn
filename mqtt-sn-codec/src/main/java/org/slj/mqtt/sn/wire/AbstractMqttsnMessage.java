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

package org.slj.mqtt.sn.wire;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnMessage;

import java.nio.charset.StandardCharsets;

public abstract class AbstractMqttsnMessage implements IMqttsnMessage {

    protected final int messageType;
    protected int id;
    protected int returnCode;

    public AbstractMqttsnMessage() {
        messageType = getMessageType();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (!needsId()) throw new MqttsnCodecException("unable to set id on message type");
        this.id = id;
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
    protected byte[] readRemainingBytesAdjusted(byte[] data, int startIdx) {
        int offset = (isLargeMessage(data) ? startIdx + 2 : startIdx);
        return readBytesAdjusted(data, startIdx, data.length - offset);
    }


    /**
     * Reads the number of bytes from the data allowing for a header size defined in its smallest
     * form for convenience but adjusted if the data is an extended type ie. > 255 bytes
     */
    protected byte[] readBytesAdjusted(byte[] data, int startIdx, int length) {
        int offset = (isLargeMessage(data) ? startIdx + 2 : startIdx);
        byte[] body = new byte[length];
        System.arraycopy(data, offset, body, 0, length);
        return body;
    }

    protected boolean readBooleanAdjusted(byte[] data, int startIdx) {
        return MqttsnWireUtils.read8bit(
                readHeaderByteWithOffset(data, startIdx)) == 1;
    }

    protected short readUInt8Adjusted(byte[] data, int startIdx) {
        return MqttsnWireUtils.read8bit(
                readHeaderByteWithOffset(data, startIdx));
    }

    protected int readUInt16Adjusted(byte[] data, int startIdx) {
        return MqttsnWireUtils.read16bit(
                readHeaderByteWithOffset(data, startIdx),
                readHeaderByteWithOffset(data, startIdx + 1));
    }

    protected long readUInt32Adjusted(byte[] data, int startIdx) {

        return  ((long) (readHeaderByteWithOffset(data, startIdx) & 0xFF) << 24) |
                ((long) (readHeaderByteWithOffset(data, startIdx + 1) & 0xFF) <<  16) |
                ((long) (readHeaderByteWithOffset(data, startIdx + 2) & 0xFF) << 8) |
                ((long) (readHeaderByteWithOffset(data, startIdx + 3) & 0xFF));
    }

    protected String readUTF8EncodedStringAdjusted(byte[] data, int startIdx) {

        //-- read 16 bit from the beginning of the field to obtain the length of the UTF8 string
        //-- this will determine the length
        int size = readUInt16Adjusted(data, startIdx);
        byte[] arr = readBytesAdjusted(data, startIdx + 2, size);
        String decodedString = new String(arr, MqttsnConstants.CHARSET);
        return decodedString;
    }

    protected static void writeUTF8EncodedStringData(byte[] dest, int startIdx, String stringData) {

        byte[] arr = stringData.getBytes(MqttsnConstants.CHARSET);
        if(arr.length > MqttsnConstants.UNSIGNED_MAX_16)
            throw new MqttsnCodecException("invalid encoded string data length");

        if(((arr.length) + 2) > (dest.length - startIdx))
            throw new MqttsnCodecException("buffer too small to accommodate data");

        dest[startIdx++] = (byte) ((arr.length >> 8) & 0xFF);
        dest[startIdx++] = (byte) (arr.length & 0xFF);

        System.arraycopy(arr, 0, dest, startIdx, arr.length);
    }

    protected static void writeUTF8EncodedStringDataNoLength(byte[] dest, int startIdx, String stringData) {

        byte[] arr = stringData.getBytes(MqttsnConstants.CHARSET);
        if(arr.length > MqttsnConstants.UNSIGNED_MAX_16)
            throw new MqttsnCodecException("invalid encoded string data length");

        if(((arr.length)) > (dest.length - startIdx))
            throw new MqttsnCodecException("buffer too small to accommodate data");

        System.arraycopy(arr, 0, dest, startIdx, arr.length);
    }

    /**
     * Reads the remaining bytes as UTF-8 encoded bytes WITHOUT the preceding 16 bits size
     */
    protected String readRemainingUTF8EncodedAdjustedNoLength(byte[] data, int startIdx) {
        byte[] arr = readRemainingBytesAdjusted(data, startIdx);
        String decodedString = new String(arr, MqttsnConstants.CHARSET);
        return decodedString;
    }

    protected void writeUInt32(byte[] data, int startIdx, long value){
        data[startIdx++] = (byte) (value >> 24);
        data[startIdx++] = (byte) (value >> 16);
        data[startIdx++] = (byte) (value >> 8);
        data[startIdx] = (byte) (value);
    }

    public String getMessageName() {
        return getClass().getSimpleName();
    }

    public boolean needsId() {
        return false;
    }

    public abstract int getMessageType();

    public abstract void decode(byte[] data) throws MqttsnCodecException;

    public abstract byte[] encode() throws MqttsnCodecException;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getMessageName());
        if (needsId()) {
            sb.append('{').append(getId()).append("}");
        }
        return sb.toString();
    }


    public static byte readHeaderByteWithOffset(byte[] data, int index) {
        return isLargeMessage(data) ? data[index + 2] : data[index];
    }

    public static boolean isLargeMessage(byte[] data) {
        return data[0] == 0x01;
    }

    public static int readMessageLength(byte[] data) {
        int length = 0;
        if (isLargeMessage(data)) {
            //big payload
            length = ((data[1] & 0xFF) << 8) + (data[2] & 0xFF);
        } else {
            //small payload
            length = (data[0] & 0xFF);
        }
        return length;
    }

    public static void main(String[] args) {
        byte[] arr = new byte[7];
        writeUTF8EncodedStringData(arr, 0, "simon");
        System.out.println(MqttsnWireUtils.toBinary(arr));
    }
}
