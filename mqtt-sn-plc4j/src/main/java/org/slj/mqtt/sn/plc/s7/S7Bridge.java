package org.slj.mqtt.sn.plc.s7;

import org.slj.mqtt.sn.cloud.ProtocolBridgeDescriptor;
import org.slj.mqtt.sn.gateway.spi.bridge.ProtocolBridgeOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.plc.PLX4JBridge;

public class S7Bridge extends PLX4JBridge {

    public S7Bridge(IMqttsnGatewayRuntimeRegistry registry, ProtocolBridgeDescriptor descriptor, ProtocolBridgeOptions options) {
        super(registry, descriptor, options);
    }
}
