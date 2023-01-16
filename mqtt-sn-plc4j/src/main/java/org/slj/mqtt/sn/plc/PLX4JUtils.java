package org.slj.mqtt.sn.plc;

import org.apache.plc4x.java.api.messages.PlcSubscriptionEvent;
import org.apache.plc4x.java.api.model.PlcField;
import org.slj.mqtt.sn.utils.Pair;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PLX4JUtils {

    public static List<Pair<String, byte[]>> getDataFromSubscriptionEvent(PlcSubscriptionEvent evt){
        List<Pair<String, byte[]>> output = new ArrayList<>();
        Collection<String> s = evt.getFieldNames();
        for (String field : s) {

            byte[] arr = null;
            if(evt.isValidString(field)){
                arr = evt.getString(field).getBytes(StandardCharsets.UTF_8);
            } else if (evt.isValidDouble(field)) {
                arr = ByteBuffer.allocate(8).putDouble(evt.getDouble(field)).array();
            } else if (evt.isValidInteger(field)) {
                arr = ByteBuffer.allocate(4).putInt(evt.getInteger(field)).array();
            } else if (evt.isValidLong(field)) {
                arr = ByteBuffer.allocate(8).putLong(evt.getLong(field)).array();
            } else if (evt.isValidShort(field)) {
                arr = ByteBuffer.allocate(2).putShort(evt.getShort(field)).array();
            } else if (evt.isValidByte(field)) {
                arr = ByteBuffer.allocate(1).put(evt.getByte(field)).array();
            }
            output.add(Pair.of(field,arr));
        }
        return output;
    }

}
