package org.slj.mqtt.sn.spi;

public interface IProtectionScheme {
	
	String getName();
	
	byte getIndex();
	
	short getNominalTagLength();
	
	short getKeyLength();
	
	boolean isAuthenticationOnly();
}
