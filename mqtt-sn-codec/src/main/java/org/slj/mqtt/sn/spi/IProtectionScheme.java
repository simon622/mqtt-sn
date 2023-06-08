package org.slj.mqtt.sn.spi;

public interface IProtectionScheme {
	public byte getIndex();
	public short getNominalTagLength();
	public short getKeyLength();
	public boolean isAuthenticationOnly();
}
