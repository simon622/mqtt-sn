package org.slj.mqtt.sn.gateway.impl.bridge;

import org.slj.mqtt.sn.cloud.ProtocolBridgeDescriptor;
import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.bridge.IProtocolBridgeConnection;
import org.slj.mqtt.sn.gateway.spi.bridge.ProtocolBridgeException;
import org.slj.mqtt.sn.gateway.spi.bridge.ProtocolBridgeOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LoadGeneratingBridge extends AbstractProtocolBridge {

    public LoadGeneratingBridge(IMqttsnGatewayRuntimeRegistry registry, ProtocolBridgeDescriptor descriptor, ProtocolBridgeOptions options) {
        super(registry, descriptor, options);
    }

    @Override
    public IProtocolBridgeConnection createConnection() throws ProtocolBridgeException {
        return new AbstractProtocolBridgeConnection(options, descriptor, registry) {

            volatile boolean connected = false;
            volatile ScheduledFuture generator = null;

            @Override
            protected ConnectResult connectExternal(IClientIdentifierContext context) throws ProtocolBridgeException {
                connected = true;
                try {
                    return new ConnectResult(Result.STATUS.SUCCESS);
                } finally{
                    generator = ((IMqttsnGatewayRuntimeRegistry)registry).getProtocolBridgeService().schedulePolling(() -> {
                        try {
                           receiveExternal(context, options.getTopic(), 0,
                                   MqttsnUtils.randomBytes(25));
                        } catch(Exception e){
                            logger.error("error polling plc connection", e);
                        }
                    }, 5000, options.getInterval(), TimeUnit.MILLISECONDS);
                }
            }

            @Override
            protected DisconnectResult disconnectExternal(IClientIdentifierContext context) throws ProtocolBridgeException {
                try {
                    connected = false;
                    if(generator != null) generator.cancel(true);
                }
                catch(Exception e){
                    logger.error("error stopping generator", e);
                }
                finally {
                    return new DisconnectResult(Result.STATUS.SUCCESS);
                }
            }

            @Override
            protected SubscribeResult subscribeExternal(IClientIdentifierContext context, String topicPath, int grantedQoS) throws ProtocolBridgeException {
                return new SubscribeResult(Result.STATUS.SUCCESS);
            }

            @Override
            protected UnsubscribeResult unsubscribeExternal(IClientIdentifierContext context, String topicPath) throws ProtocolBridgeException {
                return new UnsubscribeResult(Result.STATUS.SUCCESS);
            }

            @Override
            protected PublishResult publishExternal(IClientIdentifierContext context, String topicPath, int QoS, byte[] payload) throws ProtocolBridgeException {
                return new PublishResult(Result.STATUS.SUCCESS);
            }

            @Override
            protected void receiveExternal(IClientIdentifierContext context, String topicPath, int QoS, byte[] payload) throws ProtocolBridgeException {

                super.receive(context, new TopicPath(topicPath), QoS, payload);
            }

            @Override
            public boolean isConnected() throws ProtocolBridgeException {
                return connected;
            }

            @Override
            public void close() {
                connected = false;
            }
        };
    }

    @Override
    public String getConnectionString() {
        return "";
    }
}
