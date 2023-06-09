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
import org.slj.mqtt.sn.codec.AbstractProtectionScheme;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.spi.IProtectionScheme;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.math.BigInteger;
import java.util.Arrays;

public class MqttsnProtection extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    private static final Logger logger = LoggerFactory.getLogger(MqttsnProtection.class);

    static final short FLAGS_FIELD_BYTE_INDEX=2;
    static final short PROTECTIONSCHEME_FIELD_BYTE_INDEX=3;
    static final short SENDERID_FIELD_BYTE_INDEX=4;
    static final short RANDOM_FIELD_BYTE_INDEX=12;
    static final short SENDERID_FIELD_SIZE=8;
	static final short RANDOM_FIELD_SIZE=4;
    static final short PROTECTION_PACKET_FIXED_PART_LENGTH = 4 + //bytes for the fields "length", "packet type", "flags", "protection scheme"
															SENDERID_FIELD_SIZE + //bytes for the field "senderId"
															RANDOM_FIELD_SIZE; //bytes for the field "random"

    protected IProtectionScheme protectionScheme=null; //1 byte (byte 4)
    private byte[] senderId = new byte[SENDERID_FIELD_SIZE]; //bytes 5-12
    private byte[] random; //bytes 13-16
    private byte[] cryptoMaterial=null; //bytes 17-P
    private int monotonicCounter; //bytes q-r
    private byte[] encapsulatedPacket=null; //bytes S-T
    private byte[] authenticationTag=null; //bytes U-N
    private ProtectionPacketFlags flags = null;
    private byte[] protectionKey=null;
    private byte[] protectionPacket=null; //bytes 1-N
    private short protectionPacketLength;
    
  /*  //-- derived fields
    private int authenticationTagLength = 0;
    private int cryptoMaterialLength = 0;
    private int monotonicCounterLength = 0;*/
    
    public MqttsnProtection(){
        Arrays.fill(this.senderId, (byte) 0x00);
    }

    public IProtectionScheme getProtectionScheme() {
        return protectionScheme;
    }

    public void setProtectionScheme(IProtectionScheme protectionScheme) {
        this.protectionScheme =  protectionScheme;
    }

    public void setProtectionKey(byte[] protectionKey) {
        this.protectionKey=protectionKey;
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

    public byte[] getRandom() {
        return random;
    }

    public void setRandom(byte[] random) {
        this.random = random;
    }

    public byte[] getCryptoMaterial() {
        return cryptoMaterial;
    }

    public void setCryptoMaterial(byte[] cryptoMaterial) {
    	this.cryptoMaterial = cryptoMaterial;
    }

    public int getMonotonicCounter() {
        return monotonicCounter;
    }

    public void setMonotonicCounter(int monotonicCounter) {
        this.monotonicCounter = monotonicCounter;
    }

    public byte[] getEncapsulatedPacket() {
        return encapsulatedPacket;
    }

    public void setEncapsulatedPacket(byte[] encapsulatedPacket) {
        this.encapsulatedPacket = encapsulatedPacket;
    }

    public byte[] getAuthenticationTag() {
        return authenticationTag;
    }

    public void setAuthenticationTag(byte[] authenticationTag) {
        this.authenticationTag = authenticationTag;
    }

    @Override
    public int getMessageType() {
        return MqttsnConstants.PROTECTION;
    }

    public void setFlags(ProtectionPacketFlags flags)
    {
    	this.flags=flags;
    }
    
    private short readInt16Adjusted(byte[] data, int startIdx) {
        int unprocessedResult = 
        		((readHeaderByteWithOffset(data, startIdx) & 0xFF) << 8) |
                ((readHeaderByteWithOffset(data, startIdx + 1) & 0xFF));
        return (short) unprocessedResult;
    }

    private int readInt32Adjusted(byte[] data, int startIdx) {
        long unprocessedResult=
        		((readHeaderByteWithOffset(data, startIdx) & 0xFF) << 24) |
                ((readHeaderByteWithOffset(data, startIdx + 1) & 0xFF) <<  16) |
                ((readHeaderByteWithOffset(data, startIdx + 2) & 0xFF) << 8) |
                ((readHeaderByteWithOffset(data, startIdx + 3) & 0xFF));
        return (int) unprocessedResult;
    }

    private void writeInt32(byte[] data, int startIdx, int value){
        data[startIdx++] = (byte) (value >> 24);
        data[startIdx++] = (byte) (value >> 16);
        data[startIdx++] = (byte) (value >> 8);
        data[startIdx] = (byte) (value);
    }

    private void writeInt16(byte[] data, int startIdx, short value){
        data[startIdx++] = (byte) (value >> 8);
        data[startIdx] = (byte) (value);
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
    	protectionPacketLength = (short) data.length;
        logger.debug("protectionPacketLength---->"+protectionPacketLength);
    	flags=ProtectionPacketFlags.decodeProtectionPacketFlags(readByteAdjusted(data,FLAGS_FIELD_BYTE_INDEX));
        logger.debug("flags---->"+flags.toString());
        protectionScheme=AbstractProtectionScheme.getProtectionScheme(readByteAdjusted(data,PROTECTIONSCHEME_FIELD_BYTE_INDEX)); 		
        logger.debug("protectionScheme---->"+protectionScheme.getName());
        senderId=readBytesAdjusted(data,PROTECTIONSCHEME_FIELD_BYTE_INDEX,SENDERID_FIELD_SIZE);
        logger.debug("senderID---->0x"+MqttsnWireUtils.toHex(senderId));
        random=readBytesAdjusted(data,RANDOM_FIELD_BYTE_INDEX,RANDOM_FIELD_SIZE);
        logger.debug("random---->0x"+MqttsnWireUtils.toHex(random));
        int idx = PROTECTION_PACKET_FIXED_PART_LENGTH;
        byte cryptoMaterialLength=flags.getCryptoMaterialLengthDecoded();
        if(cryptoMaterialLength>0)
        {
            cryptoMaterial=readBytesAdjusted(data,PROTECTION_PACKET_FIXED_PART_LENGTH,cryptoMaterialLength);
            idx+=cryptoMaterialLength;
            logger.debug("cryptoMaterial---->0x"+MqttsnWireUtils.toHex(cryptoMaterial));
        }
        byte monotonicCounterLength=flags.getMonotonicCounterLengthDecoded();
        if(monotonicCounterLength>0)
        {
        	if(monotonicCounterLength == ProtectionPacketFlags.SHORT_MONOTONIC_COUNTER){
        		monotonicCounter=readInt16Adjusted(data, idx);
                idx += monotonicCounterLength;
            } else if(monotonicCounterLength == ProtectionPacketFlags.LONG_MONOTONIC_COUNTER){
        		monotonicCounter=readInt32Adjusted(data, idx);
                idx += monotonicCounterLength;
            }
        	logger.debug("monotonicCounter---->"+monotonicCounter);
        }
        short authenticatedTagLength=flags.getAuthenticationTagLengthDecoded();
        int encapsulatedPayloadLength = data.length - (idx + authenticatedTagLength);
        
        //byte a = readByteAdjusted(data, 3);
        /*
        
        int encapSize = data.length - (idx + authTagLength);
        encapsultedPacket = readBytesAdjusted(data, idx, encapSize);
        idx += encapSize;

        authTag = readRemainingBytesAdjusted(data, idx);
        if(authTag.length != authTagLength){
            throw new MqttsnCodecException("Invalid security data");
        }*/
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {
        protectionPacketLength = PROTECTION_PACKET_FIXED_PART_LENGTH;
        protectionPacketLength += flags.getCryptoMaterialLengthDecoded(); //crypto material bytes
        protectionPacketLength += flags.getMonotonicCounterLengthDecoded(); //monotonic counter
        protectionPacketLength += encapsulatedPacket.length; //packet
        short authenticationTagLength=flags.getAuthenticationTagLengthDecoded(); //authentication tag length
        protectionPacketLength += authenticationTagLength;

        int idx = 0;
        if ((protectionPacketLength) > 0xFF) {
        	protectionPacketLength += 2;
            protectionPacket = new byte[protectionPacketLength];
            protectionPacket[idx++] = (byte) 0x01;
            protectionPacket[idx++] = ((byte) (0xFF & (protectionPacketLength >> 8)));
            protectionPacket[idx++] = ((byte) (0xFF & protectionPacketLength));
        } else {
        	protectionPacket = new byte[protectionPacketLength];
        	protectionPacket[idx++] = (byte) protectionPacketLength;
        }

        protectionPacket[idx++] = (byte) getMessageType();
        protectionPacket[idx++] = flags.getFlagsAsByte();
        protectionPacket[idx++] = protectionScheme.getIndex();

        System.arraycopy(senderId, 0, protectionPacket, idx, SENDERID_FIELD_SIZE);
        idx += SENDERID_FIELD_SIZE;

        System.arraycopy(random, 0, protectionPacket, idx, RANDOM_FIELD_SIZE);
        idx += RANDOM_FIELD_SIZE;

        short cryptoMaterialLengthDecoded=flags.getCryptoMaterialLengthDecoded();
        if(cryptoMaterialLengthDecoded!=0)
        {
        	System.arraycopy(cryptoMaterial, 0, protectionPacket, idx, cryptoMaterialLengthDecoded);
        	idx += cryptoMaterialLengthDecoded;
        }

        short monotonicCounterLengthDecoded=flags.getMonotonicCounterLengthDecoded();
        if(monotonicCounterLengthDecoded!=0)
        {
        	if(monotonicCounterLengthDecoded == ProtectionPacketFlags.SHORT_MONOTONIC_COUNTER){
                writeInt16(protectionPacket, idx, (short) monotonicCounter);
                idx += monotonicCounterLengthDecoded;
            } else if(monotonicCounterLengthDecoded == ProtectionPacketFlags.LONG_MONOTONIC_COUNTER){
                writeInt32(protectionPacket, idx, (int) monotonicCounter);
                idx += monotonicCounterLengthDecoded;
            }
        }

        System.arraycopy(encapsulatedPacket, 0, protectionPacket, idx, encapsulatedPacket.length);
        idx += encapsulatedPacket.length;

        if(protectionScheme.isAuthenticationOnly())
        {
            short payloadToBeAuthenticatedLength=(short) (protectionPacketLength-authenticationTagLength);
            byte[] payloadToBeAuthenticated=new byte[payloadToBeAuthenticatedLength];
            System.arraycopy(protectionPacket, 0, payloadToBeAuthenticated, 0, payloadToBeAuthenticatedLength);
        	authenticationTag = ((AbstractAuthenticationOnlyProtectionScheme)protectionScheme).protect(payloadToBeAuthenticated, protectionKey); 
        }
        else
        {
        	//TODO PP
        }
        
        System.arraycopy(authenticationTag, 0, protectionPacket, idx, flags.getAuthenticationTagLengthDecoded());
        return protectionPacket;
    }


    @Override
    public void validate() throws MqttsnCodecException {

       /* protected IProtectionScheme protectionScheme=null; //1 byte (byte 4)
        private byte[] senderId = new byte[SENDERID_FIELD_SIZE]; //bytes 5-12
        private byte[] random; //bytes 13-16
        private byte[] cryptoMaterial=null; //bytes 17-P
        private int monotonicCounter; //bytes q-r
        private byte[] encapsulatedPacket=null; //bytes S-T
        private byte[] authenticationTag=null; //bytes U-N
        private ProtectionPacketFlags flags = null;
        private byte[] protectionKey=null;
        private byte[] protectionPacket=null; //bytes 1-N
        private short protectionPacketLength;
*/
        /*if(Arrays.binarySearch(ALLOWED_SCHEMES, protectionSchema) == -1){
            throw new MqttsnCodecException("Invalid protection schema");
        }*/
        //MqttsnSpecificationValidator.validateUInt32(random);
        /*MqttsnSpecificationValidator.validateByteArrayLength(senderId, SENDERID_FIELD_SIZE);
        if(encapsulatedPacket == null || encapsulatedPacket.length < 2){
            throw new MqttsnCodecException("Invalid encapsulated value");
        }*/
        /*if(cryptoMaterialLength == 2){
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
        }*/
        //MqttsnSpecificationValidator.validateByteArrayLength(authTag, authTagLength);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder("Protection Packet:\n\t0x");
        sb.append(MqttsnWireUtils.toHex(protectionPacket));
        sb.append("\n\tLength=").append(protectionPacketLength);
        sb.append("\n\t").append(flags.toString());
        sb.append("\n\tProtection Scheme=").append(protectionScheme.getName()).append(" (0x").append(String.format("%02x", protectionScheme.getIndex()&0xff).toUpperCase()).append(")");
        sb.append("\n\tSenderId=0x").append(MqttsnWireUtils.toHex(senderId));
        sb.append("\n\tRandom=0x").append(MqttsnWireUtils.toHex(random));
        byte cryptoMaterialLength=flags.getCryptoMaterialLength();
        if(cryptoMaterialLength>0)
        	sb.append("\n\tCrypto Material=0x").append(MqttsnWireUtils.toHex(cryptoMaterial));
        else
            sb.append("\n\tCrypto Material Length=").append(cryptoMaterialLength);
        byte monotonicCounterLength=flags.getMonotonicCounterLength();
        if(monotonicCounterLength>0)
        	sb.append("\n\tMonotonic Counter=").append(monotonicCounter).append(" (0x").append(MqttsnWireUtils.toHex(BigInteger.valueOf(monotonicCounter).toByteArray())).append(")");
        else
            sb.append("\n\tMonotonic Counter Length=").append(monotonicCounterLength);
        sb.append("\n\tEncapsulatedPacket=0x").append(MqttsnWireUtils.toHex(encapsulatedPacket));
        sb.append("\n\tAuthenticationTagLength=").append(flags.getAuthenticationTagLengthDecoded());
        sb.append("\n\tAuthenticationTag=0x").append(MqttsnWireUtils.toHex(authenticationTag));
        return sb.toString();
    }
}
