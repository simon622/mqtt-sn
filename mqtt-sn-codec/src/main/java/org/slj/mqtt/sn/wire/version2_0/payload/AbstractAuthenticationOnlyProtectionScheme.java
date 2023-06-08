package org.slj.mqtt.sn.wire.version2_0.payload;

import org.slj.mqtt.sn.codec.AbstractProtectionScheme;
import org.slj.mqtt.sn.codec.MqttsnCodecException;

public abstract class AbstractAuthenticationOnlyProtectionScheme extends AbstractProtectionScheme
{
	public AbstractAuthenticationOnlyProtectionScheme()
	{
		authenticationOnly=true;
	}
	
	//It returns the authenticatedPayload if the authenticity is verified, an exception otherwise
	abstract public byte[] unprotect(byte[] authenticatedPayload, byte[] tagToBeVerified, byte[] key) throws MqttsnCodecException;
	
	//It returns the tag of nominalTagLength
	abstract public byte[] protect(byte[] authenticatedPayload, byte[] key); 
}
