package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.model.INetworkContext;

import java.util.concurrent.Future;

public interface ITransportLocator extends IMqttsnService {

    Future<INetworkContext> writeToTransport(INetworkContext context, IMqttsnMessage message) throws MqttsnException;

    IMqttsnTransport getTransport(INetworkContext context) throws MqttsnException;
}
