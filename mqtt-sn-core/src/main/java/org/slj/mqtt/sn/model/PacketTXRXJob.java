package org.slj.mqtt.sn.model;

public class PacketTXRXJob implements IPacketTXRXJob {

    private boolean complete = false;
    private Throwable error;
    private INetworkContext networkContext;


    public PacketTXRXJob() {
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public void setNetworkContext(INetworkContext networkContext) {
        this.networkContext = networkContext;
    }

    @Override
    public INetworkContext getNetworkContext() {
        return networkContext;
    }

    @Override
    public Throwable getError() {
        return error;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public boolean isError() {
        return error != null;
    }
}
