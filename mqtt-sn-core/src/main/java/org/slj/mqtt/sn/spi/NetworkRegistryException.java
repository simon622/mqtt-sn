package org.slj.mqtt.sn.spi;

public class NetworkRegistryException extends Exception {

    public NetworkRegistryException() {
    }

    public NetworkRegistryException(String message) {
        super(message);
    }

    public NetworkRegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public NetworkRegistryException(Throwable cause) {
        super(cause);
    }
}
