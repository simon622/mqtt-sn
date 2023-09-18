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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MqttsnProtection extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    private static final Logger logger = LoggerFactory.getLogger(MqttsnProtection.class);
    //The serial number is obtained from SHA-224 of "MqttsnProtection"
    private static final long serialVersionUID = (new BigInteger("ca3875934ca453484558751e6e6da24d198c30f32b2e46933eebf774",16)).longValue();
    
    private static final short FLAGS_FIELD_BYTE_INDEX=2;
    private static final short PROTECTIONSCHEME_FIELD_BYTE_INDEX=3;
    private static final short SENDERID_FIELD_BYTE_INDEX=4;
    private static final short RANDOM_FIELD_BYTE_INDEX=12;
    private static final short SENDERID_FIELD_SIZE=8;
    private static final short RANDOM_FIELD_SIZE=4;
    private static final short PROTECTION_PACKET_FIXED_PART_LENGTH = 4 + //bytes for the fields "length", "packet type", "flags", "protection scheme"
															SENDERID_FIELD_SIZE + //bytes for the field "senderId"
															RANDOM_FIELD_SIZE; //bytes for the field "random"

    private ProtectionPacketFlags flags = null; //1 byte (byte 3)
    protected IProtectionScheme protectionScheme=null; //1 byte (byte 4)
    private byte[] senderId = new byte[SENDERID_FIELD_SIZE]; //bytes 5-12
    private byte[] random; //bytes 13-16
    private byte[] cryptoMaterial=null; //bytes 17-P
    private int monotonicCounter; //bytes q-r
    private byte[] encapsulatedPacket=null; //bytes S-T
    private byte[] encryptedEncapsulatedPacket=null;
    private byte[] authenticationTag=null; //bytes U-N
    private byte[] protectionKey=null;
    private byte[] protectionPacket=null; //bytes 1-N
    private short protectionPacketLength;
    private short authenticatedPayloadLength;
    private SecureRandom secureRandom = null;

    public MqttsnProtection()
    {
        Arrays.fill(this.senderId, (byte) 0x00);
        try {
            secureRandom = SecureRandom.getInstanceStrong();
        }
        catch(Exception e)
        {
        	throw new MqttsnCodecException(e);
        }
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
    	protectionPacket=data;
    	protectionPacketLength = (short) data.length;
        protectionScheme=AbstractProtectionScheme.getProtectionScheme(readByteAdjusted(data,PROTECTIONSCHEME_FIELD_BYTE_INDEX)); 		
    	flags=ProtectionPacketFlags.decodeProtectionPacketFlags(readByteAdjusted(data,FLAGS_FIELD_BYTE_INDEX),protectionScheme);
        senderId=readBytesAdjusted(data,SENDERID_FIELD_BYTE_INDEX,SENDERID_FIELD_SIZE);
        random=readBytesAdjusted(data,RANDOM_FIELD_BYTE_INDEX,RANDOM_FIELD_SIZE);
        int idx = PROTECTION_PACKET_FIXED_PART_LENGTH;
        byte cryptoMaterialLength=flags.getCryptoMaterialLengthDecoded();
        if(cryptoMaterialLength>0)
        {
            cryptoMaterial=readBytesAdjusted(data,PROTECTION_PACKET_FIXED_PART_LENGTH,cryptoMaterialLength);
            idx+=cryptoMaterialLength;
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
        }
        short authenticatedTagLength=flags.getAuthenticationTagLengthDecoded();
        int encapsulatedPacketLength = data.length - (idx + authenticatedTagLength);
        encapsulatedPacket = readBytesAdjusted(data, idx, encapsulatedPacketLength);
        idx += encapsulatedPacketLength;
        authenticationTag = readRemainingBytesAdjusted(data, idx);
        authenticatedPayloadLength = (short)(data.length - authenticatedTagLength);
        if(authenticationTag.length != authenticatedTagLength)
        {
            throw new MqttsnCodecException("Invalid security data: Authentication Tag is "+authenticationTag+" bytes");
        }
    }
    
    public byte[] unprotect(List<ProtectionKey> protectionKeys) throws MqttsnCodecException
    {
    	if(protectionScheme==null || flags==null)
    		throw new MqttsnCodecException("unprotect not called after a decode");
    	
        int availableKeys = protectionKeys.size();
    	if(protectionScheme.isAuthenticationOnly())
    	{
	        byte[] authenticatedPayload=new byte[authenticatedPayloadLength]; 
	        System.arraycopy(protectionPacket, 0, authenticatedPayload, 0, authenticatedPayloadLength);
	        
	        AbstractAuthenticationOnlyProtectionScheme abstractAuthenticationOnlyProtectionScheme=(AbstractAuthenticationOnlyProtectionScheme)protectionScheme;
			for(int i=0; i<availableKeys; i++)
	        {
		        try
		        {
		        	ProtectionKey protectionKey=protectionKeys.get(i);
		            if(abstractAuthenticationOnlyProtectionScheme.allowedKeyLength!=Byte.MIN_VALUE && abstractAuthenticationOnlyProtectionScheme.allowedKeyLength!=protectionKey.getProtectionKeyLength())
		            	continue;
		        	abstractAuthenticationOnlyProtectionScheme.unprotect(authenticatedPayload,authenticationTag,protectionKey.getProtectionKey());
		        	logger.debug("Protection key used: 0x"+protectionKey.getProtectionKeyHash());
		            logger.debug(toString());
			        return authenticatedPayload;
		        }
		        catch(Exception e)
		        {
		        	logger.debug("Authentication Tag invalid for key "+i);
		        }
	        }
            logger.debug(toString());
	        logger.error("Authentication Tag invalid!");
	        return null;
    	}
    	else
    	{
            byte associatedDataLength=(byte) (protectionPacketLength-flags.getAuthenticationTagLengthDecoded()-encapsulatedPacket.length);
            byte[] associatedData=new byte[associatedDataLength];
            System.arraycopy(protectionPacket, 0, associatedData, 0, associatedDataLength);
            encryptedEncapsulatedPacket=new byte[encapsulatedPacket.length];
            System.arraycopy(encapsulatedPacket, 0, encryptedEncapsulatedPacket, 0, encapsulatedPacket.length);

            AbstractAeadProtectionScheme abstractAeadProtectionScheme=(AbstractAeadProtectionScheme)protectionScheme;
	        for(int i=0; i<availableKeys; i++)
	        {
		        try
		        {
		        	ProtectionKey protectionKey=protectionKeys.get(i);
		            if(abstractAeadProtectionScheme.allowedKeyLength!=protectionKey.getProtectionKeyLength())
		            	continue;
		        	encapsulatedPacket=abstractAeadProtectionScheme.unprotect(associatedData,encapsulatedPacket,authenticationTag,protectionKey.getProtectionKey());
		        	logger.debug("Protection key used: 0x"+protectionKey.getProtectionKeyHash());
		            logger.debug(toString());
			        return encapsulatedPacket;
		        }
		        catch(Exception e)
		        {
		        	logger.debug("Authentication Tag invalid for key "+i,e);
		        }
	        }
            logger.debug(toString());
	        logger.error("Authentication Tag invalid!");
	        return null;
    	}
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {
        protectionPacketLength = PROTECTION_PACKET_FIXED_PART_LENGTH;
        protectionPacketLength += flags.getCryptoMaterialLengthDecoded(); //crypto material bytes
        protectionPacketLength += flags.getMonotonicCounterLengthDecoded(); //monotonic counter
        protectionPacketLength += encapsulatedPacket.length; //packet
        authenticatedPayloadLength = protectionPacketLength;
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

        random = new byte[RANDOM_FIELD_SIZE]; 
        secureRandom.nextBytes(random);
        
        System.arraycopy(random, 0, protectionPacket, idx, RANDOM_FIELD_SIZE);
        idx += RANDOM_FIELD_SIZE;

        byte cryptoMaterialLengthDecoded=flags.getCryptoMaterialLengthDecoded();
        cryptoMaterial=protectionScheme.getCryptoMaterial(cryptoMaterialLengthDecoded);
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

        if(protectionScheme.isAuthenticationOnly())
        {
            System.arraycopy(encapsulatedPacket, 0, protectionPacket, idx, encapsulatedPacket.length);
            idx += encapsulatedPacket.length;

            short payloadToBeAuthenticatedLength=(short) (protectionPacketLength-authenticationTagLength);
            byte[] payloadToBeAuthenticated=new byte[payloadToBeAuthenticatedLength];
            System.arraycopy(protectionPacket, 0, payloadToBeAuthenticated, 0, payloadToBeAuthenticatedLength);
            authenticationTag = Arrays.copyOfRange(((AbstractAuthenticationOnlyProtectionScheme)protectionScheme).protect(payloadToBeAuthenticated, protectionKey), 0, authenticationTagLength);
        }
        else
        {
            byte associatedDataLength=(byte) (protectionPacketLength-authenticationTagLength-encapsulatedPacket.length);
            byte[] associatedData=new byte[associatedDataLength];
            System.arraycopy(protectionPacket, 0, associatedData, 0, associatedDataLength);
            encryptedEncapsulatedPacket = new byte[encapsulatedPacket.length];
            authenticationTag = Arrays.copyOfRange(((AbstractAeadProtectionScheme)protectionScheme).protect(associatedData, encapsulatedPacket, protectionKey, encryptedEncapsulatedPacket), 0, authenticationTagLength);
            System.arraycopy(encryptedEncapsulatedPacket, 0, protectionPacket, idx, encapsulatedPacket.length);
            idx += encapsulatedPacket.length;
        }
        
        System.arraycopy(authenticationTag, 0, protectionPacket, idx, authenticationTagLength);
        return protectionPacket;
    }

    @Override
    public void validate() throws MqttsnCodecException {
    	if(flags==null)
    		throw new MqttsnCodecException("Flags not available");
    	if(protectionScheme==null)
    		throw new MqttsnCodecException("Protection scheme not available");
		AbstractProtectionScheme.getProtectionScheme(protectionScheme.getIndex());
		if(senderId==null || senderId.length!=SENDERID_FIELD_SIZE)
    		throw new MqttsnCodecException("SenderId not available or invalid length");
		if(random==null || random.length!=RANDOM_FIELD_SIZE)
    		throw new MqttsnCodecException("Random not available or invalid length");
		byte cryptoMaterialLength=flags.getCryptoMaterialLengthDecoded();
		if(cryptoMaterial==null && cryptoMaterialLength>0)
    		throw new MqttsnCodecException("CryptoMaterial not available");
		if(cryptoMaterial!=null && cryptoMaterialLength!=cryptoMaterial.length)
    		throw new MqttsnCodecException("CryptoMaterial invalid length");
		byte monotonicCounterLength=flags.getMonotonicCounterLengthDecoded();
		if(monotonicCounterLength==ProtectionPacketFlags.SHORT_MONOTONIC_COUNTER && (monotonicCounter<Short.MIN_VALUE || monotonicCounter>Short.MAX_VALUE))
    		throw new MqttsnCodecException("Monotonic Counter is not represented over a short integer");
		short authenticationTagLength=flags.getAuthenticationTagLengthDecoded();
		if(authenticationTag==null)
    		throw new MqttsnCodecException("Authentication Tag not available");
		if(authenticationTag!=null && authenticationTagLength!=authenticationTag.length)
    		throw new MqttsnCodecException("Authentication Tag invalid length");

        if(encapsulatedPacket == null || encapsulatedPacket.length < 2)
            throw new MqttsnCodecException("Invalid encapsulated value");
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
        byte monotonicCounterLength=flags.getMonotonicCounterLengthDecoded();
        if(monotonicCounterLength>0)
        {
        	if(monotonicCounterLength == ProtectionPacketFlags.SHORT_MONOTONIC_COUNTER)
        	{
            	sb.append("\n\tMonotonic Counter=").append(monotonicCounter).append(" (0x").append(MqttsnWireUtils.toHex(BigInteger.valueOf((short)monotonicCounter).toByteArray())).append(")");
            } 
        	else if(monotonicCounterLength == ProtectionPacketFlags.LONG_MONOTONIC_COUNTER)
        	{
            	sb.append("\n\tMonotonic Counter=").append(monotonicCounter).append(" (0x").append(Integer.toHexString(monotonicCounter)).append(")");
            }
        }
        else
            sb.append("\n\tMonotonic Counter Length=").append(monotonicCounterLength);
        sb.append("\n\tEncapsulated Packet=0x").append(MqttsnWireUtils.toHex(encapsulatedPacket));
        if(!protectionScheme.isAuthenticationOnly() && encryptedEncapsulatedPacket!=null)
            sb.append("\n\tEncrypted Encapsulated Packet=0x").append(MqttsnWireUtils.toHex(encryptedEncapsulatedPacket));
        sb.append("\n\tAuthenticated Payload Length=").append(authenticatedPayloadLength);
        sb.append("\n\tAuthentication Tag Length=").append(flags.getAuthenticationTagLengthDecoded());
        sb.append("\n\tAuthentication Tag=0x").append(MqttsnWireUtils.toHex(authenticationTag));
        return sb.toString();
    }
}
