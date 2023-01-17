package org.slj.mqtt.sn.gateway.impl.bridge;

import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.cloud.ProtocolBridgeDescriptor;
import org.slj.mqtt.sn.gateway.spi.ConnectResult;
import org.slj.mqtt.sn.gateway.spi.bridge.*;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ProtocolBridgeService extends AbstractMqttsnService implements IProtocolBridgeService {

    protected Map<ProtocolBridgeDescriptor, IProtocolBridgeConnection> connections;
    protected ScheduledExecutorService bridgePollingService;
    private final Object mutex = new Object();

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        connections = Collections.synchronizedMap(new HashMap<>());
        bridgePollingService = runtime.getRuntime().createManagedScheduledExecutorService("mqtt-sn-scheduled-bridge-polling-",
                2);
    }

    @Override
    public void stop() throws MqttsnException {
        super.stop();
        try {
            connections.entrySet().forEach(c -> c.getValue().close());
        } finally {
            connections = null;
        }
    }

    @Override
    public IProtocolBridgeConnection getActiveConnectionIfExists(ProtocolBridgeDescriptor descriptor) throws ProtocolBridgeException {
        return connections.get(descriptor);
    }

    @Override
    public IMqttsnGatewayRuntimeRegistry getRegistry() {
        return (IMqttsnGatewayRuntimeRegistry) registry;
    }

    @Override
    public boolean bridgeAvailable(ProtocolBridgeDescriptor descriptor) {
        try {
            return getBridgeClass(descriptor) != null;
        } catch(ProtocolBridgeException e){
            return false;
        }
    }

    protected Class<? extends IProtocolBridge> getBridgeClass(ProtocolBridgeDescriptor descriptor) throws ProtocolBridgeException {
        String className = descriptor.getClassName();
        if(className != null && !className.isEmpty()){
            try {
                return (Class<? extends IProtocolBridge>) Class.forName(className);
            } catch(ClassNotFoundException e){
                //-- check the context classloader
                try {
                    ClassLoader cls = Thread.currentThread().getContextClassLoader();
                    return (Class<? extends IProtocolBridge>)  cls.loadClass(className);
                } catch(ClassNotFoundException E){
                }
            }
        }
        throw new ProtocolBridgeException("unable to load connector from runtime <" + className + ">");
    }

    public boolean initializeBridge(ProtocolBridgeDescriptor descriptor, ProtocolBridgeOptions options) throws ProtocolBridgeException {
        try {
            if(bridgeAvailable(descriptor)){
                synchronized (mutex){
                    logger.info("starting new instance of bridge {} using {}", descriptor.getClassName(), options);
                    IProtocolBridge bridge = getBridgeClass(descriptor).getConstructor(IMqttsnGatewayRuntimeRegistry.class,
                                    ProtocolBridgeDescriptor.class, ProtocolBridgeOptions.class).
                            newInstance(registry, descriptor, options);
                    ProtocolBridgeClientContext context = createContextForBridge(descriptor, options);
                    IProtocolBridgeConnection connection = bridge.createConnection();
                    ConnectResult result = connection.connect(context);
                    if(result.isError()){
                        return false;
                    } else {
                        connections.put(descriptor, connection);
                        if(options.getSubscriptions() != null){
                            if(MqttsnSpecificationValidator.isValidSubscriptionTopic(options.getSubscriptions())){
                                connection.subscribe(context, new TopicPath(options.getSubscriptions()));
                            }
                        }
                        return true;
                    }
                }
            } else {
                return false;
            }
        }
        catch (Exception e){
            throw new ProtocolBridgeException("unable to initialize connector;", e);
        }
    }

    public List<ProtocolBridgeDescriptor> getActiveBridges(List<ProtocolBridgeDescriptor> descriptors) {
        List<ProtocolBridgeDescriptor> active = new ArrayList<>();
        Iterator<ProtocolBridgeDescriptor> c = connections.keySet().iterator();
        synchronized (mutex){
            while(c.hasNext()){
                ProtocolBridgeDescriptor x = c.next();
                if(descriptors.contains(x)){
                    active.add(x);
                }
            }
        }
        return active;
    }

    public ProtocolBridgeDescriptor getDescriptorById(List<ProtocolBridgeDescriptor> descriptors, String bridgeId){
        Optional<ProtocolBridgeDescriptor> descriptor = descriptors.stream().
                filter(c -> c.getClassName().equals(bridgeId)).findAny();
        return descriptor.orElse(null);
    }

    public ProtocolBridgeClientContext createContextForBridge(ProtocolBridgeDescriptor descriptor, ProtocolBridgeOptions options) throws ProtocolBridgeException{

        try {
            NetworkAddress address = new NetworkAddress(options.getPort(), options.getHostName());
            INetworkContext networkContext =
                    getRegistry().getContextFactory().createInitialNetworkContext(null, address);
            ProtocolBridgeClientContext context = new ProtocolBridgeClientContext(options.getClientId(), descriptor, options);
            getRegistry().getNetworkRegistry().bindContexts(networkContext, context);
            return context;
        } catch(Exception e){
            throw new ProtocolBridgeException(e);
        }
    }

    @Override
    public void close(IProtocolBridgeConnection connection) throws ProtocolBridgeException {
        try {
           synchronized (mutex){
               if(connection != null && connection.isConnected()){
                   connections.remove(connection.getDescriptor());
                   connection.disconnect(null);
                   connection.close();
               }
           }
        } catch(Exception e){
            throw new ProtocolBridgeException(e);
        }
    }

    @Override
    public ScheduledFuture<?> schedulePolling(Runnable runnable, long initialDelay, long period, TimeUnit unit){
        return bridgePollingService.scheduleAtFixedRate(runnable, initialDelay, period, unit);
    }
}
