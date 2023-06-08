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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.codec.AbstractProtectionScheme;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.spi.IProtectionScheme;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;

import java.util.Arrays;

public class MqttsnProtection extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    private static final Logger logger = LoggerFactory.getLogger(MqttsnProtection.class);

    static final short SENDERID_FIELD_SIZE=8;
	static final short RANDOM_FIELD_SIZE=4;
    static final short PROTECTION_PACKET_FIXED_PART_LENGTH = 4 + //bytes for the fields "length", "packet type", "flags", "protection scheme"
															SENDERID_FIELD_SIZE + //bytes for the field "senderId"
															RANDOM_FIELD_SIZE; //bytes for the field "random"

    protected IProtectionScheme protectionScheme; //1 byte (byte 4)
    private byte[] senderId = new byte[SENDERID_FIELD_SIZE]; //bytes 5-12
    private int random; //bytes 13-16
    private int cryptoMaterial; //bytes 17-P
    private int monotonicCounter; //bytes q-r
    private byte[] encapsultedPacket; //bytes S-T
    private byte[] authTag; //bytes U-N
    private ProtectionPacketFlags flags = null;
    
    //-- derived fields
    private int authTagLength = 0;
    private int cryptoMaterialLength = 0;
    private int monotonicCounterLength = 0;
    
    public MqttsnProtection(){
        Arrays.fill(this.senderId, (byte) 0x00);
    }

    public IProtectionScheme getProtectionScheme() {
        return protectionScheme;
    }

    public void setProtectionScheme(IProtectionScheme protectionScheme) {
        this.protectionScheme =  protectionScheme;
    }

    public byte[] getSenderId() {
        return senderId;
    }

    public void setSenderId(byte[] senderId) {
        if(senderId.length > SENDERID_FIELD_SIZE){
            throw new MqttsnCodecException("senderId cannot exceed "+SENDERID_FIELD_SIZE+" bytes");
        }
        System.arraycopy(senderId, 0, this.senderId, 0, senderId.length);
    }

    public int getRandom() {
        return random;
    }

    public void setRandom(int random) {
        this.random = random;
    }

    public long getCryptoMaterial() {
        return cryptoMaterial;
    }

    public void setCryptoMaterial(int cryptoMaterial) {
        this.cryptoMaterial = cryptoMaterial;
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

    public void setFlags(ProtectionPacketFlags flags)
    {
    	this.flags=flags;
    }
    
    protected void readFlags(byte b){
        /**
         Auth Tag Length X X X X
         Crypto Material Length X X
         Monotonic Counter Length X X
         **/

        authTagLength = (b & 0xF0) >> 4;
        cryptoMaterialLength = (b & 0x0C) >> 2;
        monotonicCounterLength = (b & 0x03);
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        readFlags(readByteAdjusted(data, 2));

        //-- now we have lengths to read
        protectionScheme = AbstractProtectionScheme.getProtectionScheme(readByteAdjusted(data, 3));
        senderId = readBytesAdjusted(data, 4, SENDERID_FIELD_SIZE);
        random = (int)readUInt32Adjusted(data, 12);

        //-- need a variable length marker now as the rest is offset against optional fields
        int idx = 16;
        if(cryptoMaterialLength > 0){
            cryptoMaterial =   cryptoMaterialLength == 2 ? readUInt16Adjusted(data, idx)  :
            	cryptoMaterialLength == 4 ? (int)readUInt32Adjusted(data, idx) : 0;
            idx += cryptoMaterialLength;
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

        int length = PROTECTION_PACKET_FIXED_PART_LENGTH;
        logger.debug("1---->"+length);
        length += flags.getCryptoMaterialLengthDecoded(); //crypto material bytes
        logger.debug("2---->"+length);
        length += flags.getMonotonicCounterLengthDecoded(); //monotonic counter
        logger.debug("3---->"+length);
        length += encapsultedPacket.length; //packet
        logger.debug("4---->"+length);
        length += flags.getAuthenticationTagLengthDecoded(); //authentication tag length
        logger.debug("5---->"+length);

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
        msg[idx++] = flags.getFlagsAsByte();
        msg[idx++] = protectionScheme.getIndex();

        System.arraycopy(senderId, 0, msg, idx, SENDERID_FIELD_SIZE);
        idx += SENDERID_FIELD_SIZE;

        writeUInt32(msg, idx, random);
        idx += RANDOM_FIELD_SIZE;

        short cryptoMaterialLengthDecoded=flags.getCryptoMaterialLengthDecoded();
        if(cryptoMaterialLengthDecoded == 2){
            writeUInt16(msg, idx, (short) cryptoMaterial);
            idx += 2;
        } else if(cryptoMaterialLengthDecoded == 4){
            writeUInt32(msg, idx, (int) cryptoMaterial);
            idx += 4;
        }

        short monotonicCounterLengthDecoded=flags.getMonotonicCounterLengthDecoded();
        if(monotonicCounterLengthDecoded == 2){
            writeUInt16(msg, idx, (short) monotonicCounter);
            idx += 2;
        } else if(monotonicCounterLengthDecoded == 4){
            writeUInt32(msg, idx, (int) monotonicCounter);
            idx += 4;
        }

        System.arraycopy(encapsultedPacket, 0, msg, idx, encapsultedPacket.length);
        idx += encapsultedPacket.length;

        byte[] authenticationTag=null;
        /*if(protectionScheme.isAuthenticationOnly())
        {
        	authenticationTag = protect(msg, byte[] key); 
        }
        else
        {
        	
        }*/
        logger.error("--->"+protectionScheme.getIndex());
        logger.error("--->"+protectionScheme.getClass());
        logger.error("--->"+protectionScheme.isAuthenticationOnly());
        
        System.arraycopy(authTag, 0, msg, idx, cryptoMaterialLengthDecoded);
        return msg;
    }


    @Override
    public void validate() throws MqttsnCodecException {

        /*if(Arrays.binarySearch(ALLOWED_SCHEMES, protectionSchema) == -1){
            throw new MqttsnCodecException("Invalid protection schema");
        }*/
        MqttsnSpecificationValidator.validateUInt32(random);
        MqttsnSpecificationValidator.validateByteArrayLength(senderId, SENDERID_FIELD_SIZE);
        if(encapsultedPacket == null || encapsultedPacket.length < 2){
            throw new MqttsnCodecException("Invalid encapsulated value");
        }
        if(cryptoMaterialLength == 2){
            MqttsnSpecificationValidator.validateUInt16((int) cryptoMaterial);
        }
        else if(cryptoMaterialLength == 4){
            MqttsnSpecificationValidator.validateUInt32(cryptoMaterial);
        }
        if(monotonicCounterLength == 2){
            MqttsnSpecificationValidator.validateUInt16((int) monotonicCounter);
        }
        else if(monotonicCounterLength == 4){
            MqttsnSpecificationValidator.validateUInt32(monotonicCounter);
        }
        MqttsnSpecificationValidator.validateByteArrayLength(authTag, authTagLength);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttsnIntegrity_V2_0{");
        sb.append(", protectionScheme=").append(protectionScheme.toString());
        sb.append(", senderId=").append(Arrays.toString(senderId));
        sb.append(", random=").append(random);
        sb.append(", cryptoMaterial=").append(cryptoMaterial);
        sb.append(", cryptoMaterialLength=").append(cryptoMaterialLength);
        sb.append(", monotonicCounter=").append(monotonicCounter);
        sb.append(", monotonicCounterLength=").append(monotonicCounterLength);
        sb.append(", encapsultedPacket=").append(Arrays.toString(encapsultedPacket));
        sb.append(", authTag=").append(Arrays.toString(authTag));
        sb.append(", authTagLength=").append(authTagLength);
        sb.append('}');
        return sb.toString();
    }
}
