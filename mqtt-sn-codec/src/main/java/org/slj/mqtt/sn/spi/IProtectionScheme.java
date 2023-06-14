package org.slj.mqtt.sn.spi;

public interface IProtectionScheme {
	
	String getName();
	
	byte getIndex();
	
	short getNominalTagLengthInBytes();
	
	short getBlockSizeInBytes();
	
	boolean isAuthenticationOnly();
	
	byte[] getCryptoMaterial(byte cryptoMaterialLength);
}
