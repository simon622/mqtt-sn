package org.slj.mqtt.sn.protection;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.codec.AbstractProtectionScheme;
import org.slj.mqtt.sn.impl.MqttsnSecurityService;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.IProtectionScheme;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnSecurityException;
import org.slj.mqtt.sn.utils.Security;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.version2_0.payload.MqttsnProtection;
import org.slj.mqtt.sn.wire.version2_0.payload.ProtectionPacketFlags;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Davide - super simple bit of DEMO code to get the messages secured 2 ways
 *
 * Question - should the network address be the source of truth...
 * Ie lookup based on sending address... obtain the sendingHash & clientId and verify?
 *
 * @author Simon L Johnson
 * @author Davide Lenzarini
 */
public class MqttsnProtectionService extends MqttsnSecurityService  {

    private static final String COUNTER_CONTEXT_KEY = "monotonicCounter";

    //-- The hash algo to use on the senderId
    private static final String HASH_ALG = "SHA-256";

    //-- The prefix size used for senderId lookups
    private static final int SENDER_PREFIX_LEN = 8;
    
    private Map<ByteBuffer, String> sendersWhitelist = new ConcurrentHashMap<>();

    private SecureRandom secureRandom = null;
    private ProtectionPacketFlags flags = null;
    private IProtectionScheme protectionScheme=null; 
    private byte[] protectionKey=null;
    private String protectionKeyHash=null;
	private MessageDigest digest=null;
	private boolean isGateway=false;

	public MqttsnProtectionService(boolean isGateway)
	{
		super();
		this.isGateway=isGateway;
	}
	
	public void setProtectionKey(byte[] protectionKey)
	{
        this.protectionKey = protectionKey;
        protectionKeyHash = HexFormat.of().formatHex(digest.digest(protectionKey));
	}

	public void setProtectionFlags(byte[] flags)
	{
        this.flags = new ProtectionPacketFlags(flags[0],flags[1],flags[2]);
	}

	public void setAllowedClients(String[] allowedClients)
	{
		Arrays.stream(allowedClients).forEach(allowedClient -> addAllowedClientId(allowedClient)); //Whitelist of client senders (Client clientId)
	}

	public void setProtectionScheme(byte protectionSchemeIndex)
	{
		this.protectionScheme=AbstractProtectionScheme.getProtectionScheme(protectionSchemeIndex);
	}

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        try {
            secureRandom = SecureRandom.getInstanceStrong();
        	digest = MessageDigest.getInstance("SHA-256");
        }
        catch(Exception e)
        {
        	throw new MqttsnException(e);
        }

        ProtectionSchemeHmacSha256.register();
        ProtectionSchemeCcm_64_128.register();
        
        //*** TODO PP: to be retrieved from a configuration file ***//
        byte[] protectionKeyHmac = HexFormat.of().parseHex("8d8c0e211361005215e902cdfa4b1e0b9d25e497ea71d75439224e55804aea2e7a9c975316d427cc6e00dbe5c2e389127a9c975316d427cc6e00dbe5c2e38912");
        	//256 bits Ox8d8c0e211361005215e902cdfa4b1e0b9d25e497ea71d75439224e55804aea2e
        	//512 bits Ox8d8c0e211361005215e902cdfa4b1e0b9d25e497ea71d75439224e55804aea2e7a9c975316d427cc6e00dbe5c2e389127a9c975316d427cc6e00dbe5c2e38912
    	
