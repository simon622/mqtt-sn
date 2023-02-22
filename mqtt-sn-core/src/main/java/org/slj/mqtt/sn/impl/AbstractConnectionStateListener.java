package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.spi.IMqttsnConnectionStateListener;

/**
 * @author Simon L Johnson
 */
public abstract class AbstractConnectionStateListener implements IMqttsnConnectionStateListener {

    @Override
    public void notifyConnected(IClientIdentifierContext context) {

    }

    @Override
    public void notifyRemoteDisconnect(IClientIdentifierContext context) {

    }

    @Override
    public void notifyActiveTimeout(IClientIdentifierContext context) {

    }

    @Override
    public void notifyLocalDisconnect(IClientIdentifierContext context, Throwable t) {

    }

    @Override
    public void notifyConnectionLost(IClientIdentifierContext context, Throwable t) {

    }
}
