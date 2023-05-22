package org.slj.mqtt.sn.protection;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.impl.MqttsnSecurityService;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnSecurityException;
import org.slj.mqtt.sn.utils.Security;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.version2_0.payload.MqttsnProtection;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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

    //-- The hash to use on the senderId
    private static final String HASH_ALG = "SHA-256";

    //-- The prefix size used for senderId lookups
    private static final int SENDER_PREFIX_LEN = 8;

    //-- Super simple and hacky way of storing the valid senderId and their respective
    //-- hash lookups
    protected Map<NetworkAddress, ClientInfo> hashToClient = new ConcurrentHashMap<>();

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        //add allowed client ids for testing - NOTE in reality this will be done using config
        //and external lookups
        addAllowedClientId("someClientId", NetworkAddress.localhost(25556));
    }

    @Override
    public byte[] writeVerified(INetworkContext networkContext, byte[] data)
            throws MqttsnSecurityException {
        logger.info("Protection service handling {} egress bytes 0x{} for {}", data.length, MqttsnWireUtils.toHex(data), networkContext);
        
        byte scheme = MqttsnProtection.AES_GCM_256_128;
        byte[] senderId = deriveSenderId(networkContext);
        long nonce = generateNonce();
        int counter = counter(networkContext);
        long keymaterial = 0; //-- Speak to davide RE: this one
        MqttsnProtection packet = (MqttsnProtection) getRegistry().getCodec().createMessageFactory().
                createProtectionMessage(scheme, senderId, nonce, counter, keymaterial, data);


        //-- TODO now its all done and dusted.. need to set the auth tag
        //-- I guess we need an encode phase.. then take the subset of the encoded data
        //packet.setAuthTag();
//        return getRegistry().getCodec().encode(packet);

        //-- Unit the impl id done just return the original data
        return data;

    }

    @Override
    public byte[] readVerified(INetworkContext networkContext, byte[] data) throws MqttsnSecurityException {

        logger.info("Protection service handling {} ingress bytes 0x{} for {}", data.length, MqttsnWireUtils.toHex(data), networkContext);
        
        if(isSecurityEnvelope(data)){
            MqttsnProtection packet = (MqttsnProtection)
                    getRegistry().getCodec().decode(data);

            //TODO This is where we need to verify the packet

            return data;
        }
        else {
            return super.readVerified(networkContext, data);
        }
    }

    protected boolean isSecurityEnvelope(byte[] data){
        if(getRegistry().getCodec().supportsVersion(MqttsnConstants.PROTOCOL_VERSION_2_0)){
            int msgType = MqttsnWireUtils.readMessageType(data);
            return MqttsnConstants.PROTECTION == msgType;
        }
        return false;
    }

    private byte[] deriveSenderId(INetworkContext networkContext){
        return "".getBytes();
    	/*ClientInfo info = hashToClient.get(networkContext.getNetworkAddress());
        if(info != null){
            return info.hash.getBytes(StandardCharsets.UTF_8);
        }
        throw new SecurityException("sending address was not found in whitelist");*/
    }

    private Integer counter(INetworkContext networkContext){
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

        //-- no counter
        return 0;
    }

    private void addAllowedClientId(final String clientId, final NetworkAddress address) {
        try {
            byte[] hash = Security.hash(clientId.getBytes(), HASH_ALG);
            String hashLookup = MqttsnWireUtils.toHex(hash).
                    substring(0, SENDER_PREFIX_LEN - 1);
            ClientInfo info = new ClientInfo(clientId, hashLookup, address);
            hashToClient.put(address, info);
        } catch(NoSuchAlgorithmException e){
            throw new MqttsnSecurityException(e);
        }
    }

    protected int generateNonce(){
        try {
            return SecureRandom.getInstanceStrong().nextInt();
        } catch(Exception e){
            throw new MqttsnSecurityException(e);
        }
    }

    //-- Bootstrap into the runtime for protocol packets
    public boolean protocolIntegrityEnabled(){
        return true;
    }

    static class ClientInfo {

        public final String clientId;
        public final String hash;
        public final NetworkAddress address;

        public ClientInfo(final String clientId, final String hash, final NetworkAddress address) {
            this.clientId = clientId;
            this.hash = hash;
            this.address = address;
        }
    }
}
