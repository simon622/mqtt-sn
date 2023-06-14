package org.slj.mqtt.sn.protection;

public class ProtectionSchemeCmac256 extends AbstractProtectionSchemeCmac
{
	public static void register()
	{
		protectionSchemeClasses.put(Byte.valueOf(CMAC_256), ProtectionSchemeCmac256.class);
	}
	
	public static void unregister()
	{
		protectionSchemeClasses.remove(Byte.valueOf(CMAC_256));
	}

	public ProtectionSchemeCmac256(String name, byte index)
	{
		super(name,index);
		this.allowedKeyLength=32; //256 bits keys allowed in this scheme
	}
}
