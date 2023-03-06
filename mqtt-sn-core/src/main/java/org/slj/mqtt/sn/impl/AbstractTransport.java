package org.slj.mqtt.sn.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.IPacketTXRXJob;
import org.slj.mqtt.sn.model.PacketTXRXJob;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public abstract class AbstractTransport extends AbstractMqttsnService implements ITransport {

    protected ExecutorService ingressProtocolProcessor;
    protected ExecutorService egressProtocolProcessor;

    protected final Logger wireLogger = LoggerFactory.getLogger("wire");

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        ingressProtocolProcessor = runtime.getRuntime().createManagedExecutorService(
                String.format("%s-transport-ingress-%s-", getName(), System.identityHashCode(runtime)),
                runtime.getOptions().getTransportIngressThreadCount());
        egressProtocolProcessor = runtime.getRuntime().createManagedExecutorService(
                String.format("%s-transport-egress-%s-", getName(), System.identityHashCode(runtime)),
                runtime.getOptions().getTransportEgressThreadCount());
    }

    @Override
    public void stop() throws MqttsnException {
        super.stop();
        try {
            if(ingressProtocolProcessor != null){
                registry.getRuntime().closeManagedExecutorService(ingressProtocolProcessor);
            }
        } finally {
            if(egressProtocolProcessor != null){
                registry.getRuntime().closeManagedExecutorService(egressProtocolProcessor);
            }
        }
    }

    public final Future<IPacketTXRXJob> writeToTransportWithCallback(INetworkContext context, byte[] data, Runnable task) {

        if(!running){
            logger.warn("unable to write to {} bytes transport when not running", data.length);
        }

        if(egressProtocolProcessor == null){
            throw new MqttsnRuntimeException("no processors available on transport");
        }

        if(registry.getSecurityService().protocolIntegrityEnabled()){
            data = registry.getSecurityService().writeVerified(context, data);
        }

        if(data.length > registry.getOptions().getMaxProtocolMessageSize()){
            logger.error("cannot send {} bytes - max allowed message size {}",
                    data.length, registry.getOptions().getMaxProtocolMessageSize());
            throw new MqttsnRuntimeException("cannot send messages larger than allowed max");
        }

        if(registry.getOptions().isWireLoggingEnabled()){
            wireLogger.info("wire {} tx {} ", context.getNetworkAddress(),
                    MqttsnWireUtils.toBinary(data));
        }

        final PacketTXRXJob job = new PacketTXRXJob();
        job.setNetworkContext(context);

        final byte[] d = data;
        Runnable r = () -> {
            try {
                writeToTransportInternal(context, d);
            }
            catch(Throwable t){
                job.setError(t);
            }
            finally {
                if(task != null) task.run();
                job.setComplete(true);
            }
        };

        if(running()){
            return getRegistry().getRuntime().submit(egressProtocolProcessor,
                    r, job);
        } else {
            logger.warn("unable to send to transport as no longer running");
            return null;
        }
    }


    @Override
    public final Future<IPacketTXRXJob> writeToTransport(INetworkContext context, byte[] data) {

        return writeToTransportWithCallback(context, data, null);
    }

    @Override
    public final void receiveFromTransport(INetworkContext context, byte[] data) {

        if(ingressProtocolProcessor == null){
            throw new MqttsnRuntimeException("no processors available on transport");
        }

        if (data.length > registry.getOptions().getMaxProtocolMessageSize()) {
            logger.error("receiving {} bytes - max allowed message size {} - error",
                    data.length, registry.getOptions().getMaxProtocolMessageSize());
            throw new MqttsnRuntimeException("received message was larger than allowed max");
        }

        if (registry.getOptions().isWireLoggingEnabled()) {
            wireLogger.info("wire {} rx {} ", context.getNetworkAddress(),
                    MqttsnWireUtils.toBinary(data));
        }

        if(registry.getSecurityService().protocolIntegrityEnabled()){
            data = registry.getSecurityService().readVerified(context, data);
        }

        if(!running){
            logger.warn("transport is NOT RUNNING trying to receive {} byte Datagram to {}", MqttsnWireUtils.toBinary(data), context);
            return;
        }

        final byte[] d = data;
        getRegistry().getRuntime().submit(ingressProtocolProcessor,
                () -> receiveFromTransportInternal(context, d), context);
    }

    protected abstract void writeToTransportInternal(INetworkContext context, byte[] data) ;

    protected abstract void receiveFromTransportInternal(INetworkContext networkContext, byte[] data) ;

    public void connectionLost(INetworkContext context, Throwable t){
        if(registry != null && context != null){
            registry.getRuntime().handleConnectionLost(
                    registry.getNetworkRegistry().getMqttsnContext(context), t);
        }
    }

    protected static ByteBuffer wrap(byte[] arr, int length){
        return ByteBuffer.wrap(arr, 0 , length);
    }

    protected static byte[] drain(ByteBuffer buffer){
        byte[] arr = new byte[buffer.remaining()];
        buffer.get(arr, 0, arr.length);
        return arr;
    }
}
