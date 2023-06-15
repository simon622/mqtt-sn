package org.slj.mqtt.sn.protection;

import java.util.Arrays;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.spi.MqttsnSecurityException;
import org.slj.mqtt.sn.wire.version2_0.payload.AbstractAeadProtectionScheme;

public class ProtectionSchemeChaCha20_Poly1305 extends AbstractAeadProtectionScheme
{
    private static final Logger logger = LoggerFactory.getLogger(ProtectionSchemeChaCha20_Poly1305.class);

    protected static final byte nonceLength=12;
	protected short nominalTagLengthInBits;
	
	//https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html
	private static final String CHACHA20_POLY1305_ALGORITHM = "ChaCha20-Poly1305";	
	
	private Cipher cipher=null;

	public ProtectionSchemeChaCha20_Poly1305(String name, byte index)
	{
		this.name=name;
		this.index=index;
		this.blockSizeInBytes=16;
		this.authenticationOnly=false;
		this.nominalTagLengthInBytes=16; //128 bits tag
		this.nominalTagLengthInBits=(short)(this.nominalTagLengthInBytes*8);
		this.allowedKeyLength=32; //256 bits keys allowed in this scheme
       
		try 
		{
			cipher=Cipher.getInstance(CHACHA20_POLY1305_ALGORITHM);
		} 
		catch (Exception e) 
		{
			throw new MqttsnSecurityException(e);
		}
	}
	
	public static void register()	
	{
		protectionSchemeClasses.put(Byte.valueOf(ChaCha20_Poly1305), ProtectionSchemeChaCha20_Poly1305.class);
	}

	public static void unregister()	
	{
		protectionSchemeClasses.remove(Byte.valueOf(ChaCha20_Poly1305));
	}

	public byte[] unprotect(byte[] associatedData, byte[] encryptedPayload, byte[] tagToBeVerified, byte[] key) throws MqttsnSecurityException
	{
		//The associatedData is represented by the sequence of bytes from Byte 1 to Byte R (so all packet fields until the “Encapsulated MQTT-SN Packet” field)
		//The encryptedPayload is represented by the sequence of bytes from Byte S to Byte T
		//The required IV/nonce is calculated from the associatedData as SHA256 truncated to 104 bits
		//It returns the plaintext payload if the authenticity is verified, an exception otherwise
		
		if(key.length!=allowedKeyLength)
		{
			throw new MqttsnSecurityException(this.getClass()+" can't be used with keys of size "+key.length);
		}
	
		IvParameterSpec iv=getIv(associatedData);
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, CHACHA20_POLY1305_ALGORITHM); 
		try {
			cipher=Cipher.getInstance(CHACHA20_POLY1305_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv);
		} catch (Exception e) {
			throw new MqttsnSecurityException(e);
		}
        byte[] encryptedPayloadToBeDecrypted = null;
        if(tagToBeVerified.length>nominalTagLengthInBytes)
        {
        	encryptedPayloadToBeDecrypted = new byte[encryptedPayload.length+nominalTagLengthInBytes];
            System.arraycopy(encryptedPayload, 0, encryptedPayloadToBeDecrypted, 0, encryptedPayload.length);
            System.arraycopy(tagToBeVerified, 0, encryptedPayloadToBeDecrypted, encryptedPayload.length, nominalTagLengthInBytes);
        }
        else
        {
        	encryptedPayloadToBeDecrypted = new byte[encryptedPayload.length+tagToBeVerified.length];
            System.arraycopy(encryptedPayload, 0, encryptedPayloadToBeDecrypted, 0, encryptedPayload.length);
            System.arraycopy(tagToBeVerified, 0, encryptedPayloadToBeDecrypted, encryptedPayload.length, tagToBeVerified.length);
        }
        int expectedOutputSize=encryptedPayload.length;
		int outputSize=cipher.getOutputSize(encryptedPayloadToBeDecrypted.length);
		if(outputSize!=expectedOutputSize)
			throw new MqttsnSecurityException("The size of the ChaCha20-Poly1305 input should be "+expectedOutputSize+" and not "+outputSize);

		byte[] decryptionBuffer = new byte[outputSize];

		try {
			cipher.doFinal(encryptedPayloadToBeDecrypted, 0, encryptedPayloadToBeDecrypted.length, decryptionBuffer, 0);
		} catch (Exception e) {
			throw new MqttsnSecurityException(e);
		}
		byte[] decryptedPayload=new byte[encryptedPayload.length];
        System.arraycopy(decryptionBuffer, 0, decryptedPayload, 0, outputSize);
		logger.debug("Authentication tag in plain text (Encrypt-then-MAC): 0x"+HexFormat.of().formatHex(tagToBeVerified).toUpperCase());

		return decryptedPayload;
	}

	private IvParameterSpec getIv(byte[] associatedData)	
	{
		//IV: 12 bytes as indicated in https://www.rfc-editor.org/rfc/rfc8152#section-10.1 obtained by performing SHA256 truncated to 96 bit of the sequence Byte 1 to Byte R (all packet fields until Encapsulated MQTT-SN Packet)
		byte[] iv=Arrays.copyOfRange(digest.digest(associatedData), 0, nonceLength);
		logger.debug("IV: 0x"+HexFormat.of().formatHex(iv));
		return new IvParameterSpec(iv);
	}
	
	//It returns the encrypted tag of nominalTagLength
	public byte[] protect(byte[] associatedData, byte[] plaintextPayload, byte[] key, byte[] encryptedPayload) throws MqttsnSecurityException
	{
		//The associatedData is represented by the sequence of bytes from Byte 1 to Byte R (so all packet fields until the “Encapsulated MQTT-SN Packet” field)
		//The plaintextPayload is represented by the sequence of bytes in the encapsulated MQTT-SN packet
		//The required IV/nonce is calculated from the associatedData as SHA256 truncated to 104 bits
		//It returns the tag of nominalTagLength. The returned tag is encrypted (Encrypt-then-MAC scheme)
		
		if(key.length!=allowedKeyLength)
		{
			throw new MqttsnSecurityException(this.getClass()+" can't be used with keys of size "+key.length);
		}

		IvParameterSpec iv=getIv(associatedData);
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, CHACHA20_POLY1305_ALGORITHM); 
		try {
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv);
		} catch (Exception e) {
			throw new MqttsnSecurityException(e);
		}
		int expectedOutputSize=plaintextPayload.length+nominalTagLengthInBytes;
		int outputSize=cipher.getOutputSize(plaintextPayload.length);
		if(outputSize!=expectedOutputSize)
			throw new MqttsnSecurityException("The size of the ChaCha20-Poly1305 output should be "+expectedOutputSize+" and not "+outputSize);
		byte[] outputBuffer = new byte[outputSize];
		
		try {
			cipher.doFinal(plaintextPayload, 0, plaintextPayload.length, outputBuffer, 0);
		} catch (Exception e) {
			throw new MqttsnSecurityException(e);
		}
        System.arraycopy(outputBuffer, 0, encryptedPayload, 0, (outputSize-nominalTagLengthInBytes));
		byte[] authenticationTag=Arrays.copyOfRange(outputBuffer, (outputSize-nominalTagLengthInBytes), outputSize);
        logger.debug("Authentication tag in plain text (Encrypt-then-MAC): 0x"+HexFormat.of().formatHex(authenticationTag).toUpperCase());
        return authenticationTag;
	}
}
