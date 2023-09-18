package org.slj.mqtt.sn.descriptor;

/**
 * @author Simon L Johnson
 */
public abstract class Descriptor {

    private String name;
    private String description;

    public Descriptor(final String name, final String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
