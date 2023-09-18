package org.slj.mqtt.sn.descriptor;

import javax.xml.crypto.dsig.keyinfo.RetrievalMethod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Simon L Johnson
 */
public class FieldDescriptor extends Descriptor {

    private int index;
    private int length = 1;
    private List<FlagsDescriptor> flags;
    private boolean optional = false;

    public FieldDescriptor(final String name, final String description, final int index) {
        super(name, description);
        this.index = index;
    }

    public FieldDescriptor(final String name, final String description) {
        super(name, description);
    }
    public FieldDescriptor(final String name) {
        super(name, null);
    }

    public void setIndex(int index){
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public FieldDescriptor addFlagsDescriptor(final FlagsDescriptor flagsDescriptor){
        if(flags == null){
            flags = new ArrayList<>();
        }
        flags.add(flagsDescriptor);
        return this;
    }

    public boolean hasFlags(){
        return flags != null && !flags.isEmpty();
    }

    public List<FlagsDescriptor> getFlags(){
        return Collections.unmodifiableList(flags);
    }

    public int getLength() {
        return length;
    }

    public FieldDescriptor setLength(final int length) {
        this.length = length;
        return this;
    }

    public boolean isOptional() {
        return optional;
    }

    public FieldDescriptor setOptional(final boolean optional) {
        this.optional = optional;
        return this;
    }
}
