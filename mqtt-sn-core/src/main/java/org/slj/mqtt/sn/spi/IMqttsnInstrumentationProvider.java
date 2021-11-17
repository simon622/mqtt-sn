package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.utils.StringTable;

/**
 * When called, the provider is a source of instrumentation which will be output by the runtime
 */
public interface IMqttsnInstrumentationProvider {

    /**
     * Provide a snapshot at the point of invocation
     * @return A {@link StringTable} containing details of the current runtime
     */
    StringTable provideInstrumentation();
}
