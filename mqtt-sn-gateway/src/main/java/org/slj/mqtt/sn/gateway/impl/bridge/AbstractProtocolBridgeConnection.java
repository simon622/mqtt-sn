package org.slj.mqtt.sn.gateway.impl.bridge;

import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.bridge.IProtocolBridgeConnection;
import org.slj.mqtt.sn.gateway.spi.bridge.ProtocolBridgeException;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.utils.TopicPath;

public abstract class AbstractProtocolBridgeConnection
        implements IProtocolBridgeConnection {

    protected IMqttsnRuntimeRegistry registry;

    public AbstractProtocolBridgeConnection(IMqttsnRuntimeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean isConnected() throws ProtocolBridgeException {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public DisconnectResult disconnect(IClientIdentifierContext context) throws ProtocolBridgeException {
        return null;
    }

    @Override
    public ConnectResult connect(IClientIdentifierContext context) throws ProtocolBridgeException {

        try {
//            registry.get
        } catch(Exception e){

        }
        return null;
    }

    @Override
    public SubscribeResult subscribe(IClientIdentifierContext context, TopicPath topicPath) throws ProtocolBridgeException {
        return null;
    }

    @Override
    public UnsubscribeResult unsubscribe(IClientIdentifierContext context, TopicPath topicPath) throws ProtocolBridgeException {
        return null;
    }

    @Override
    public PublishResult publish(IClientIdentifierContext context, TopicPath topicPath, int qos, boolean retained, byte[] payload) throws ProtocolBridgeException {
        return null;
    }

    @Override
    public boolean canAccept(IClientIdentifierContext context, TopicPath topicPath, byte[] payload) throws ProtocolBridgeException {
        return true;
    }


    protected abstract ConnectResult connectExternal(IClientIdentifierContext context) throws ProtocolBridgeException ;
}
