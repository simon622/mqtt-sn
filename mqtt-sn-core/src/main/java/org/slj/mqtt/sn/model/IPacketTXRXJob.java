package org.slj.mqtt.sn.model;

public interface IPacketTXRXJob {

    INetworkContext getNetworkContext();

    Throwable getError();

    boolean isComplete();

    boolean isError();

}
