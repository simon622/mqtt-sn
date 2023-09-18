package org.slj.mqtt.sn.protection.alg;

public class ProtectionSchemeCcm_64_256 extends AbstractProtectionSchemeCcm
{
	public static void register()	
	{
		protectionSchemeClasses.put(Byte.valueOf(AES_CCM_64_256), ProtectionSchemeCcm_64_256.class);
	}

	public static void unregister()	
	{
		protectionSchemeClasses.remove(Byte.valueOf(AES_CCM_64_256));
	}

	public ProtectionSchemeCcm_64_256(String name, byte index)
	{
		super(name,index);
		this.nominalTagLengthInBytes=8; //64 bits tag
		this.nominalTagLengthInBits=(short)(this.nominalTagLengthInBytes*8);
		this.allowedKeyLength=32; //256 bits keys allowed in this scheme
	}
}
