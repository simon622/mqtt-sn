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

import java.util.Arrays;

public class MqttsnProtection
        extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    public static final byte HMAC_SHA256 = 0x00,
          HMAC_SHA3_256 = 0x01,
          CMAC_128 = 0x02,
          CMAC_192 = 0x03,
          CMAC_256 = 0x04,
          AES_CCM_64_128 = 0x40,
          AES_CCM_64_192 = 0x41,
          AES_CCM_64_256 = 0x42,
          AES_CCM_128_128 = 0x43,
          AES_CCM_128_192 = 0x44,
          AES_CCM_128_256 = 0x45,
          AES_GCM_128_128 = 0x46,
          AES_GCM_192_128 = 0x47,
          AES_GCM_256_128 = 0x48,
          ChaCha20_Poly1305 = 0x49;

    byte [] ALLOWED_SCHEMES = new byte[]{
            HMAC_SHA3_256,
            CMAC_128,
            CMAC_192,
            CMAC_256,
            AES_CCM_64_128,
            AES_CCM_64_192,
            AES_CCM_64_256,
            AES_CCM_128_128,
            AES_CCM_128_192,
            AES_CCM_128_256,
            AES_GCM_128_128,
            AES_GCM_192_128,
            AES_GCM_256_128,
            ChaCha20_Poly1305
    };

    protected byte protectionSchema; //1 byte (byte 4)
    protected byte[] senderId = new byte[8]; //bytes 5-12
    protected int random; //bytes 13-16
    protected int keyMaterial; //bytes 17-P
    protected int monotonicCounter; //bytes q-r
    protected byte[] encapsultedPacket; //bytes S-T
    protected byte[] authTag; //bytes U-N

    //-- derived fields
    private int authTagLength = 0;
    private int keyMaterialLength = 0;
    private int monotonicCounterLength = 0;
    
    public MqttsnProtection(){
        Arrays.fill(this.senderId, (byte) 0x00);
    }

    public byte getProtectionSchema() {
        return protectionSchema;
    }

    public void setProtectionSchema(byte protectionSchema) {
        this.protectionSchema =  protectionSchema;
    }

    public byte[] getSenderId() {
        return senderId;
    }

    public void setSenderId(byte[] senderId) {
        if(senderId.length > 8){
            throw new MqttsnCodecException("senderId cannot exceed 8 bytes");
        }
        System.arraycopy(senderId, 0, this.senderId, 0, senderId.length);
    }

    public int getRandom() {
        return random;
    }

    public void setRandom(int random) {
        this.random = random;
    }

    public long getKeyMaterial() {
        return keyMaterial;
    }

    public void setKeyMaterial(int keyMaterial) {
        this.keyMaterial = keyMaterial;
    }

    public int getMonotonicCounter() {
        return monotonicCounter;
    }

    public void setMonotonicCounter(int monotonicCounter) {
        this.monotonicCounter = monotonicCounter;
    }

    public byte[] getEncapsultedPacket() {
        return encapsultedPacket;
    }

    public void setEncapsultedPacket(byte[] encapsultedPacket) {
        this.encapsultedPacket = encapsultedPacket;
    }

    public byte[] getAuthTag() {
        return authTag;
    }

    public void setAuthTag(byte[] authTag) {
        this.authTag = authTag;
    }

    @Override
    public int getMessageType() {
        return MqttsnConstants.PROTECTION;
    }


    protected void readFlags(byte b){
        /**
         Auth Tag Length X X X X
         Key Material Length X X
         Monotonic Counter Length X X
         **/

        authTagLength = (b & 0xF0) >> 4;
        keyMaterialLength = (b & 0x0C) >> 2;
        monotonicCounterLength = (b & 0x03);
    }

    protected byte writeFlags(){
        byte v = 0x00;
        v |= authTagLength << 4;
        v |= keyMaterialLength << 2;
        v |= monotonicCounterLength & 0x03;
        return v;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        readFlags(readByteAdjusted(data, 2));

        //-- now we have lengths to read
        protectionSchema = readByteAdjusted(data, 3);
        senderId = readBytesAdjusted(data, 4, 8);
        random = (int)readUInt32Adjusted(data, 12);

        //-- need a variable length marker now as the rest is offset against optional fields
        int idx = 16;
        if(keyMaterialLength > 0){
            keyMaterial =   keyMaterialLength == 2 ? readUInt16Adjusted(data, idx)  :
                            keyMaterialLength == 4 ? (int)readUInt32Adjusted(data, idx) : 0;
            idx += keyMaterialLength;
        }
        if(monotonicCounterLength > 0){
        	monotonicCounter =  monotonicCounterLength == 2 ? readUInt16Adjusted(data, idx)  :
								monotonicCounterLength == 4 ? (int)readUInt32Adjusted(data, idx) : 0;
            idx += monotonicCounterLength;
        }

        int encapSize = data.length - (idx + authTagLength);
        encapsultedPacket = readBytesAdjusted(data, idx, encapSize);
        idx += encapSize;

        authTag = readRemainingBytesAdjusted(data, idx);
        if(authTag.length != authTagLength){
            throw new MqttsnCodecException("Invalid security data");
        }
    }



    @Override
    public byte[] encode() throws MqttsnCodecException {

        //the field values must be set before encoding the data
        keyMaterialLength = determineNumberLengthInBytes(keyMaterial);
        monotonicCounterLength = determineNumberLengthInBytes(monotonicCounter);
        authTagLength = authTag.length;

        int length = 2; //type + len
        length += 1; //flags
        length += 1; //PS
        length += 8; //senderId
        length += 4; //nonce 4
        length += keyMaterialLength; //key material
        length += monotonicCounterLength; //mon. counter
        length += encapsultedPacket.length; //packet
        length += authTagLength; //auth tag

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

        System.arraycopy(senderId, 0, msg, idx, 8);
        idx += 8;

        writeUInt32(msg, idx, random);
        idx += 4;

        if(keyMaterialLength == 2){
            writeUInt16(msg, idx, (int) keyMaterial);
            idx += 2;
        } else if(keyMaterialLength == 4){
            writeUInt32(msg, idx, (int) keyMaterial);
            idx += 4;
        }

        if(monotonicCounterLength == 2){
            writeUInt16(msg, idx, (int) monotonicCounter);
            idx += 2;
        } else if(monotonicCounterLength == 4){
            writeUInt32(msg, idx, (int) monotonicCounter);
            idx += 4;
        }

        System.arraycopy(encapsultedPacket, 0, msg, idx, encapsultedPacket.length);
        idx += encapsultedPacket.length;

        System.arraycopy(authTag, 0, msg, idx, authTag.length);
        return msg;
    }


    @Override
    public void validate() throws MqttsnCodecException {

        if(Arrays.binarySearch(ALLOWED_SCHEMES, protectionSchema) == -1){
            throw new MqttsnCodecException("Invalid protection schema");
        }
        MqttsnSpecificationValidator.validateUInt32(random);
        MqttsnSpecificationValidator.validateByteArrayLength(senderId, 8);
        if(encapsultedPacket == null || encapsultedPacket.length < 2){
            throw new MqttsnCodecException("Invalid encapsulated value");
        }
        if(keyMaterialLength == 2){
            MqttsnSpecificationValidator.validateUInt16((int) keyMaterial);
        }
        else if(keyMaterialLength == 4){
            MqttsnSpecificationValidator.validateUInt32(keyMaterial);
        }
        if(monotonicCounterLength == 2){
            MqttsnSpecificationValidator.validateUInt16((int) monotonicCounter);
        }
        else if(monotonicCounterLength == 4){
            MqttsnSpecificationValidator.validateUInt32(monotonicCounter);
        }
        MqttsnSpecificationValidator.validateByteArrayLength(authTag, authTagLength);
    }

    private short determineNumberLengthInBytes(long number){
        if(number == 0) return 0;
        else if(number <= MqttsnConstants.UNSIGNED_MAX_16){
            return 2;
        } else if(number <= MqttsnConstants.UNSIGNED_MAX_32){
            return 4;
        }
        else throw new MqttsnCodecException("unsupported number length (" + number + ")");
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttsnIntegrity_V2_0{");
        sb.append(", protectionSchema=").append(protectionSchema);
        sb.append(", senderId=").append(Arrays.toString(senderId));
        sb.append(", random=").append(random);
        sb.append(", keyMaterial=").append(keyMaterial);
        sb.append(", keyMaterialLength=").append(keyMaterialLength);
        sb.append(", monotonicCounter=").append(monotonicCounter);
        sb.append(", monotonicCounterLength=").append(monotonicCounterLength);
        sb.append(", encapsultedPacket=").append(Arrays.toString(encapsultedPacket));
        sb.append(", authTag=").append(Arrays.toString(authTag));
        sb.append(", authTagLength=").append(authTagLength);
        sb.append('}');
        return sb.toString();
    }
}
