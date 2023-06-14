package org.slj.mqtt.sn.protection;

public class ProtectionSchemeGcm_128_192 extends AbstractProtectionSchemeGcm
{
	public static void register()	
	{
		protectionSchemeClasses.put(Byte.valueOf(AES_GCM_128_192), ProtectionSchemeGcm_128_192.class);
	}

	public static void unregister()	
	{
		protectionSchemeClasses.remove(Byte.valueOf(AES_GCM_128_192));
	}

	public ProtectionSchemeGcm_128_192(String name, byte index)
	{
		super(name,index);
		this.allowedKeyLength=24; //192 bits keys allowed in this scheme
	}
}
