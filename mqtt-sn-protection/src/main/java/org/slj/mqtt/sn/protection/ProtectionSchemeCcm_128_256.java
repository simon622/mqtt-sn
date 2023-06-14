package org.slj.mqtt.sn.protection;

public class ProtectionSchemeCcm_128_256 extends AbstractProtectionSchemeCcm
{
	public static void register()	
	{
		protectionSchemeClasses.put(Byte.valueOf(AES_CCM_128_256), ProtectionSchemeCcm_128_256.class);
	}

	public static void unregister()	
	{
		protectionSchemeClasses.remove(Byte.valueOf(AES_CCM_128_256));
	}

	public ProtectionSchemeCcm_128_256(String name, byte index)
	{
		super(name,index);
		this.nominalTagLengthInBytes=16; //128 bits tag
		this.nominalTagLengthInBits=(short)(this.nominalTagLengthInBytes*8);
		this.allowedKeyLength=32; //256 bits keys allowed in this scheme
	}
}
