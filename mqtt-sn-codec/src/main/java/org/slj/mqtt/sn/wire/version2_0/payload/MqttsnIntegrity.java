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
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.util.Arrays;

public class MqttsnIntegrity
        extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected byte flags;
    protected byte protectionSchema; //1 byte
    protected long sequence; // 4 bytes
    protected byte[] publicUID = new byte[8]; //8 bytes
    protected int keyMaterial; //16 bit number
    protected byte[] cipherText; //n bytes
    protected byte[] mac; //n bytes (specified in header)

    //flags
    protected int macLength;
    protected boolean sequenceExists;
    protected boolean authOnly; //authOnly / auth & encrypted

    public MqttsnIntegrity(){
        Arrays.fill(this.publicUID, (byte) 0x00);
    }

    public byte getProtectionSchema() {
        return protectionSchema;
    }

    public void setProtectionSchema(byte protectionSchema) {
        this.protectionSchema = protectionSchema;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        sequenceExists = true;
        this.sequence = sequence;
    }

    public byte[] getPublicUID() {
        return publicUID;
    }

    public void setSenderUID(byte[] publicUID) {
        if(publicUID.length > 8){
            throw new MqttsnCodecException("publicUID cannot exceed 8 bytes");
        }
        System.arraycopy(publicUID, 0, this.publicUID, 0, publicUID.length);
    }

    public int getKeyMaterial() {
        return keyMaterial;
    }

    public void setKeyMaterial(int keyMaterial) {
        this.keyMaterial = keyMaterial;
    }

    public byte[] getCipherText() {
        return cipherText;
    }

    public void setCipherText(byte[] cipherText) {
        this.cipherText = cipherText;
    }

    public byte[] getMac() {
        return mac;
    }

    public void setMac(byte[] mac) {
        macLength = mac.length;
        this.mac = mac;
    }

    public boolean isAuthOnly() {
        return authOnly;
    }

    public void setAuthOnly(boolean authOnly) {
        this.authOnly = authOnly;
    }

    @Override
    public int getMessageType() {
        return MqttsnConstants.INTEGRITY;
    }

    protected void readFlags(byte b){
        /**
         Reserved       Mac Length       Sequence Exists            Auth Only
         (7,6)             (5,4,3,2)                    (1)                     (0)
         **/

        macLength = (b & 0x3c) >> 2;
        sequenceExists = (b & 0x02) != 0;
        authOnly = (b & 0x01) != 0;
    }



    protected byte writeFlags(){

        byte v = 0x00;
        v |= macLength << 2;
        if(sequenceExists)  v |= 0x02;
        if(authOnly)  v |= 0x01;
        return v;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        readFlags(readByteAdjusted(data, 2));
        protectionSchema = readByteAdjusted(data, 3);
        keyMaterial = readUInt16Adjusted(data, 4);
        int idx = 6;
        if(sequenceExists){
            sequence = readUInt32Adjusted(data, idx);
            idx += 4;
        }
        publicUID = readBytesAdjusted(data, idx, 8);
        idx += 8;

        int cipherSize = data.length - (idx + macLength);
        cipherText = readBytesAdjusted(data, idx, cipherSize);
        mac = readRemainingBytesAdjusted(data, idx + cipherSize);
    }



    @Override
    public byte[] encode() throws MqttsnCodecException {

        //mac size
        //sequence exists
        int length = 2;
        length += cipherText.length;
        length += macLength;
        if(sequenceExists ){
            length += 4;
        }
        length += 8; //8 bytes padded
        length += 2; //2 bytes keyMaterial
        length += 1; //1 byte protection schema
        length += 1; //1 byte flags

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
        msg[idx++] = writeFlags();
        msg[idx++] = protectionSchema;

        writeUInt16(msg, idx, keyMaterial);
        idx += 2;

        if(sequenceExists){
            writeUInt32(msg, idx, sequence);
            idx += 4;
        }

        System.arraycopy(publicUID, 0, msg, idx, 8);
        idx += 8;

        //cipher text
        System.arraycopy(cipherText, 0, msg, idx, cipherText.length);
        idx += cipherText.length;

        //mac
        System.arraycopy(mac, 0, msg, idx, macLength);

        return msg;
    }


    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateAuthReasonCode(returnCode);
        MqttsnSpecificationValidator.validateUInt32(sequence);
        MqttsnSpecificationValidator.validateByteArrayLength(publicUID, 8);
        MqttsnSpecificationValidator.validateByteArrayLength(mac, macLength);
        MqttsnSpecificationValidator.validateUInt16(keyMaterial);
    }

    @Override
    public String toString() {
        return "MqttsnIntegrity{" +
                ", protectionSchema=" + protectionSchema +
                ", sequence=" + sequence +
                ", keyMaterial=" + keyMaterial +
                ", publicUID=" + Arrays.toString(publicUID) +
                ", cipherText=" + Arrays.toString(cipherText) +
                ", mac=" + Arrays.toString(mac) +
                ", macLength=" + macLength +
                ", sequenceExists=" + sequenceExists +
                ", authOnly=" + authOnly +
                '}';
    }
}
