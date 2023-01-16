package org.slj.mqtt.sn.gateway.spi.bridge;

import org.slj.mqtt.sn.cloud.ProtocolBridgeDescriptor;
import org.slj.mqtt.sn.model.ClientIdentifierContext;

public class ProtocolBridgeClientContext extends ClientIdentifierContext {

    protected ProtocolBridgeDescriptor descriptor;
    protected ProtocolBridgeOptions options;

    public ProtocolBridgeClientContext(String id, ProtocolBridgeDescriptor descriptor, ProtocolBridgeOptions options) {
        super(id);
        this.descriptor = descriptor;
        this.options = options;
    }

    public ProtocolBridgeDescriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(ProtocolBridgeDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public ProtocolBridgeOptions getOptions() {
        return options;
    }

    public void setOptions(ProtocolBridgeOptions options) {
        this.options = options;
    }
}
