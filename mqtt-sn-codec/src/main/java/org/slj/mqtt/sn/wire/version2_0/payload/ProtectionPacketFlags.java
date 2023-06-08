package org.slj.mqtt.sn.wire.version2_0.payload;

import org.slj.mqtt.sn.codec.MqttsnCodecException;

public class ProtectionPacketFlags {

	private static final short AUTHENTICATION_TAG_LENGTH_MULTIPLIER = 2; //bytes
    
	public static final short NO_CRYPTO_MATERIAL = 0; 
    public static final short SHORT_CRYPTO_MATERIAL = 2; //bytes
    public static final short LONG_CRYPTO_MATERIAL = 4; //bytes
    public static final short VERYLONG_CRYPTO_MATERIAL = 10; //bytes
    public static final short NO_MONOTONIC_COUNTER = 0; 
    public static final short SHORT_MONOTONIC_COUNTER = 2; //bytes
    public static final short LONG_MONOTONIC_COUNTER = 4; //bytes
    
	private byte authenticationTagLength = 0x00; //Reserved value by default to force a selection
    private byte cryptoMaterialLength = 0x03; //Reserved value by default to force a selection
    private byte monotonicCounterLength = 0x03; //Reserved value by default to force a selection
    
    private byte flagsAsByte=0x00;
    
    public ProtectionPacketFlags(byte authenticationTagLength, byte cryptoMaterialLength, byte monotonicCounterLength) throws MqttsnCodecException
    {
    	if(authenticationTagLength<0x03 || authenticationTagLength>0x0F)
    	{
    		throw new MqttsnCodecException("Invalid Authentication Tag Length flag! 0x"+ String.format("%02x", authenticationTagLength&0xff).toUpperCase());
    	}
    	if(cryptoMaterialLength>0x03)
    	{
    		throw new MqttsnCodecException("Invalid Crypto Material Length flag! 0x"+ String.format("%02x", cryptoMaterialLength&0xff).toUpperCase());
    	}
    	if(monotonicCounterLength>=0x03)
    	{
    		throw new MqttsnCodecException("Invalid Monotonic Counter Length flag! 0x"+ String.format("%02x", monotonicCounterLength&0xff).toUpperCase());
    	}
    	this.authenticationTagLength=authenticationTagLength;
    	this.cryptoMaterialLength=cryptoMaterialLength;
    	this.monotonicCounterLength=monotonicCounterLength;
    	this.flagsAsByte |= this.authenticationTagLength << 4;
    	this.flagsAsByte |= this.cryptoMaterialLength << 2;
    	this.flagsAsByte |= this.monotonicCounterLength & 0x03;
    }
    
    protected byte getFlagsAsByte(){
        return flagsAsByte;
    }
    
    public byte getAuthenticationTagLength() {
		return authenticationTagLength;
	}
	
    public byte getCryptoMaterialLength() {
		return cryptoMaterialLength;
	}

    public byte getMonotonicCounterLength() {
		return monotonicCounterLength;
	}
    
    public short getAuthenticationTagLengthDecoded() {
    	return (short) ((authenticationTagLength+1)*AUTHENTICATION_TAG_LENGTH_MULTIPLIER);
    }
    
    public short getCryptoMaterialLengthDecoded() {
    	switch(cryptoMaterialLength)
    	{
	    	case 0x01:
	    		return SHORT_CRYPTO_MATERIAL;
	    	case 0x02:
	    		return LONG_CRYPTO_MATERIAL;
	    	case 0x03:
	    		return VERYLONG_CRYPTO_MATERIAL;
	    	default:
	    		return NO_CRYPTO_MATERIAL;
    	}
    }

    public short getMonotonicCounterLengthDecoded() {
    	switch(monotonicCounterLength)
    	{
	    	case 0x01:
	    		return SHORT_MONOTONIC_COUNTER;
	    	case 0x02:
	    		return LONG_MONOTONIC_COUNTER;
	    	case 0x03:
	    		throw new MqttsnCodecException("Invalid Crypto Material Length! 0x"+ String.format("%02x", cryptoMaterialLength&0xff).toUpperCase()); 
	    	default:
	    		return NO_CRYPTO_MATERIAL;
    	}
    }

    @Override
    public String toString()
	{
    	StringBuilder sb = new StringBuilder("Protection Flags: ");
    	sb.append("0x").append(String.format("%02x", authenticationTagLength&0xff).toUpperCase()).
    		append(" (").append(getAuthenticationTagLengthDecoded()).append("bytes").append(")");
    	sb.append(", ").append("0x").append(String.format("%02x", cryptoMaterialLength&0xff).toUpperCase()).
    		append(" (").append(getCryptoMaterialLengthDecoded()).append("bytes").append(")");
    	sb.append(", ").append("0x").append(String.format("%02x", monotonicCounterLength&0xff).toUpperCase()).
    		append(" (").append(getMonotonicCounterLengthDecoded()).append("bytes").append(")");
    	return sb.toString();
    }
}
