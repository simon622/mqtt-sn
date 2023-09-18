package org.slj.mqtt.sn.wire.version2_0;

import org.slj.mqtt.sn.descriptor.ProtocolDescriptor;

/**
 * @author Simon L Johnson
 */
public class Mqttsn_v2_0_ProtocolDescriptor extends ProtocolDescriptor  {

    public static final ProtocolDescriptor INSTANCE = new Mqttsn_v2_0_ProtocolDescriptor();

    private Mqttsn_v2_0_ProtocolDescriptor() {
        super("Mqtt-Sn Version 2.0", "An evolution of the 1.2 SN protocol for battery constrained, network enabled devices");
        initPacketDescriptors();
    }

    protected void initPacketDescriptors(){
    }
}
