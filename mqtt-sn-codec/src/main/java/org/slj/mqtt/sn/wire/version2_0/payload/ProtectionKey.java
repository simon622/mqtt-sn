package org.slj.mqtt.sn.wire.version2_0.payload;

import java.security.MessageDigest;

import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

public class ProtectionKey
{
	public String protectionKeyHash;
	private byte[] protectionKey;
	private MessageDigest digest;
	
	public ProtectionKey(byte[] protectionKey) throws MqttsnCodecException
	{
		try 
		{
			digest = MessageDigest.getInstance("SHA-256");
        }
        catch(Exception e)
        {
        	throw new MqttsnCodecException(e);
        }
		this.protectionKey = protectionKey;
		this.protectionKeyHash = MqttsnWireUtils.toHex(digest.digest(protectionKey));
	}
	
	public byte[] getProtectionKey()
	{
		return protectionKey;
	}
}