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

        //*** TODO: to be retrieved from a configuration file ***//
        protectionKey = HexFormat.of().parseHex("8d8c0e211361005215e902cdfa4b1e0b9d25e497ea71d75439224e55804aea2e7a9c975316d427cc6e00dbe5c2e389127a9c975316d427cc6e00dbe5c2e38912");
        	//256 bits Ox8d8c0e211361005215e902cdfa4b1e0b9d25e497ea71d75439224e55804aea2e
        	//512 bits Ox8d8c0e211361005215e902cdfa4b1e0b9d25e497ea71d75439224e55804aea2e7a9c975316d427cc6e00dbe5c2e389127a9c975316d427cc6e00dbe5c2e38912
        protectionKeyHash = HexFormat.of().formatHex(digest.digest(protectionKey));
    	
        if(isGateway)
        {
            //Server configuration
            addAllowedClientId("protectionClient"); //Whitelist of client senders
            //The protectionScheme to be used is defined by each client
            //The flags to be used are defined by each client
        }
        else
        {
            //Client configuration
            addAllowedClientId("protectionGateway"); //Whitelist of gateway senders (gateway clientId
            protectionScheme=AbstractProtectionScheme.getProtectionScheme(AbstractProtectionScheme.HMAC_SHA256);
            //The flags have to be defined after the protection scheme is selected
            flags = new ProtectionPacketFlags((byte)0x03,(byte)0x00,(byte)0x01);//64 bits of authentication tag, no crypto material, short monotonic counter
        }
        logger.debug(getProtectionConfiguration());
        //*** END TODO ***//
    }

    @Override
    public byte[] writeVerified(INetworkContext networkContext, byte[] data)
            throws MqttsnSecurityException {
    	//data is the message to be encapsulated
        logger.info("Protection service handling {} egress bytes 0x{} for {}", data.length, MqttsnWireUtils.toHex(data), networkContext);
        
        ByteBuffer senderId = deriveSenderId(registry.getOptions().getContextId());

        int random = secureRandom.nextInt();
        int cryptoMaterial=0; 
        switch(flags.getAuthenticationTagLengthDecoded())
        {
        	case ProtectionPacketFlags.SHORT_CRYPTO_MATERIAL:
            	//Add here the required short crypto material
        		break;
        	case ProtectionPacketFlags.LONG_CRYPTO_MATERIAL:
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
                createProtectionMessage(protectionScheme, flags, senderId.array(), random, cryptoMaterial, monotonicCounter, data);

        //-- TODO now its all done and dusted.. need to set the auth tag
        //-- I guess we need an encode phase.. then take the subset of the encoded data
        //packet.setAuthTag();
//        return getRegistry().getCodec().encode(packet);

        //-- Unit the impl id done just return the original data
        byte[] protectionPacket = packet.encode();
        logger.debug(getProtectionPacketAsLogString(packet, protectionPacket));
        return protectionPacket;
        //return data;
    }

    @Override
    public byte[] readVerified(INetworkContext networkContext, byte[] data) throws MqttsnSecurityException {
    	//data is the message to be decapsulated
        logger.info("Protection service handling {} ingress bytes 0x{} for {}", data.length, MqttsnWireUtils.toHex(data), networkContext);
        
        
        if(isSecurityEnvelope(data)){
            logger.info("Protection packet identified");
            MqttsnProtection packet = (MqttsnProtection) getRegistry().getCodec().decode(data);
           
            String senderClientId=sendersWhitelist.get(ByteBuffer.wrap(packet.getSenderId()));
            if(senderClientId!=null)
            {
                //logger.info("Protection packet received from {}: {}",senderClientId,getProtectionPacketAsLogString(packet));
                //TODO This is where we need to verify the packet
                return data;
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
    
    private String getProtectionPacketAsLogString(MqttsnProtection packet, byte[] protectionPacket)
    {
        final StringBuilder sb = new StringBuilder("Protection Packet: 0x");
        sb.append(HexFormat.of().formatHex(protectionPacket));
        sb.append("\n\tprotectionSchema=").append(String.format("%02x", packet.getProtectionScheme().getIndex()&0xff).toUpperCase());
        sb.append("\n\tsenderId=").append(HexFormat.of().formatHex(packet.getSenderId()));
        sb.append("\n\trandom=").append(packet.getRandom());
        sb.append("\n\tcryptoMaterial=").append(packet.getCryptoMaterial());
        sb.append("\n\tcryptoMaterialLength=").append(flags.getCryptoMaterialLength());
        sb.append("\n\tmonotonicCounter=").append(packet.getMonotonicCounter());
        sb.append("\n\tmonotonicCounterLength=").append(flags.getMonotonicCounterLength());
        sb.append("\n\tencapsultedPacket=").append(HexFormat.of().formatHex(packet.getEncapsultedPacket()));
        sb.append("\n\tauthTag=").append(Arrays.toString(packet.getAuthTag()));
        sb.append("\n\tauthTagLength=").append(flags.getAuthenticationTagLength());
        sb.append('}');
        return sb.toString();
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
