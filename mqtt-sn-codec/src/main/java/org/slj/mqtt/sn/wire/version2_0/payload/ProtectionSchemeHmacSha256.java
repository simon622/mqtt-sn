package org.slj.mqtt.sn.wire.version2_0.payload;

import org.slj.mqtt.sn.codec.MqttsnCodecException;

public class ProtectionSchemeHmacSha256 extends AbstractAuthenticationOnlyProtectionScheme
{
	public ProtectionSchemeHmacSha256(String name, byte index)
	{
		this.name=name;
		this.index=index;
		nominalTagLength=BYTES_FOR_256_BITS;
		keyLength=BYTES_FOR_256_BITS;
		authenticationOnly=true;
	}
	
	public byte[] unprotect(byte[] authenticatedPayload, byte[] tagToBeVerified, byte[] key) throws MqttsnCodecException
	{
		//The authenticatedPayload is represented by the sequence of bytes from Byte 1 to Byte T
		//If the tagToBeVerified is truncated, the comparison will be done after truncating the calculated tag at the same level (from the most significant bits first order)
		//It returns the authenticatedPayload if the authenticity is verified, an exception otherwise
		return null;
	}
	
	public byte[] protect(byte[] authenticatedPayload, byte[] key) 
	{
		//The authenticatedPayload is represented by the sequence of bytes from Byte 1 to Byte T
		//It returns the tag of nominalTagLength
		return null;
	}
}
