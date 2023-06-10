package org.slj.mqtt.sn.protection;

import org.slj.mqtt.sn.spi.MqttsnSecurityException;
import org.slj.mqtt.sn.wire.version2_0.payload.AbstractAuthenticationOnlyProtectionScheme;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

public class ProtectionSchemeHmacSha256 extends AbstractAuthenticationOnlyProtectionScheme
{
	//https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#mac-algorithms
	private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

	private Mac mac;

	public static void register()
	{
		protectionSchemeClasses.put(Byte.valueOf(HMAC_SHA256), ProtectionSchemeHmacSha256.class);
	}

	public static void unregister()
	{
		protectionSchemeClasses.remove(Byte.valueOf(HMAC_SHA256));
	}

	public ProtectionSchemeHmacSha256(String name, byte index)
	{
		this.name=name;
		this.index=index;
		nominalTagLength=BYTES_FOR_256_BITS;
		keyLength=BYTES_FOR_256_BITS;
		authenticationOnly=true;
		try 
		{
			mac= Mac.getInstance(HMAC_SHA256_ALGORITHM);
		} 
		catch (NoSuchAlgorithmException e) 
		{
			throw new MqttsnSecurityException(e);
		}
	}
	
	private void macSetup(SecretKeySpec secretKeySpec) throws MqttsnSecurityException
	{
		try 
		{
			mac.reset();
			mac.init(secretKeySpec);
		} 
		catch (InvalidKeyException e) 
		{
			throw new MqttsnSecurityException(e);
		}
	}
	
	public byte[] unprotect(byte[] authenticatedPayload, byte[] tagToBeVerified, byte[] key) throws MqttsnSecurityException
	{
		//The authenticatedPayload is represented by the sequence of bytes from Byte 1 to Byte T
		//If the tagToBeVerified is truncated, the comparison will be done after truncating the calculated tag at the same level (from the most significant bits first order)
		//It returns the authenticatedPayload if the authenticity is verified, an exception otherwise
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, HMAC_SHA256_ALGORITHM); 
		macSetup(secretKeySpec);
	    byte[] tag=mac.doFinal(authenticatedPayload);
	    if(tagToBeVerified.length!=nominalTagLength)
	    {
	    	//Truncated tag
	    	tag = Arrays.copyOfRange(tag, 0, tagToBeVerified.length);
	    }
	    if(Arrays.equals(tag, tagToBeVerified))
	    {
	    	return authenticatedPayload;
	    }
		throw new MqttsnSecurityException("Authentication Tag not matching");
	}
	
	public byte[] protect(byte[] payloadToBeAuthenticated, byte[] key) throws MqttsnSecurityException
	{
		//The authenticatedPayload is represented by the sequence of bytes from Byte 1 to Byte T
		//It returns the tag of nominalTagLength
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, HMAC_SHA256_ALGORITHM); 
		try 
		{
			mac.reset();
			mac.init(secretKeySpec);
		} 
		catch (InvalidKeyException e) 
		{
			throw new MqttsnSecurityException(e);
		}
	    return mac.doFinal(payloadToBeAuthenticated);
	}
}
