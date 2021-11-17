package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.MqttsnContext;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.net.NetworkContext;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnConnect;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MqttsnContextFactory<T extends IMqttsnRuntimeRegistry>
        extends MqttsnService<T> implements IMqttsnContextFactory {

    protected static Logger logger = Logger.getLogger(MqttsnContextFactory.class.getName());

    @Override
    public INetworkContext createInitialNetworkContext(NetworkAddress address) throws MqttsnException {

        logger.log(Level.INFO,
                String.format("create new network context for [%s]", address));
        NetworkContext context = new NetworkContext(address);
        return context;
    }

    @Override
    public IMqttsnContext createInitialApplicationContext(INetworkContext networkContext, String clientId) throws MqttsnSecurityException {

        logger.log(Level.INFO, String.format("create new mqtt-sn context for [%s]", clientId));
        MqttsnContext context = new MqttsnContext(clientId);
        return context;
    }

    @Override
    public IMqttsnContext createTemporaryApplicationContext(INetworkContext networkContext) throws MqttsnSecurityException {

        logger.log(Level.INFO, String.format("create temporary mqtt-sn context for [%s]", networkContext));
        MqttsnContext context = new MqttsnContext(null);
        return context;
    }
}
