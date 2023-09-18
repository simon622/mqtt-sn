package org.slj.mqtt.sn.protection.spi;

import org.slj.mqtt.sn.spi.IMqttsnService;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnService;

import java.nio.ByteBuffer;

/**
 * @author Simon L Johnson
 */
@MqttsnService
public interface IProtectedSenderRegistry extends IMqttsnService {

    ByteBuffer deriveSenderId(String clientId) throws MqttsnException;

    ProtectedSender lookupProtectedSender(ByteBuffer prefix);

}
