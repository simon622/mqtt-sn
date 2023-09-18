package org.slj.mqtt.sn.protection.impl;

import org.slj.mqtt.sn.protection.spi.IProtectedSenderRegistry;
import org.slj.mqtt.sn.protection.spi.MqttsnProtectionOptions;
import org.slj.mqtt.sn.protection.spi.ProtectedSender;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.Security;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Simon L Johnson
 */
public class InMemoryProtectedSenderRegistry extends AbstractMqttsnService implements IProtectedSenderRegistry {

    private Map<ByteBuffer, ProtectedSender> sendersWhitelist = new ConcurrentHashMap<>();
    protected MqttsnProtectionOptions options;

    public InMemoryProtectedSenderRegistry(final List<ProtectedSender> allowedSenders, final MqttsnProtectionOptions options) {
        this.options = options;
        allowedSenders.stream().forEach(p -> addAllowedClientId(p));
    }

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        logger.debug("Starting protected sender registry");
        super.start(runtime);
    }

    public void addAllowedClientId(final ProtectedSender sender) {
        sendersWhitelist.put(deriveSenderId(sender.getClientId()), sender);
    }

    @Override
    public ByteBuffer deriveSenderId(final String clientId) {
        if(clientId == null || clientId.length() == 0){
            throw new MqttsnRuntimeException("invalid clientId format - null or empty");
        }
        else {
            try {
                return ByteBuffer.wrap(Arrays.copyOfRange(Security.hash(clientId.getBytes(), options.getHashAlgorithm()), 0,
                        options.getSenderPrefixLength()));
            } catch (NoSuchAlgorithmException e) {
                throw new MqttsnRuntimeException(e);
            }
        }
    }

    @Override
    public ProtectedSender lookupProtectedSender(ByteBuffer prefix) {
        return sendersWhitelist.get(prefix);
    }
}
