package org.slj.mqtt.sn.wire.version2_0.payload;

import java.security.MessageDigest;

import org.slj.mqtt.sn.codec.AbstractProtectionScheme;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IProtectionScheme;

public abstract class AbstractAeadProtectionScheme extends AbstractProtectionScheme implements IProtectionScheme
{
	private static final String IV_NONCE_HASH_ALGORITHM="SHA-256";

	protected MessageDigest digest;
	protected byte allowedKeyLength;

	public AbstractAeadProtectionScheme()
	{
		authenticationOnly=false;
		try 
		{
			digest = MessageDigest.getInstance(IV_NONCE_HASH_ALGORITHM);
        }
        catch(Exception e)
        {
        	throw new MqttsnCodecException(e);
        }
	}
	
	//It returns the plaintext payload if the authenticity is verified, an exception otherwise
	abstract public byte[] unprotect(byte[] associatedData, byte[] encryptedPayload, byte[] tagToBeVerified, byte[] key) throws MqttsnCodecException;
	
	//It returns the tag of nominalTagLength
	abstract public byte[] protect(byte[] associatedData, byte[] plaintextPayload, byte[] key, byte[] encryptedPayload); 
}
