package org.slj.mqtt.sn.plc;

import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.messages.PlcSubscriptionRequest;
import org.apache.plc4x.java.api.messages.PlcSubscriptionResponse;
import org.apache.plc4x.java.api.model.PlcField;
import org.apache.plc4x.java.api.model.PlcSubscriptionHandle;
import org.slj.mqtt.sn.cloud.ProtocolBridgeDescriptor;
import org.slj.mqtt.sn.gateway.impl.bridge.AbstractProtocolBridgeConnection;
import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.bridge.IProtocolBridgeConnection;
import org.slj.mqtt.sn.gateway.spi.bridge.ProtocolBridgeException;
import org.slj.mqtt.sn.gateway.spi.bridge.ProtocolBridgeOptions;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.utils.Pair;
import org.slj.mqtt.sn.utils.StringTable;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.Collection;
import java.util.List;

public class PLX4JConnection extends AbstractProtocolBridgeConnection implements IProtocolBridgeConnection {

    protected PlcDriverManager plcDriverManager;
    protected PlcConnection plcConnection;
    protected IClientIdentifierContext context;


    public PLX4JConnection(PlcDriverManager plcDriverManager, ProtocolBridgeOptions options, IMqttsnRuntimeRegistry registry, ProtocolBridgeDescriptor descriptor)  {
        super(options, descriptor, registry);
        this.plcDriverManager = plcDriverManager;
    }

    protected String createConnectionString(ProtocolBridgeOptions options){
        //opcua:tcp://Simons-Laptop.broadband:53530/OPCUA/SimulationServer
        return String.format("%s://%s:%s/%s",
                options.getProtocol(),
                options.getHostName(),
                options.getPort(),
                options.getResourcePath());
    }

    @Override
    protected synchronized ConnectResult connectExternal(IClientIdentifierContext context) throws ProtocolBridgeException {

        try {
            if(plcConnection == null || !plcConnection.isConnected()){
                this.context = context;
                String connectionString = createConnectionString(options);
                logger.info("connecting via plx4j to {}", connectionString);
                plcConnection = plcDriverManager.getConnection(connectionString);
                plcConnection.connect();
            }
            return new ConnectResult(Result.STATUS.SUCCESS);
        } catch(Exception e){
            throw new ProtocolBridgeException(e);
        }
    }

    @Override
    protected synchronized DisconnectResult disconnectExternal(IClientIdentifierContext context) throws ProtocolBridgeException {
        try {
            if(plcConnection != null && plcConnection.isConnected()){
                plcConnection.close();
            }
            return new DisconnectResult(Result.STATUS.SUCCESS);
        } catch(Exception e){
            throw new ProtocolBridgeException(e);
        }
    }

    @Override
    protected SubscribeResult subscribeExternal(final IClientIdentifierContext context, final String topicPath, final int grantedQoS)
            throws ProtocolBridgeException {

        try {
            if(plcConnection != null && !plcConnection.isConnected()){
                throw new ProtocolBridgeException("unable to perform operation on disconnected bridge");
            }

            if (!plcConnection.getMetadata().canSubscribe()) {
                return new SubscribeResult(Result.STATUS.ERROR, "connection doesn't support subscriptions");
            }

            final PlcSubscriptionRequest.Builder builder = plcConnection.subscriptionRequestBuilder();
            builder.addChangeOfStateField("FieldValue", topicPath);

            PlcSubscriptionRequest subscriptionRequest = builder.build();
            final PlcSubscriptionResponse subscriptionResponse =
                    subscriptionRequest.execute().get();

            for (String subscriptionName : subscriptionResponse.getFieldNames()) {
                final PlcSubscriptionHandle subscriptionHandle =
                        subscriptionResponse.getSubscriptionHandle(subscriptionName);
                subscriptionHandle.register(
                        event -> {
                            List<Pair<String, byte[]>> l = PLX4JUtils.getDataFromSubscriptionEvent(event);
                            for(Pair<String, byte[]> p : l){
                                if(p.getRight() != null && p.getRight().length > 0){
                                    try {
                                        logger.info("received {} from OPCUA -> {}", p.getLeft(),
                                                p.getRight().length);
                                        receiveExternal(context, topicPath, grantedQoS, p.getRight());
                                    } catch (Exception e) {
                                        logger.error("error receiving bytes from OPCUA -> {}", topicPath, e);
                                    }
                                }
                            }
                        }
                );
            }
            return new SubscribeResult(Result.STATUS.SUCCESS);

        } catch(Exception e){
            throw new ProtocolBridgeException(e);
        }
    }

    @Override
    protected UnsubscribeResult unsubscribeExternal(IClientIdentifierContext context, String topicPath) throws ProtocolBridgeException {
        return null;
    }

    @Override
    protected PublishResult publishExternal(IClientIdentifierContext context, String topicPath, int QoS, byte[] payload) throws ProtocolBridgeException {
        return null;
    }

    @Override
    protected void receiveExternal(IClientIdentifierContext context, String topicPath, int QoS, byte[] payload) throws ProtocolBridgeException {
        super.receive(context, new TopicPath(topicPath), QoS, payload);
    }

    @Override
    public boolean isConnected() throws ProtocolBridgeException {
        return plcConnection != null && plcConnection.isConnected();
    }

    @Override
    public synchronized void close() {
        try {
            if(plcConnection != null &&
                    plcConnection.isConnected()){
                plcConnection.close();
            }
        } catch(Exception e){

        }
    }


    private static byte[] convert(Byte[] oBytes) {
        byte[] bytes = new byte[oBytes.length];
        for(int i = 0; i < oBytes.length; i++) {
            bytes[i] = oBytes[i];
        }
        return bytes;
    }
}
