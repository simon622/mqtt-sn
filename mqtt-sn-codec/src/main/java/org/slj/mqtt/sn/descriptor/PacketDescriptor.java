package org.slj.mqtt.sn.descriptor;

import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Simon L Johnson
 */
public class PacketDescriptor extends Descriptor {

    private List<FieldDescriptor> fields = new ArrayList<>();
    private int packetType;
    private String notes;

    public PacketDescriptor(final String name, final String description) {
        super(name, description);
    }

    public PacketDescriptor(final String name, final String description, final int packetType) {
        super(name, description);
        this.packetType = packetType;
    }

    public synchronized void addFieldDescriptor(final FieldDescriptor fieldDescriptor){
        if(!fields.isEmpty()){
            FieldDescriptor previous = fields.get(fields.size() - 1);
            int idx = previous.getIndex() + previous.getLength();
            fieldDescriptor.setIndex(idx);
        }
        fields.add(fieldDescriptor);
    }

    public List<FieldDescriptor> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public int getPacketType(){
        return packetType;
    }

    public int getMinSize(){
        if(fields.isEmpty()){
           return 0;
        }
        FieldDescriptor last = fields.get(fields.size() - 1);
        if(last.isOptional()){
            return last.getIndex();
        } else {
            if(last.getLength() == Integer.MAX_VALUE){
                return last.getIndex() + 1;
            }
            else {
                return last.getIndex() + last.getLength();
            }
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }

    public String getPacketTypeHex(){
        return "00x"+ MqttsnWireUtils.toHex((byte) getPacketType());
    }

    public String getPacketTypeDecimal(){
        return ""+ getPacketType();
    }
}
