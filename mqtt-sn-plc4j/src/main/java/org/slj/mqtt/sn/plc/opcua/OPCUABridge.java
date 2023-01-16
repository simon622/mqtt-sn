package org.slj.mqtt.sn.plc.opcua;

import org.slj.mqtt.sn.cloud.ProtocolBridgeDescriptor;
import org.slj.mqtt.sn.gateway.spi.bridge.ProtocolBridgeOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.plc.PLX4JBridge;

public class OPCUABridge extends PLX4JBridge {

    public OPCUABridge(IMqttsnGatewayRuntimeRegistry registry, ProtocolBridgeDescriptor descriptor, ProtocolBridgeOptions options) {
        super(registry, descriptor, options);
    }
}
