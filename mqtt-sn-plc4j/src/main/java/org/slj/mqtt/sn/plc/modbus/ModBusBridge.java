package org.slj.mqtt.sn.plc.modbus;

import org.slj.mqtt.sn.cloud.ProtocolBridgeDescriptor;
import org.slj.mqtt.sn.gateway.spi.bridge.ProtocolBridgeOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.plc.PLX4JBridge;

public class ModBusBridge extends PLX4JBridge {

    public ModBusBridge(IMqttsnGatewayRuntimeRegistry registry, ProtocolBridgeDescriptor descriptor, ProtocolBridgeOptions options) {
        super(registry, descriptor, options);
    }
}
