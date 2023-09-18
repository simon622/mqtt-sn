package org.slj.mqtt.sn.protection.spi;

import org.slj.mqtt.sn.wire.version2_0.payload.ProtectionKey;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Simon L Johnson
 */
public class ProtectedSender {

    private final String clientId;
    private final ArrayList<ProtectionKey> protectionKeys = new ArrayList<>();

    public ProtectedSender(final String clientId, final List<byte[]> protectionKeys){
        this.clientId = clientId;
        protectionKeys.forEach(protectionKey ->
                this.protectionKeys.add(new ProtectionKey(protectionKey)));
    }

    public List<ProtectionKey> getProtectionKeys() {
        return protectionKeys;
    }

    public String getClientId() {
        return clientId;
    }
}