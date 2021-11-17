package org.slj.mqtt.sn.spi;

import java.util.Date;
import java.util.UUID;

/**
 * The message registry is a normalised view of transiting messages, it context the raw payload of publish operations
 * so light weight references to the payload can exist in multiple storage systems without duplication of data.
 * For example, when running in gateway mode, the same message my reside in queues for numerous devices which are
 * in different connection states. We should not store payloads N times in this case.
 *
 * The lookup is a simple UUID -> byte[] relationship. It is up to the registry implementation to decide how to store
 * this data.
 *
 */
public interface IMqttsnMessageRegistry <T extends IMqttsnRuntimeRegistry> extends IMqttsnRegistry<T>{

    void tidy() throws MqttsnException ;

    UUID add(byte[] data, boolean removeAfterRead) throws MqttsnException ;

    UUID add(byte[] data, Date expires) throws MqttsnException;

    byte[] get(UUID messageId) throws MqttsnException;
}
