package org.slj.mqtt.sn.protection.impl;

import org.slj.mqtt.sn.utils.Files;

/**
 * @author Simon L Johnson
 */
public class ProtectionUtils {

    public static byte[] loadKey(String clientId, String keyName) {

        try {
            return Files.read(
                    ProtectionUtils.class.getResourceAsStream(String.format("/%s-%s.key", clientId, keyName)), 1024);
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
