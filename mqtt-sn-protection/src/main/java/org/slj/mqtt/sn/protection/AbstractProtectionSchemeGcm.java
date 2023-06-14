package org.slj.mqtt.sn.protection;

import java.security.Security;
import java.util.Arrays;
import java.util.HexFormat;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.spi.MqttsnSecurityException;
import org.slj.mqtt.sn.wire.version2_0.payload.AbstractAeadProtectionScheme;

public abstract class AbstractProtectionSchemeGcm extends AbstractAeadProtectionScheme
{
    private static final Logger logger = LoggerFactory.getLogger(AbstractProtectionSchemeGcm.class);

    protected static final byte nonceLength=12;
	protected short nominalTagLengthInBits;
	
	private GCMBlockCipher gcmBlockCipher=null;

	public AbstractProtectionSchemeGcm(String name, byte index)
	{
		this.name=name;
		this.index=index;
		this.blockSizeInBytes=16;
		this.authenticationOnly=false;
		this.nominalTagLengthInBytes=16; //128 bits tag
		this.nominalTagLengthInBits=(short)(this.nominalTagLengthInBytes*8);
        Security.addProvider(new BouncyCastleProvider());

		try 
		{
			gcmBlockCipher= new GCMBlockCipher(new AESEngine());
		} 
		catch (Exception e) 
		{
			throw new MqttsnSecurityException(e);
		}
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
	
		AEADParameters aeadParameters=getAEADParameters(associatedData,key);
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
        gcmBlockCipher.reset();
        gcmBlockCipher.init(false,aeadParameters);
		int expectedOutputSize=encryptedPayload.length;
		int outputSize=gcmBlockCipher.getOutputSize(encryptedPayloadToBeDecrypted.length);
		if(outputSize!=expectedOutputSize)
			throw new MqttsnSecurityException("The size of the GCM input should be "+expectedOutputSize+" and not "+outputSize);

		byte[] decryptionBuffer = new byte[outputSize];

		int bytesWritten = gcmBlockCipher.processBytes(encryptedPayloadToBeDecrypted, 0, encryptedPayloadToBeDecrypted.length, decryptionBuffer, 0);
		try {
			bytesWritten = gcmBlockCipher.doFinal(decryptionBuffer, bytesWritten);
		} catch (Exception e) {
			throw new MqttsnSecurityException(e);
		}
		byte[] decryptedPayload=new byte[encryptedPayload.length];
        System.arraycopy(decryptionBuffer, 0, decryptedPayload, 0, outputSize);
		logger.debug("Mac in plain text: 0x"+HexFormat.of().formatHex(gcmBlockCipher.getMac()).toUpperCase());
		return decryptedPayload;
	}

	private AEADParameters getAEADParameters(byte[] associatedData, byte[] key)	
	{
		//Nonce: 12 bytes as indicated in https://www.rfc-editor.org/rfc/rfc8152#section-10.1 obtained by performing SHA256 truncated to 96 bit of the sequence Byte 1 to Byte R (all packet fields until Encapsulated MQTT-SN Packet)
		byte[] nonce=Arrays.copyOfRange(digest.digest(associatedData), 0, nonceLength);
		logger.debug("Nonce: 0x"+HexFormat.of().formatHex(nonce));
		return new AEADParameters(new KeyParameter(key),nominalTagLengthInBits,nonce,associatedData);
	}
	
	//It returns the encrypted tag of nominalTagLength
	public byte[] protect(byte[] associatedData, byte[] plaintextPayload, byte[] key, byte[] encryptedPayload) throws MqttsnSecurityException
	{
		//The associatedData is represented by the sequence of bytes from Byte 1 to Byte R (so all packet fields until the “Encapsulated MQTT-SN Packet” field)
		//The plaintextPayload is represented by the sequence of bytes in the encapsulated MQTT-SN packet
		//The required IV/nonce is calculated from the associatedData as SHA256 truncated to 104 bits
		//It returns the tag of nominalTagLength. The returned tag is encrypted (authenticate-then-encrypt scheme)
		
		if(key.length!=allowedKeyLength)
		{
			throw new MqttsnSecurityException(this.getClass()+" can't be used with keys of size "+key.length);
		}

		AEADParameters aeadParameters=getAEADParameters(associatedData,key);
		gcmBlockCipher.reset();
		gcmBlockCipher.init(true,aeadParameters);
		int expectedOutputSize=plaintextPayload.length+nominalTagLengthInBytes;
		int outputSize=gcmBlockCipher.getOutputSize(plaintextPayload.length);
		if(outputSize!=expectedOutputSize)
			throw new MqttsnSecurityException("The size of the GCM output should be "+expectedOutputSize+" and not "+outputSize);
		byte[] outputBuffer = new byte[outputSize];
		
		int bytesWritten = gcmBlockCipher.processBytes(plaintextPayload, 0, plaintextPayload.length, outputBuffer, 0);
		try {
			bytesWritten = gcmBlockCipher.doFinal(outputBuffer, bytesWritten);
		} catch (Exception e) {
			throw new MqttsnSecurityException(e);
		}
        System.arraycopy(outputBuffer, 0, encryptedPayload, 0, (outputSize-nominalTagLengthInBytes));
		byte[] authenticationTag=Arrays.copyOfRange(outputBuffer, (outputSize-nominalTagLengthInBytes), outputSize);
        logger.debug("Mac in plain text: 0x"+HexFormat.of().formatHex(gcmBlockCipher.getMac()).toUpperCase());
        
        return authenticationTag;
	}
}
