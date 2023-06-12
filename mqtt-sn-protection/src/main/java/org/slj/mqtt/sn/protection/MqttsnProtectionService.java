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
import org.slj.mqtt.sn.wire.version2_0.payload.ProtectionKey;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
    
    private class Sender
	{
		String clientId; 
		private ArrayList<ProtectionKey> protectionKeys=new ArrayList<ProtectionKey>();
		
		Sender(String clientId, ArrayList<byte[]> protectionKeys)
		{
			this.clientId=clientId;
			protectionKeys.forEach(protectionKey -> this.protectionKeys.add(new ProtectionKey(protectionKey)));
		}
		
		public String toString()
		{
			StringBuilder sb=new StringBuilder("Sender: ");
			sb.append(clientId).append(",[");
			protectionKeys.forEach(protectionKey -> sb.append(protectionKey.protectionKeyHash));
			sb.append("]");
			return sb.toString();
		}
	}
	
    private Map<ByteBuffer, Sender> sendersWhitelist = new ConcurrentHashMap<>();

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

	public void setAllowedClients(Sender[] allowedClients)
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
        	digest = MessageDigest.getInstance("SHA-256");
        }
        catch(Exception e)
        {
        	throw new MqttsnException(e);
        }

        ProtectionSchemeHmacSha256.register();
        ProtectionSchemeCcm_64_128.register();
        
        //*** TODO PP: to be retrieved from a configuration file ***//
        byte[] gatewayProtectionKeyHmac = HexFormat.of().parseHex("112233211361005215e902cdfa4b1e0b9d25e497ea71d75439224e55804aea2e7a9c975316d427cc6e00dbe5c2e389127a9c975316d427cc6e00dbe5c2e38912");
        byte[] clientProtectionKeyHmac = HexFormat.of().parseHex("8d8c0e211361005215e902cdfa4b1e0b9d25e497ea71d75439224e55804aea2e7a9c975316d427cc6e00dbe5c2e389127a9c975316d427cc6e00dbe5c2e38912");
        if(isGateway)
        {
			setProtectionKey(gatewayProtectionKeyHmac);
        	setAllowedClients(new Sender[] {new Sender("protectionClient",new ArrayList<byte[]>(Arrays.asList(clientProtectionKeyHmac)))});
	        //The protectionScheme to be used is defined by each client
	        //The flags to be used are defined by each client
        }
        else
        {
			setProtectionKey(clientProtectionKeyHmac);
        	setAllowedClients(new Sender[] {new Sender("protectionGateway",new ArrayList<byte[]>(Arrays.asList(gatewayProtectionKeyHmac)))});
        	setProtectionScheme(AbstractProtectionScheme.HMAC_SHA256);
			//64 bits of authentication tag, no crypto material, no monotonic counter
        	setProtectionFlags(new byte[] {(byte)0x03,(byte)0x00,(byte)0x00});
        }
        logger.debug(getProtectionConfiguration());
        //*** END TODO ***//
    }

    @Override
    public byte[] writeVerified(INetworkContext networkContext, byte[] encapsulatedPacket) throws MqttsnSecurityException {
    	//data is the message to be encapsulated
    	String clientId=registry.getOptions().getContextId();
        logger.info("Protection service handling {} egress bytes 0x{} from {} for {}", encapsulatedPacket.length, MqttsnWireUtils.toHex(encapsulatedPacket), clientId, networkContext);
        
        ByteBuffer senderId = deriveSenderId(clientId);

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
        		//No monotonic counter
        }
        MqttsnProtection packet = (MqttsnProtection) getRegistry().getCodec().createMessageFactory().
                createProtectionMessage(protectionScheme, protectionKey, flags, senderId.array(), monotonicCounter, encapsulatedPacket);

        byte[] protectionPacket = packet.encode();
        logger.debug(packet.toString());
        return protectionPacket;
    }

    @Override
    public byte[] readVerified(INetworkContext networkContext, byte[] data) throws MqttsnSecurityException {
    	//data is the message to be decapsulated
        logger.info("Protection service handling {} ingress bytes 0x{} for {}", data.length, MqttsnWireUtils.toHex(data), networkContext);
        
        if(isSecurityEnvelope(data)){
            logger.debug("Protection packet identified");
            MqttsnProtection packet = (MqttsnProtection) getRegistry().getCodec().decode(data);
           
            Sender sender=sendersWhitelist.get(ByteBuffer.wrap(packet.getSenderId()));
            if(sender!=null)
            {
                //Authorized senderId
            	if(packet.verifyAuthenticationTag(sender.protectionKeys))
            	{
            		logger.debug("The Authentication Tag is valid");
            		return packet.getEncapsulatedPacket();
            	}
            	throw new MqttsnSecurityException("Invalid Authentication Tag!"); 
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
            if(shortVersion && newValue>=(Short.MAX_VALUE+1))
            	newValue=Short.MIN_VALUE;
            return newValue;
        }
        throw new MqttsnSecurityException("Unable to create a monotonic counter!");
    }

    private void addAllowedClientId(final Sender sender) {
        try {
            sendersWhitelist.put(deriveSenderId(sender.clientId), sender);
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
    
    public String getProtectionConfiguration()
    {
        StringBuilder sb = new StringBuilder("Protection configuration:");
        sb.append("\n\tWhitelist:");
        sendersWhitelist.forEach((key, value) -> sb.append("\n\t\t0x").append(HexFormat.of().formatHex(key.array())).append("-").append(value.toString()));
        sb.append("\n\tProtectionKey hash: 0x").append(protectionKeyHash);
        if(!isGateway)
        {
	        sb.append("\n\t").append(protectionScheme.toString());
	        sb.append("\n\t").append(flags.toString());
        }
        return sb.toString();
    }
}
