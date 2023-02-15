package org.slj.mqtt.sn.client.spi;

import org.slj.mqtt.sn.model.MqttsnOptions;

/**
 * @author Simon L Johnson
 */
public class MqttsnClientOptions extends MqttsnOptions {

    public static boolean DEFAULT_DISCONNECT_STOPS_TRANSPORT = true;
    public static boolean DEFAULT_SLEEP_STOPS_TRANSPORT = false;

    public boolean disconnectStopsTransport = DEFAULT_DISCONNECT_STOPS_TRANSPORT;
    public boolean sleepStopsTransport = DEFAULT_SLEEP_STOPS_TRANSPORT;


    public boolean getDisconnectStopsTransport() {
        return disconnectStopsTransport;
    }

    public MqttsnClientOptions withDisconnectStopsTransport(boolean defaultDisconnectStopsTransport) {
        disconnectStopsTransport = defaultDisconnectStopsTransport;
        return this;
    }

    public boolean getSleepStopsTransport() {
        return sleepStopsTransport;
    }

    public MqttsnClientOptions withSleepStopsTransport(boolean sleepStopsTransport) {
        this.sleepStopsTransport = sleepStopsTransport;
        return this;
    }
}
