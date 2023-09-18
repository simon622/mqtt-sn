package org.slj.mqtt.sn.descriptor;

/**
 * @author Simon L Johnson
 */
public class FlagsDescriptor extends Descriptor {

    private final int startIdx;
    private final int endIdx;

    public FlagsDescriptor(final String name, final String description, final int startIdx, final int endIdx) {
        super(name, description);
        this.startIdx = startIdx;
        this.endIdx = endIdx;
    }

    public FlagsDescriptor(final String name, final String description, final int startIdx) {
        super(name, description);
        this.startIdx = startIdx;
        this.endIdx = startIdx;
    }

    public int getStartIdx() {
        return startIdx;
    }

    public int getEndIdx() {
        return endIdx;
    }
}
