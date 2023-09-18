package org.slj.mqtt.tree;

public class MqttTreeException extends RuntimeException{

    public MqttTreeException() {
    }

    public MqttTreeException(String message) {
        super(message);
    }

    public MqttTreeException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttTreeException(Throwable cause) {
        super(cause);
    }
}
