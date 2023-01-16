package org.slj.mqtt.sn.gateway.spi.bridge;

import org.slj.mqtt.sn.cloud.ProtocolBridgeDescriptor;
import org.slj.mqtt.sn.spi.IMqttsnService;

import java.util.List;

public interface IProtocolBridgeService extends IMqttsnService {

    boolean initializeBridge(ProtocolBridgeDescriptor descriptor, ProtocolBridgeOptions options) throws ProtocolBridgeException;

    List<ProtocolBridgeDescriptor> getActiveBridges(List<ProtocolBridgeDescriptor> descriptors);

    ProtocolBridgeDescriptor getDescriptorById(List<ProtocolBridgeDescriptor> descriptors, String bridgeId);

    boolean bridgeAvailable(ProtocolBridgeDescriptor descriptor);

    IProtocolBridgeConnection getActiveConnectionIfExists(ProtocolBridgeDescriptor descriptor) throws ProtocolBridgeException;

    void close(IProtocolBridgeConnection connection) throws ProtocolBridgeException;
}
