package org.slj.mqtt.sn.wire.version2_0.payload;

import org.slj.mqtt.sn.codec.AbstractProtectionScheme;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IProtectionScheme;

public abstract class AbstractAeadProtectionScheme extends AbstractProtectionScheme implements IProtectionScheme
{
	public AbstractAeadProtectionScheme()
	{
		authenticationOnly=false;
	}
	
	//It returns the plaintext payload if the authenticity is verified, an exception otherwise
	abstract public byte[] unprotect(byte[] associatedData, byte[] encryptedPayload, byte[] tagToBeVerified, byte[] key) throws MqttsnCodecException;
	
	//It returns the tag of nominalTagLength
	abstract public byte[] protect(byte[] associatedData, byte[] plaintextPayload, byte[] key, byte[] encryptedPayload); 
}
