package org.slj.mqtt.sn.protection.alg;

/**
 * @author Simon L Johnson
 */
public class MqttsnProtectionAlgorithmInitializer {

    public static void initDefaults(){
        ProtectionSchemeHmacSha256.register();
        ProtectionSchemeHmacSha3_256.register();
        ProtectionSchemeCmac128.register();
        ProtectionSchemeCmac192.register();
        ProtectionSchemeCmac256.register();
        ProtectionSchemeCcm_64_128.register();
        ProtectionSchemeCcm_64_192.register();
        ProtectionSchemeCcm_64_256.register();
        ProtectionSchemeCcm_128_128.register();
        ProtectionSchemeCcm_128_192.register();
        ProtectionSchemeCcm_128_256.register();
        ProtectionSchemeGcm_128_128.register();
        ProtectionSchemeGcm_128_192.register();
        ProtectionSchemeGcm_128_256.register();
        ProtectionSchemeChaCha20_Poly1305.register();
    }
}
