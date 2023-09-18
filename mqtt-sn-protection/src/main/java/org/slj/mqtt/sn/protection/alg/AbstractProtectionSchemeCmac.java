package org.slj.mqtt.sn.protection.alg;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slj.mqtt.sn.spi.MqttsnSecurityException;
import org.slj.mqtt.sn.wire.version2_0.payload.AbstractAuthenticationOnlyProtectionScheme;

import java.util.Arrays;
import java.security.Security;

public class AbstractProtectionSchemeCmac extends AbstractAuthenticationOnlyProtectionScheme
{
	private CMac cmac =null;

	public AbstractProtectionSchemeCmac(String name, byte index)
	{
		this.name=name;
		this.index=index;
		nominalTagLengthInBytes=16;
		blockSizeInBytes=16;
		authenticationOnly=true;
        Security.addProvider(new BouncyCastleProvider());

		try 
		{
			cmac= new CMac(new AESEngine());
		} 
		catch (Exception e) 
		{
			throw new MqttsnSecurityException(e);
		}
		
		int cmacMacSize=cmac.getMacSize();
		if(cmacMacSize!=nominalTagLengthInBytes)
			throw new MqttsnSecurityException("CMAC tag length different from nominal! "+cmacMacSize+" instead of "+nominalTagLengthInBytes);
	}
	
	public byte[] unprotect(byte[] authenticatedPayload, byte[] tagToBeVerified, byte[] key) throws MqttsnSecurityException
	{
		//The authenticatedPayload is represented by the sequence of bytes from Byte 1 to Byte T
		//If the tagToBeVerified is truncated, the comparison will be done after truncating the calculated tag at the same level (from the most significant bits first order)
		//It returns the authenticatedPayload if the authenticity is verified, an exception otherwise

		if(key.length!=allowedKeyLength)
		{
			throw new MqttsnSecurityException(this.getClass()+" can't be used with keys of size "+key.length);
		}

		cmac.init(new KeyParameter(key));
		cmac.update(authenticatedPayload, 0, authenticatedPayload.length);
		byte[] tag = new byte[nominalTagLengthInBytes];
		Arrays.fill(tag, (byte) 0x00);
		cmac.doFinal(tag, 0);

	    if(tagToBeVerified.length!=nominalTagLengthInBytes)
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
		
		if(key.length!=allowedKeyLength)
		{
			throw new MqttsnSecurityException(this.getClass()+" can't be used with keys of size "+key.length);
		}

		cmac.init(new KeyParameter(key));
		cmac.update(payloadToBeAuthenticated, 0, payloadToBeAuthenticated.length);
		byte[] tag = new byte[nominalTagLengthInBytes];
		Arrays.fill(tag, (byte) 0x00);
		cmac.doFinal(tag, 0);
		return tag;
	}
}
