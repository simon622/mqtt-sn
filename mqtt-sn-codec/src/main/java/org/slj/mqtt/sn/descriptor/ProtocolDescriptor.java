package org.slj.mqtt.sn.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Simon L Johnson
 */
public class ProtocolDescriptor extends Descriptor {

    private List<PacketDescriptor> packets = new ArrayList<>();

    public ProtocolDescriptor(final String name, final String description) {
        super(name, description);
    }

    public void addPacketDescriptor(final PacketDescriptor packetDescriptor){
        this.packets.add(packetDescriptor);
    }

    public List<PacketDescriptor> getPackets() {
        return Collections.unmodifiableList(packets);
    }
}
