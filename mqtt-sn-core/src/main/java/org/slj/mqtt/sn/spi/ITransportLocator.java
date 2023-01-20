package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.IPacketTXRXJob;

import java.util.concurrent.Future;

@MqttsnService(order = MqttsnService.ANY)
public interface ITransportLocator extends IMqttsnService {

    Future<IPacketTXRXJob> writeToTransport(INetworkContext context, IMqttsnMessage message) throws MqttsnException;

    ITransport getTransport(INetworkContext context) throws MqttsnException;
}
