package org.slj.mqtt.sn.tools;

import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.descriptor.FieldDescriptor;
import org.slj.mqtt.sn.descriptor.FlagsDescriptor;
import org.slj.mqtt.sn.descriptor.PacketDescriptor;
import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.utils.StringTable;
import org.slj.mqtt.sn.utils.StringTableWriters;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Simon L Johnson
 */
public class ProtocolInformationGenerator {

    private IMqttsnCodec codec;

    public ProtocolInformationGenerator(final IMqttsnCodec codec) {
        this.codec = codec;
    }

    public StringTable generateProtocolPacketListing(){
        StringTable stringTable = new StringTable("Type (Hex)", "Type (Dec)", "Min. Length", "Packet Name", "Packet Description");
        for(PacketDescriptor packet : codec.getProtocolDescriptor().getPackets()){
            stringTable.addRow(packet.getPacketTypeHex(),
                    packet.getPacketTypeDecimal(),
                    packet.getMinSize(),
                    String.format("<a href=\"#%s\">%s</a>",packet.getName(), packet.getName()),
                    packet.getDescription());
        }
        return stringTable;
    }

    public List<StringTable> generateAllPacketInformationTables(){
        List<StringTable> tables = new ArrayList<>();
        for(PacketDescriptor packet : codec.getProtocolDescriptor().getPackets()){
            tables.add(generatePacketInformation(packet));
        }
        return tables;
    }

    public StringTable generatePacketInformation(final PacketDescriptor descriptor){
        StringTable stringTable = new StringTable("Index", "Field Name", "Field Description");
        stringTable.setTableName(String.format("(%s) %s (min. %s bytes)", descriptor.getPacketTypeHex(), descriptor.getName(), descriptor.getMinSize()));
        stringTable.setTableDescription(descriptor.getDescription());
        stringTable.setId(descriptor.getName());

        for(FieldDescriptor field : descriptor.getFields()){
            int len = field.getLength();
            String name = field.getName();
            String description = field.getDescription();

            if(field.hasFlags()){
                StringTable flags = generateFieldFlags(field);
                description = "<pre>"+StringTableWriters.writeStringTableAsASCII(flags) + "</pre>";
            }

            String optionalAsterix = field.isOptional() ? "* " : "";
            if(len == 2) {
                stringTable.addCellValue(String.format("%sByte %s", optionalAsterix, field.getIndex() + 1));
                stringTable.addCellValue(String.format("%s MSB", name));
                stringTable.addCellValue(String.format("%s", description == null ? "" : description));
                stringTable.addCellValue(String.format("%sByte %s", optionalAsterix, field.getIndex() + 2));
                stringTable.addCellValue(String.format("%s LSB", name));
                stringTable.addCellValue("");
            } else {
                if(len == Integer.MAX_VALUE){
                    stringTable.addCellValue(String.format("%sByte %s : N", optionalAsterix, field.getIndex() + 1));
                } else {
                    stringTable.addCellValue(String.format("%sByte %s", optionalAsterix, field.getIndex() + 1));
                }
                stringTable.addCellValue(String.format("%s", name));
                stringTable.addCellValue(String.format("%s", description == null ? "" : description));
            }
        }
        return stringTable;
    }

    public StringTable generateFieldFlags(final FieldDescriptor descriptor){
        StringTable stringTable = new StringTable("Bit", "7", "6", "5", "4", "3", "2", "1", "0");
        List<FlagsDescriptor> flagsDescriptors = descriptor.getFlags();
        stringTable.addCellValue("");
        for (FlagsDescriptor flagsDescriptor : flagsDescriptors){
            int start = flagsDescriptor.getStartIdx();
            int end = flagsDescriptor.getEndIdx();
            do {
                stringTable.addCellValue(flagsDescriptor.getName());
            } while(start-- > end);
        }
        return stringTable;
    }

    public static void main(String[] args) {
        ProtocolInformationGenerator generator = new ProtocolInformationGenerator(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2);
        StringTable stringTable = generator.generateProtocolPacketListing();
//        System.err.println(StringTableWriters.writeStringTableAsASCII(stringTable));
        System.err.println(StringTableWriters.writeStringTableAsBasicHTML(stringTable, "main", "table table-striped table-bordered"));

        List<StringTable> stringTables = generator.generateAllPacketInformationTables();
        for (StringTable table : stringTables){
            System.err.println(StringTableWriters.writeStringTableAsBasicHTML(table, table.getId(), "table table-striped table-bordered"));
        }
    }


}
