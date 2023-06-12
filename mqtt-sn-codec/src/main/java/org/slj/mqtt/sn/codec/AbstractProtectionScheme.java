package org.slj.mqtt.sn.codec;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.spi.IProtectionScheme;
import org.slj.mqtt.sn.wire.version2_0.payload.ProtectionPacketFlags;

public abstract class AbstractProtectionScheme implements IProtectionScheme
{
    private static final Logger logger = LoggerFactory.getLogger(AbstractProtectionScheme.class);

    protected static short BYTES_FOR_256_BITS = 32;
    
    public static final byte HMAC_SHA256 = 0x00,
	    HMAC_SHA3_256 = 0x01,
	    CMAC_128 = 0x02,
	    CMAC_192 = 0x03,
	    CMAC_256 = 0x04,
	    AES_CCM_64_128 = 0x40,
	    AES_CCM_64_192 = 0x41,
	    AES_CCM_64_256 = 0x42,
	    AES_CCM_128_128 = 0x43,
	    AES_CCM_128_192 = 0x44,
	    AES_CCM_128_256 = 0x45,
	    AES_GCM_128_128 = 0x46,
	    AES_GCM_192_128 = 0x47,
	    AES_GCM_256_128 = 0x48,
	    ChaCha20_Poly1305 = 0x49,
		RESERVED=(byte)0xFF;

     private static final byte [] ALLOWED_SCHEMES = new byte[]{
		 HMAC_SHA256,
		 HMAC_SHA3_256,
		 CMAC_128,
		 CMAC_192,
		 CMAC_256,
		 AES_CCM_64_128,
		 AES_CCM_64_192,
		 AES_CCM_64_256,
		 AES_CCM_128_128,
		 AES_CCM_128_192,
		 AES_CCM_128_256,
		 AES_GCM_128_128,
		 AES_GCM_192_128,
		 AES_GCM_256_128,
		 ChaCha20_Poly1305
      };

    protected final static HashMap<Byte,Class<?>> protectionSchemeClasses = new HashMap<Byte,Class<?>>();
    
    protected byte index=RESERVED;
	protected String name=null;
	protected short nominalTagLength;
	protected short keyLength;
	protected boolean authenticationOnly;
    protected SecureRandom secureRandom = null;

    public AbstractProtectionScheme() throws MqttsnCodecException
    {
        try 
        {
            secureRandom = SecureRandom.getInstanceStrong();
        }
        catch(Exception e)
        {
        	throw new MqttsnCodecException(e);
        }
    }
    
	public String getName()
	{
		return name;
	}

	public byte getIndex()
	{
		return index;
	}
	
	public short getNominalTagLength()
	{
		return nominalTagLength;
	}
	
	public short getKeyLength()
	{
		return keyLength;
	}
	
	public boolean isAuthenticationOnly()
	{
		return authenticationOnly;
	}

	public static IProtectionScheme getProtectionScheme(byte protectionSchemeIndex) throws MqttsnCodecException
	{
		Field[] fields = AbstractProtectionScheme.class.getDeclaredFields();
		for (Field field : fields) {
		    if(field.getType() == byte.class)
			{
			    try
			    {
			    	String fieldName=field.getName();
			    	byte fieldValue=field.getByte(fieldName);
			    	if(fieldValue==protectionSchemeIndex)
			    	{
				    	StringBuilder sb = new StringBuilder("Protection Scheme: ");
						if(Arrays.binarySearch(ALLOWED_SCHEMES, protectionSchemeIndex) == -1){
					    	sb.append("0x").append(String.format("%02x", protectionSchemeIndex&0xff).toUpperCase());
					    	throw new MqttsnCodecException("Invalid "+sb.toString());
			            }
				    	sb.append("0x").append(String.format("%02x", protectionSchemeIndex&0xff).toUpperCase());
						logger.info(sb.toString());
						Class<?>[] constructorParameters=new Class<?>[2];
						constructorParameters[0]=String.class;
						constructorParameters[1]=byte.class;
						return (IProtectionScheme) (protectionSchemeClasses.get(protectionSchemeIndex).getDeclaredConstructor(constructorParameters).newInstance(fieldName,protectionSchemeIndex));
			    	}
			    }
			    catch(Exception ex)
			    {
			    	throw new MqttsnCodecException(ex);
			    }
			}
		}
		return null;
	}
	
    @Override
    public String toString()
	{
    	StringBuilder sb = new StringBuilder("Protection Scheme: ");
    	sb.append(name).
    	append(" (0x").
    	append(String.format("%02x", index&0xff).toUpperCase()).append(")");
    	return sb.toString();
    }
    
	public byte[] getCryptoMaterial(byte cryptoMaterialLength)
	{
        byte[] cryptoMaterial=null; 
        switch(cryptoMaterialLength)
        {
        	case ProtectionPacketFlags.SHORT_CRYPTO_MATERIAL:
        		//Add here the required short crypto material
                cryptoMaterial=new byte[ProtectionPacketFlags.SHORT_CRYPTO_MATERIAL]; //THIS IS ONLY AN EXAMPLE. IMPLEMENTATION DEPENDENT
                secureRandom.nextBytes(cryptoMaterial);
        		break;
        	case ProtectionPacketFlags.LONG_CRYPTO_MATERIAL:
        		//Add here the required long crypto material
                cryptoMaterial=new byte[ProtectionPacketFlags.LONG_CRYPTO_MATERIAL]; //THIS IS ONLY AN EXAMPLE. IMPLEMENTATION DEPENDENT 
                secureRandom.nextBytes(cryptoMaterial);
            	break;
        	case ProtectionPacketFlags.VERYLONG_CRYPTO_MATERIAL:
        		//Add here the required long crypto material
                cryptoMaterial=new byte[ProtectionPacketFlags.VERYLONG_CRYPTO_MATERIAL]; //THIS IS ONLY AN EXAMPLE. IMPLEMENTATION DEPENDENT 
                secureRandom.nextBytes(cryptoMaterial);
            	break;
        	default:
        		//No crypto material
        }
        return cryptoMaterial;
	}
}
