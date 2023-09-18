package org.slj.mqtt.sn.protection.alg;

public class ProtectionSchemeCcm_128_192 extends AbstractProtectionSchemeCcm
{
	public static void register()	
	{
		protectionSchemeClasses.put(Byte.valueOf(AES_CCM_128_192), ProtectionSchemeCcm_128_192.class);
	}

	public static void unregister()	
	{
		protectionSchemeClasses.remove(Byte.valueOf(AES_CCM_128_192));
	}

	public ProtectionSchemeCcm_128_192(String name, byte index)
	{
		super(name,index);
		this.nominalTagLengthInBytes=16; //128 bits tag
		this.nominalTagLengthInBits=(short)(this.nominalTagLengthInBytes*8);
		this.allowedKeyLength=24; //192 bits keys allowed in this scheme
	}
}
