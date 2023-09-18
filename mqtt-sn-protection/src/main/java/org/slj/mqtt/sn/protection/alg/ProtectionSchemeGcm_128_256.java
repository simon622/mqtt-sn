package org.slj.mqtt.sn.protection.alg;

public class ProtectionSchemeGcm_128_256 extends AbstractProtectionSchemeGcm
{
	public static void register()	
	{
		protectionSchemeClasses.put(Byte.valueOf(AES_GCM_128_256), ProtectionSchemeGcm_128_256.class);
	}

	public static void unregister()	
	{
		protectionSchemeClasses.remove(Byte.valueOf(AES_GCM_128_256));
	}

	public ProtectionSchemeGcm_128_256(String name, byte index)
	{
		super(name,index);
		this.allowedKeyLength=32; //256 bits keys allowed in this scheme
	}
}
