package org.slj.mqtt.sn.gateway.impl.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.cloud.ProtocolBridgeDescriptor;
import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.bridge.IProtocolBridgeConnection;
import org.slj.mqtt.sn.gateway.spi.bridge.ProtocolBridgeException;
import org.slj.mqtt.sn.gateway.spi.bridge.ProtocolBridgeOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.model.ClientState;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.TopicPath;

public abstract class AbstractProtocolBridgeConnection
        implements IProtocolBridgeConnection {

    protected static Logger logger = LoggerFactory.getLogger(AbstractProtocolBridgeConnection.class.getName());

    protected IMqttsnRuntimeRegistry registry;
    protected IClientIdentifierContext context;
    protected ProtocolBridgeDescriptor descriptor;
    protected ProtocolBridgeOptions options;

    public AbstractProtocolBridgeConnection(ProtocolBridgeOptions options, ProtocolBridgeDescriptor descriptor, IMqttsnRuntimeRegistry registry) {
        this.registry = registry;
        this.descriptor = descriptor;
        this.options = options;
    }

    public ProtocolBridgeDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public DisconnectResult disconnect(IClientIdentifierContext context) throws ProtocolBridgeException {
        ISession session = null;
        try {
            DisconnectResult result = null;
            if((result = disconnectExternal(context)).getStatus() == Result.STATUS.SUCCESS) {
                session = registry.getSessionRegistry().getSession(context, false);
                if(session != null){
                    registry.getSessionRegistry().modifyClientState(session, ClientState.DISCONNECTED);
                }
            }
            return result;
        } catch(Exception e){
            if(session != null) {
                registry.getSessionRegistry().modifyClientState(session, ClientState.DISCONNECTED);
            }
            return new DisconnectResult(Result.STATUS.ERROR, "error while disconnecting bridge");
        }
    }

    @Override
    public ConnectResult connect(IClientIdentifierContext context) throws ProtocolBridgeException {

        ISession session = null;
        try {
            ConnectResult result = null;
            logger.info("about to connect bridge to context {}", context);
            if((result = connectExternal(context)).getStatus() == Result.STATUS.SUCCESS) {
                if(registry.getSessionRegistry().hasSession(context)){
                    registry.getSessionRegistry().cleanSession(context, true);
                }
                session = registry.getSessionRegistry().getSession(context, true);
                registry.getSessionRegistry().modifyClientState(session, ClientState.ACTIVE);
            }
            return result;
        } catch(Exception e){
            logger.error("error connecting bridge", e);
            if(session != null) {
                registry.getSessionRegistry().modifyClientState(session, ClientState.DISCONNECTED);
            }
            return new ConnectResult(Result.STATUS.ERROR, "error while connecting bridge");
        }
    }

    @Override
    public SubscribeResult subscribe(IClientIdentifierContext context, TopicPath topicPath) throws ProtocolBridgeException {
        try {
            String topic = topicPath.toString();
            logger.info("about to subscribe bridge to {} -> {}", context, topic);
            ISession session = getGatewaySessionAssertConnected(context);
            SubscribeResult result = null;
            if((result = subscribeExternal(context, topic, 2)).getStatus() == Result.STATUS.SUCCESS) {
                registry.getSubscriptionRegistry().subscribe(session, topic, 2);
            }
            return result;
        }catch(Exception e){
            logger.error("error subscribing bridge", e);
            return new SubscribeResult(Result.STATUS.ERROR, "error while subscribing bridge");
        }

    }

    @Override
    public UnsubscribeResult unsubscribe(IClientIdentifierContext context, TopicPath topicPath) throws ProtocolBridgeException {
        try {
            String topic = topicPath.toString();
            ISession session = getGatewaySessionAssertConnected(context);
            UnsubscribeResult result = null;
            if((result = unsubscribeExternal(context, topic)).getStatus() == Result.STATUS.SUCCESS) {
                registry.getSubscriptionRegistry().unsubscribe(session, topic);
            }
            return result;
        }catch(Exception e){
            return new UnsubscribeResult(Result.STATUS.ERROR, "error while unsubscribing bridge");
        }
    }

    @Override
    public PublishResult publish(IClientIdentifierContext context, TopicPath topicPath, int qos, boolean retained, byte[] payload) throws ProtocolBridgeException {

        try {
            String topic = topicPath.toString();
            ISession session = getGatewaySessionAssertConnected(context);
            PublishResult result = null;
            if((result = publishExternal(context, topic, qos, payload)).getStatus() == Result.STATUS.SUCCESS) {
                //do something?
            }
            return result;
        }catch(Exception e){
            return new PublishResult(Result.STATUS.ERROR, "error while unsubscribing bridge");
        }
    }

    public ReceiveResult receive(IClientIdentifierContext context, TopicPath topicPath, int QoS, byte[] data) throws ProtocolBridgeException {

        try {
            ISession session = getGatewaySessionAssertConnected(context);
            PublishResult result = ((IMqttsnGatewayRuntimeRegistry) registry).
                    getBackendService().publish(context, topicPath, QoS, false, data, null);
            return new ReceiveResult(result.getStatus(), result.getReturnCode(), result.getMessage());
        } catch(Exception e){
            return new ReceiveResult(Result.STATUS.ERROR, "error while receiving from bridge");
        }
    }

    @Override
    public boolean canAccept(IClientIdentifierContext context, TopicPath topicPath, byte[] payload) throws ProtocolBridgeException {
        return true;
    }


    protected ISession getGatewaySessionAssertConnected(IClientIdentifierContext context) throws ProtocolBridgeException {

        try {
            ISession session = registry.getSessionRegistry().getSession(context, false);
            if(session.getClientState() != ClientState.ACTIVE) {
                throw new ProtocolBridgeException("session not connected in gateway");
            }
            return session;
        } catch(MqttsnException e){
            throw new ProtocolBridgeException(e);
        }
    }

    protected abstract ConnectResult connectExternal(IClientIdentifierContext context) throws ProtocolBridgeException ;

    protected abstract DisconnectResult disconnectExternal(IClientIdentifierContext context) throws ProtocolBridgeException ;

    protected abstract SubscribeResult subscribeExternal(IClientIdentifierContext context, String topicPath, int grantedQoS) throws ProtocolBridgeException ;

    protected abstract UnsubscribeResult unsubscribeExternal(IClientIdentifierContext context, String topicPath) throws ProtocolBridgeException ;

    protected abstract PublishResult publishExternal(IClientIdentifierContext context, String topicPath, int QoS, byte[] payload) throws ProtocolBridgeException ;

    protected abstract void receiveExternal(IClientIdentifierContext context, String topicPath, int QoS, byte[] payload) throws ProtocolBridgeException ;
}
