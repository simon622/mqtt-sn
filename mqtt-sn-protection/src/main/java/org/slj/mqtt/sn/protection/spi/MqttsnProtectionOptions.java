package org.slj.mqtt.sn.protection.spi;

import org.slj.mqtt.sn.model.MqttsnSecurityOptions;
import org.slj.mqtt.sn.spi.IProtectionScheme;
import org.slj.mqtt.sn.wire.version2_0.payload.ProtectionPacketFlags;

import java.util.Arrays;
import java.util.List;

/**
 * @author Simon L Johnson
 */
public class MqttsnProtectionOptions extends MqttsnSecurityOptions {
    private static final String DEFAULT_HASH_ALG = "SHA-256";
    private static final int DEFAULT_SENDER_PREFIX_LEN = 8;
    private String hashAlgorithm = DEFAULT_HASH_ALG;
    private int senderPrefixLength = DEFAULT_SENDER_PREFIX_LEN;
    private byte[] protectionPacketFlags;
    private byte protectionScheme;
    private byte[] protectionKey;

    public MqttsnProtectionOptions withHashAlgorithm(final String hashAlgorithm){
        this.hashAlgorithm = hashAlgorithm;
        return this;
    }

    public MqttsnProtectionOptions withSenderPrefixLength(final int senderPrefixLength){
        this.senderPrefixLength = senderPrefixLength;
        return this;
    }

    public MqttsnProtectionOptions withProtectionPacketFlags(final byte[] protectionPacketFlags){
        this.protectionPacketFlags = protectionPacketFlags;
        return this;
    }

    public MqttsnProtectionOptions withProtectionKey(final byte[] protectionKey){
        this.protectionKey = protectionKey;
        return this;
    }

    public MqttsnProtectionOptions withProtectionScheme(final byte protectionScheme){
        this.protectionScheme = protectionScheme;
        return this;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public int getSenderPrefixLength() {
        return senderPrefixLength;
    }

    public byte[] getProtectionPacketFlags() {
        return protectionPacketFlags;
    }

    public byte getProtectionScheme() {
        return protectionScheme;
    }

    public byte[] getProtectionKey() {
        return protectionKey;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttsnProtectionOptions{");
        sb.append("hashAlgorithm='").append(hashAlgorithm).append('\'');
        sb.append(", senderPrefixLength=").append(senderPrefixLength);
        sb.append(", protectionPacketFlags=").append(Arrays.toString(protectionPacketFlags));
        sb.append(", protectionScheme=").append(protectionScheme);
        sb.append(", protectionKey=").append(Arrays.toString(protectionKey));
        sb.append('}');
        return sb.toString();
    }
}
