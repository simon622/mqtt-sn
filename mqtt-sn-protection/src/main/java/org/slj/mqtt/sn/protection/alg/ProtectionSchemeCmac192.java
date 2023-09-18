package org.slj.mqtt.sn.protection.alg;

public class ProtectionSchemeCmac192 extends AbstractProtectionSchemeCmac
{
	public static void register()
	{
		protectionSchemeClasses.put(Byte.valueOf(CMAC_192), ProtectionSchemeCmac192.class);
	}
	
	public static void unregister()
	{
		protectionSchemeClasses.remove(Byte.valueOf(CMAC_192));
	}

	public ProtectionSchemeCmac192(String name, byte index)
	{
		super(name,index);
		this.allowedKeyLength=24; //192 bits keys allowed in this scheme
	}
}
