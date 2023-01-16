package org.slj.mqtt.sn.plc;

import org.apache.plc4x.java.PlcDriverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.cloud.ProtocolBridgeDescriptor;
import org.slj.mqtt.sn.gateway.impl.bridge.AbstractProtocolBridge;
import org.slj.mqtt.sn.gateway.spi.bridge.IProtocolBridgeConnection;
import org.slj.mqtt.sn.gateway.spi.bridge.ProtocolBridgeOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;

public class PLX4JBridge extends AbstractProtocolBridge {

    static Logger logger = LoggerFactory.getLogger(PLX4JBridge.class.getName());

    protected PlcDriverManager driverManager;

    public PLX4JBridge(IMqttsnGatewayRuntimeRegistry registry, ProtocolBridgeDescriptor descriptor, ProtocolBridgeOptions options) {
        super(registry, descriptor, options);
        driverManager = new PlcDriverManager();
    }

    @Override
    public IProtocolBridgeConnection createConnection(){
        logger.info("creating new PLX4J connection");
        PLX4JConnection connection = new PLX4JConnection(driverManager, options, registry, descriptor);
        return connection;
    }

    @Override
    public String getConnectionString() {
        return descriptor.toString();
    }
}
