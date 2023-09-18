package org.slj.mqtt.sn.wire.version2_0.payload;

import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IProtectionScheme;

public class ProtectionPacketFlags {
	private static final short AUTHENTICATION_TAG_LENGTH_MULTIPLIER = 2; //bytes
    
	public static final byte NO_CRYPTO_MATERIAL = 0; 
    public static final byte SHORT_CRYPTO_MATERIAL = 2; //bytes
    public static final byte LONG_CRYPTO_MATERIAL = 4; //bytes
    public static final byte VERYLONG_CRYPTO_MATERIAL = 12; //bytes
    public static final byte NO_MONOTONIC_COUNTER = 0; 
    public static final byte SHORT_MONOTONIC_COUNTER = 2; //bytes
    public static final byte LONG_MONOTONIC_COUNTER = 4; //bytes
    
	private byte authenticationTagLength = 0x00; //Reserved value by default to force a selection
    private byte cryptoMaterialLength = 0x03; //Reserved value by default to force a selection
    private byte monotonicCounterLength = 0x03; //Reserved value by default to force a selection
    
    private byte flagsAsByte=0x00;
	protected final IProtectionScheme scheme;
    
    public static ProtectionPacketFlags decodeProtectionPacketFlags(byte flags, IProtectionScheme protectionScheme) throws MqttsnCodecException
    {
    	return new ProtectionPacketFlags((byte)((((byte)(flags & 0xF0)) >> 4) & 0x0F), (byte)(((byte)(flags & 0x0C)) >> 2), (byte)((flags & 0x03)),protectionScheme);
    }
    
    public ProtectionPacketFlags(byte authenticationTagLength, byte cryptoMaterialLength, byte monotonicCounterLength, IProtectionScheme protectionScheme) throws MqttsnCodecException
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
    	if(!protectionScheme.isAuthenticationOnly() && protectionScheme.getNominalTagLengthInBytes()>getAuthenticationTagLengthDecoded())
    	{
    		throw new MqttsnCodecException("Invalid Authentication Tag Length as shorter than the one defined by the protection scheme "+protectionScheme.getName()+" used! Flag=0x"+ String.format("%02x", authenticationTagLength&0xff).toUpperCase());
    	}
        this.cryptoMaterialLength=cryptoMaterialLength;
    	this.monotonicCounterLength=monotonicCounterLength;
    	this.flagsAsByte |= this.authenticationTagLength << 4;
    	this.flagsAsByte |= this.cryptoMaterialLength << 2;
    	this.flagsAsByte |= this.monotonicCounterLength & 0x03;
		this.scheme = protectionScheme;
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
    
    public byte getCryptoMaterialLengthDecoded() {
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

    public byte getMonotonicCounterLengthDecoded() {
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

	public IProtectionScheme getProtectionScheme() {
		return scheme;
	}

	@Override
    public String toString()
	{
    	StringBuilder sb = new StringBuilder("Flags= ");
    	sb.append("0x").append(String.format("%02x", authenticationTagLength&0xff).toUpperCase()).
    		append(" (").append(getAuthenticationTagLengthDecoded()).append("bytes").append(")");
    	sb.append(", ").append("0x").append(String.format("%02x", cryptoMaterialLength&0xff).toUpperCase()).
    		append(" (").append(getCryptoMaterialLengthDecoded()).append("bytes").append(")");
    	sb.append(", ").append("0x").append(String.format("%02x", monotonicCounterLength&0xff).toUpperCase()).
    		append(" (").append(getMonotonicCounterLengthDecoded()).append("bytes").append(")");
    	return sb.toString();
    }
}
