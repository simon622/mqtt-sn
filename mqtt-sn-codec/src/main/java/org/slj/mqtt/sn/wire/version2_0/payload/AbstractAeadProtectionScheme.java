package org.slj.mqtt.sn.wire.version2_0.payload;

import org.slj.mqtt.sn.codec.AbstractProtectionScheme;
import org.slj.mqtt.sn.codec.MqttsnCodecException;

public abstract class AbstractAeadProtectionScheme extends AbstractProtectionScheme
{
	public AbstractAeadProtectionScheme()
	{
		authenticationOnly=false;
	}
	
	//It returns the plaintext payload if the authenticity is verified, an exception otherwisee
	abstract public byte[] unprotect(byte[] associatedData, byte[] encryptedPayload, byte[] tagToBeVerified, byte[] key) throws MqttsnCodecException;
	
	//It returns the tag of nominalTagLength
	abstract public byte[] protect(byte[] associatedData, byte[] plaintextPayload, byte[] key, byte[] encryptedPayload); 
}
