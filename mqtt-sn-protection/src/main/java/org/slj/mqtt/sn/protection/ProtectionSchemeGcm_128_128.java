package org.slj.mqtt.sn.protection;

public class ProtectionSchemeGcm_128_128 extends AbstractProtectionSchemeGcm
{
	public static void register()	
	{
		protectionSchemeClasses.put(Byte.valueOf(AES_GCM_128_128), ProtectionSchemeGcm_128_128.class);
	}

	public static void unregister()	
	{
		protectionSchemeClasses.remove(Byte.valueOf(AES_GCM_128_128));
	}

	public ProtectionSchemeGcm_128_128(String name, byte index)
	{
		super(name,index);
		this.allowedKeyLength=16; //128 bits keys allowed in this scheme
	}
}
