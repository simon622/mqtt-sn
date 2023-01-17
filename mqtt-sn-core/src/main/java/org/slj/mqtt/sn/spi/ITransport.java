package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.IPacketTXRXJob;
import org.slj.mqtt.sn.utils.StringTable;

import java.util.concurrent.Future;

@MqttsnService(order = MqttsnService.FIRST)
public interface ITransport extends IMqttsnService {

    void receiveFromTransport(INetworkContext context, byte[] data) throws MqttsnException ;

    Future<IPacketTXRXJob> writeToTransport(INetworkContext context, byte[] data) throws MqttsnException ;

    Future<IPacketTXRXJob> writeToTransportWithCallback(INetworkContext context, byte[] data, Runnable task) ;

    void connectionLost(INetworkContext context, Throwable t);

    StringTable getTransportDetails();

    String getName();

    int getPort();

    String getDescription();

}