        if(isGateway)
        {
        	setProtectionKey(protectionKeyHmac); //TODO PP to be removed as client dependent
        	setAllowedClients(new String[] {"protectionClient"});
	        //The protectionScheme to be used is defined by each client
	        //The flags to be used are defined by each client
        }
        else
        {
			setProtectionKey(protectionKeyHmac);
        	setAllowedClients(new String[] {"protectionGateway"});
        	setProtectionScheme(AbstractProtectionScheme.HMAC_SHA256);
			//64 bits of authentication tag, no crypto material, no monotonic counter
        	setProtectionFlags(new byte[] {(byte)0x03,(byte)0x00,(byte)0x00});
        }
        logger.debug(getProtectionConfiguration());
        //*** END TODO ***//
    }

    @Override
    public byte[] writeVerified(INetworkContext networkContext, byte[] data) throws MqttsnSecurityException {
    	//data is the message to be encapsulated
    	String clientId=registry.getOptions().getContextId();
        logger.info("Protection service handling {} egress bytes 0x{} from {} for {}", data.length, MqttsnWireUtils.toHex(data), clientId, networkContext);
        
        ByteBuffer senderId = deriveSenderId(clientId);

        byte[] random = BigInteger.valueOf(secureRandom.nextInt()).toByteArray();
        byte[] cryptoMaterial=null; 
        switch(flags.getCryptoMaterialLengthDecoded())
        {
        	case ProtectionPacketFlags.SHORT_CRYPTO_MATERIAL:
        		cryptoMaterial=BigInteger.valueOf(Short.MIN_VALUE).toByteArray();
        		//Add here the required short crypto material
        		break;
        	case ProtectionPacketFlags.LONG_CRYPTO_MATERIAL:
        		cryptoMaterial=BigInteger.valueOf(Integer.MIN_VALUE).toByteArray();
            	//Add here the required long crypto material
        		break;
        	default:
        }
        int monotonicCounter = 0;
        switch(flags.getMonotonicCounterLengthDecoded())
        {
        	case ProtectionPacketFlags.SHORT_MONOTONIC_COUNTER:
                monotonicCounter = nextMonotonicCounterValue(networkContext,(int)Short.MIN_VALUE,true);
        		break;
        	case ProtectionPacketFlags.LONG_MONOTONIC_COUNTER:
                monotonicCounter = nextMonotonicCounterValue(networkContext,Integer.MIN_VALUE,false);
        		break;
        	default:
        }
        MqttsnProtection packet = (MqttsnProtection) getRegistry().getCodec().createMessageFactory().
                createProtectionMessage(protectionScheme, protectionKey, flags, senderId.array(), random, cryptoMaterial, monotonicCounter, data);

        byte[] protectionPacket = packet.encode();
        logger.debug(packet.toString());
        return protectionPacket;
        //return data; //to return the original data
    }

    @Override
    public byte[] readVerified(INetworkContext networkContext, byte[] data) throws MqttsnSecurityException {
    	//data is the message to be decapsulated
        logger.info("Protection service handling {} ingress bytes 0x{} for {}", data.length, MqttsnWireUtils.toHex(data), networkContext);
        
        //TODO PP store in the context the flags and protection scheme of the client to be reused for all communications from now on
        
        if(isSecurityEnvelope(data)){
            logger.info("Protection packet identified");
            MqttsnProtection packet = (MqttsnProtection) getRegistry().getCodec().decode(data);
           
            String senderClientId=sendersWhitelist.get(ByteBuffer.wrap(packet.getSenderId()));
            if(senderClientId!=null)
            {
                //logger.info("Protection packet received from {}: {}",senderClientId,getProtectionPacketAsLogString(packet));
                //TODO This is where we need to verify the packet
                //return data;
            }
            throw new MqttsnSecurityException("Unauthorized senderId: "+HexFormat.of().formatHex(packet.getSenderId()));
        }
        else {
            return super.readVerified(networkContext, data);
        }
    }

    private boolean isSecurityEnvelope(byte[] data){
        if(getRegistry().getCodec().supportsVersion(MqttsnConstants.PROTOCOL_VERSION_2_0)){
            int msgType = MqttsnWireUtils.readMessageType(data);
            return MqttsnConstants.PROTECTION == msgType;
        }
        return false;
    }

    private Integer nextMonotonicCounterValue(INetworkContext networkContext, int intialValue, boolean shortVersion) throws MqttsnSecurityException
    {
        IClientIdentifierContext clientIdentifierContext =
                getRegistry().getNetworkRegistry().getMqttsnContext(networkContext);
        if(clientIdentifierContext != null){
            //this must be an anonymous message OR connect.. so we need to handle this out of band
            AtomicInteger counter = (AtomicInteger) clientIdentifierContext.getContextObject(COUNTER_CONTEXT_KEY);
            if(counter == null){
                counter = new AtomicInteger(intialValue);
                clientIdentifierContext.putContextObject(COUNTER_CONTEXT_KEY, counter);
            }
            int newValue=counter.incrementAndGet();
            if(shortVersion && newValue==(Short.MAX_VALUE+1))
            	newValue=Short.MIN_VALUE;
            return newValue;
        }
        throw new MqttsnSecurityException("Unable to create a monotonic counter!");
    }

    private void addAllowedClientId(final String clientId) {
        try {
            sendersWhitelist.put(deriveSenderId(clientId), clientId);
        }
    	catch(Exception e){
    		throw new MqttsnSecurityException(e);
    	}
    }

    private void removeAllowedClientId(final String clientId) {
        try {
            sendersWhitelist.remove(deriveSenderId(clientId));
        }
    	catch(Exception e){
    		throw new MqttsnSecurityException(e);
    	}
    }

    //-- Bootstrap into the runtime for protocol packets
    public boolean protocolIntegrityEnabled(){
        return true;
    }

    private ByteBuffer deriveSenderId(String clientId) throws MqttsnSecurityException
    {
    	try
    	{
	    	if(clientId!=null && clientId.length()>0)
	    	{
	    		return ByteBuffer.wrap(Arrays.copyOfRange(Security.hash(clientId.getBytes(), HASH_ALG), 0, SENDER_PREFIX_LEN));
	    	}
    	}
    	catch(NoSuchAlgorithmException e)
    	{
        	throw new MqttsnSecurityException(e);
    	}
    	throw new MqttsnSecurityException("Unable to generate the SenderId: ClientId not available!");
    }
    
    private String getProtectionConfiguration()
    {
        StringBuilder sb = new StringBuilder("Protection configuration:");
        sb.append("\n\tWhitelist:");
        sendersWhitelist.forEach((key, value) -> sb.append("\n\t\t0x").append(HexFormat.of().formatHex(key.array())).append("-").append(value));
        sb.append("\n\tProtectionKey hash: 0x").append(protectionKeyHash);
        if(!isGateway)
        {
	        sb.append("\n\t").append(protectionScheme.toString());
	        sb.append("\n\t").append(flags.toString());
        }
        return sb.toString();
    }
}
