package org.slj.mqtt.sn.protection;

public class ProtectionSchemeCcm_64_192 extends AbstractProtectionSchemeCcm
{
	public static void register()	
	{
		protectionSchemeClasses.put(Byte.valueOf(AES_CCM_64_192), ProtectionSchemeCcm_64_192.class);
	}

	public static void unregister()	
	{
		protectionSchemeClasses.remove(Byte.valueOf(AES_CCM_64_192));
	}

	public ProtectionSchemeCcm_64_192(String name, byte index)
	{
		super(name,index);
		this.nominalTagLengthInBytes=8; //64 bits tag
		this.nominalTagLengthInBits=(short)(this.nominalTagLengthInBytes*8);
		this.allowedKeyLength=24; //192 bits keys allowed in this scheme
	}
}
