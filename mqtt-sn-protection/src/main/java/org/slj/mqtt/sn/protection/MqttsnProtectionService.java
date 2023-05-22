package org.slj.mqtt.sn.protection;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.impl.MqttsnSecurityService;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnSecurityException;
import org.slj.mqtt.sn.utils.Security;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.version2_0.payload.MqttsnProtection;

import java.nio.ByteBuffer;
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
    private byte authenticationTagLength = 0x00; //Reserved value by default to force a selection
    private byte keyMaterialLength = 0x00; //no key material by default
    private byte monotonicCounterLength = 0x00; //no monotonic counter by default
    private byte protectionScheme = 0x3F; //Reserved value by default to force a selection 

    
    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        try {
            secureRandom = SecureRandom.getInstanceStrong();
        }
        catch(Exception e)
        {
        	throw new MqttsnException(e);
        }

        //*** TODO: to be retrieved from a configuration file ***//
        //Client configuration
        addAllowedClientId("protectionClient"); //Whitelist of client senders
        authenticationTagLength = 0x03; //64 bits of authentication tag
        keyMaterialLength = 0x00; //no key material
        monotonicCounterLength = 0x01; //16 bits of monotonic counter
        protectionScheme=MqttsnProtection.HMAC_SHA256;

        //Server configuration
        addAllowedClientId("protectionGateway"); //Whitelist of gateway senders
        //*** END TODO ***//
    }

    @Override
    public byte[] writeVerified(INetworkContext networkContext, byte[] data)
            throws MqttsnSecurityException {
    	//data is the message to be encapsulated
        logger.info("Protection service handling {} egress bytes 0x{} for {}", data.length, MqttsnWireUtils.toHex(data), networkContext);
        
        ByteBuffer senderId = deriveSenderId(registry.getOptions().getContextId());

        int random = secureRandom.nextInt();
        int keymaterial = 0; //-- Speak to davide RE: this one
        int monotonicCounter = nextMonotonicCounterValue(networkContext);
        MqttsnProtection packet = (MqttsnProtection) getRegistry().getCodec().createMessageFactory().
                createProtectionMessage(protectionScheme, senderId.array(), random, keymaterial, monotonicCounter, data);
        logger.info("Protection packet to be sent: "+getProtectionPacketAsLogString(packet));

        //-- TODO now its all done and dusted.. need to set the auth tag
        //-- I guess we need an encode phase.. then take the subset of the encoded data
        //packet.setAuthTag();
//        return getRegistry().getCodec().encode(packet);

        //-- Unit the impl id done just return the original data
        return data;

    }

    @Override
    public byte[] readVerified(INetworkContext networkContext, byte[] data) throws MqttsnSecurityException {
    	//data is the message to be decapsulated
        logger.info("Protection service handling {} ingress bytes 0x{} for {}", data.length, MqttsnWireUtils.toHex(data), networkContext);
        
        if(isSecurityEnvelope(data)){
            MqttsnProtection packet = (MqttsnProtection)
                    getRegistry().getCodec().decode(data);
            logger.info("Protection packet received: "+getProtectionPacketAsLogString(packet));
            
            //TODO This is where we need to verify the packet

            return data;
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

    private Integer nextMonotonicCounterValue(INetworkContext networkContext) throws MqttsnSecurityException
    {
        IClientIdentifierContext clientIdentifierContext =
                getRegistry().getNetworkRegistry().getMqttsnContext(networkContext);
        if(clientIdentifierContext != null){
            //this must be an anonymous message OR connect.. so we need to handle this out of band
            AtomicInteger counter = (AtomicInteger) clientIdentifierContext.getContextObject(COUNTER_CONTEXT_KEY);
            if(counter == null){
                counter = new AtomicInteger();
                clientIdentifierContext.putContextObject(COUNTER_CONTEXT_KEY, counter);
            }
            return counter.incrementAndGet();
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
    
    private String getProtectionPacketAsLogString(MqttsnProtection packet)
    {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("protectionSchema=").append(packet.getProtectionSchema());
        sb.append(", senderId=").append(HexFormat.of().formatHex(packet.getSenderId()));
        sb.append(", random=").append(packet.getRandom());
        sb.append(", keyMaterial=").append(packet.getKeyMaterial());
        sb.append(", keyMaterialLength=").append(keyMaterialLength);
        sb.append(", monotonicCounter=").append(packet.getMonotonicCounter());
        sb.append(", monotonicCounterLength=").append(monotonicCounterLength);
        sb.append(", encapsultedPacket=").append(HexFormat.of().formatHex(packet.getEncapsultedPacket()));
        sb.append(", authTag=").append(Arrays.toString(packet.getAuthTag()));
        sb.append(", authTagLength=").append(authenticationTagLength);
        sb.append('}');
        return sb.toString();
    }
}
