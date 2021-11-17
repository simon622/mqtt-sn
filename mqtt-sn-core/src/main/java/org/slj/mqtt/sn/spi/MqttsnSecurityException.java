package org.slj.mqtt.sn.spi;

public class MqttsnSecurityException extends SecurityException {

    public MqttsnSecurityException() {
    }

    public MqttsnSecurityException(String s) {
        super(s);
    }

    public MqttsnSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttsnSecurityException(Throwable cause) {
        super(cause);
    }
}
