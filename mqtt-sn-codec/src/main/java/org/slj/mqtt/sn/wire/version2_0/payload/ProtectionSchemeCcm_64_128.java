package org.slj.mqtt.sn.wire.version2_0.payload;

import org.slj.mqtt.sn.codec.MqttsnCodecException;

public class ProtectionSchemeCcm_64_128 extends AbstractAeadProtectionScheme
{
	protected static byte nonceLength=13;
	
	public ProtectionSchemeCcm_64_128(String name, byte index)
	{
		this.name=name;
		this.index=index;
		nominalTagLength=BYTES_FOR_256_BITS;
		keyLength=BYTES_FOR_256_BITS;
		authenticationOnly=true;
	}
	
	public byte[] unprotect(byte[] associatedData, byte[] encryptedPayload, byte[] tagToBeVerified, byte[] key) throws MqttsnCodecException
	{
		//The associatedData is represented by the sequence of bytes from Byte 1 to Byte R (so all packet fields until the “Encapsulated MQTT-SN Packet” field)
		//The encryptedPayload is represented by the sequence of bytes from Byte S to Byte T
		//The required IV/nonce is calculated from the associatedData as SHA256 truncated to 104 bits
		//It returns the plaintext payload if the authenticity is verified, an exception otherwise
		return null;
	}
	
	//It returns the tag of nominalTagLength
	public byte[] protect(byte[] associatedData, byte[] plaintextPayload, byte[] key, byte[] encryptedPayload)
	{
		//The associatedData is represented by the sequence of bytes from Byte 1 to Byte R (so all packet fields until the “Encapsulated MQTT-SN Packet” field)
		//The plaintextPayload is represented by the sequence of bytes in the encapsulated MQTT-SN packet
		//The required IV/nonce is calculated from the associatedData as SHA256 truncated to 104 bits
		//It returns the tag of nominalTagLength. The returned tag is encrypted (authenticate-then-encrypt scheme)
		return null;
	}
}
