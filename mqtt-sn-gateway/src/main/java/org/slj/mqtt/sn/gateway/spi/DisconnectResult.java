package org.slj.mqtt.sn.gateway.spi;

public class DisconnectResult extends Result {

    public DisconnectResult(STATUS status, String message) {
        super(status);
        setMessage(message);
    }

    public DisconnectResult(STATUS status, int returnCode, String message) {
        super(status);
        setMessage(message);
        setReturnCode(returnCode);
    }
}