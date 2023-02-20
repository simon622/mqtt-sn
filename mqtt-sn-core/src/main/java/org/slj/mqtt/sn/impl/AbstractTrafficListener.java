package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnTrafficListener;

/**
 * Convenience hook for implementing listeners
 * @author Simon L Johnson
 */
public abstract class AbstractTrafficListener implements IMqttsnTrafficListener {

    @Override
    public void trafficSent(INetworkContext context, byte[] data, IMqttsnMessage message) {
        //Override me
    }

    @Override
    public void trafficReceived(INetworkContext context, byte[] data, IMqttsnMessage message) {
        //Override me
    }
}
