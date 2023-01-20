package org.slj.mqtt.sn.net;

import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.IPacketTXRXJob;
import org.slj.mqtt.sn.spi.*;

import java.util.concurrent.Future;

public class ContextTransportLocator extends AbstractMqttsnService implements ITransportLocator  {

    @Override
    public Future<IPacketTXRXJob> writeToTransport(INetworkContext context, IMqttsnMessage message)
            throws MqttsnException {
        return ((IMqttsnTransport)context.getTransport()).writeToTransport(context, message);
    }

    @Override
    public ITransport getTransport(INetworkContext context) throws MqttsnException {
        return context.getTransport();
    }
}
