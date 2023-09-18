package org.slj.mqtt.sn.protection.alg;

public class ProtectionSchemeCcm_64_128 extends AbstractProtectionSchemeCcm
{
	public static void register()	
	{
		protectionSchemeClasses.put(Byte.valueOf(AES_CCM_64_128), ProtectionSchemeCcm_64_128.class);
	}

	public static void unregister()	
	{
		protectionSchemeClasses.remove(Byte.valueOf(AES_CCM_64_128));
	}

	public ProtectionSchemeCcm_64_128(String name, byte index)
	{
		super(name,index);
		this.nominalTagLengthInBytes=8; //64 bits tag
		this.nominalTagLengthInBits=(short)(this.nominalTagLengthInBytes*8);
		this.allowedKeyLength=16; //128 bits keys allowed in this scheme
	}
}
