package org.slj.mqtt.sn.protection.alg;

public class ProtectionSchemeCmac128 extends AbstractProtectionSchemeCmac
{
	public static void register()
	{
		protectionSchemeClasses.put(Byte.valueOf(CMAC_128), ProtectionSchemeCmac128.class);
	}
	
	public static void unregister()
	{
		protectionSchemeClasses.remove(Byte.valueOf(CMAC_128));
	}

	public ProtectionSchemeCmac128(String name, byte index)
	{
		super(name,index);
		this.allowedKeyLength=16; //128 bits keys allowed in this scheme
	}
}
