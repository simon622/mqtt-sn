package org.slj.mqtt.tree;

public class MqttTreeInputException extends MqttTreeException{

    public MqttTreeInputException() {
    }

    public MqttTreeInputException(String message) {
        super(message);
    }

    public MqttTreeInputException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttTreeInputException(Throwable cause) {
        super(cause);
    }
}
