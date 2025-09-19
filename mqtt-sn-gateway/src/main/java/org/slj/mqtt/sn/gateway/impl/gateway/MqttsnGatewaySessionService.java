/*
 * Copyright (c) 2021 Simon Johnson <simon622 AT gmail DOT com>
 *
 * Find me on GitHub:
 * https://github.com/simon622
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.slj.mqtt.sn.gateway.impl.gateway;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewaySessionService;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.impl.AbstractMqttsnBackoffThreadService;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.ClientState;
import org.slj.mqtt.sn.model.IMqttsnMessageContext;
import org.slj.mqtt.sn.model.TopicInfo;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.model.session.IWillData;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnIllegalFormatException;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.TopicPath;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnConnect;
import org.slj.mqtt.sn.wire.version2_0.payload.MqttsnConnect_V2_0;

import java.util.Date;
import java.util.Iterator;

public class MqttsnGatewaySessionService extends AbstractMqttsnBackoffThreadService
        implements IMqttsnGatewaySessionService {
    private static final int MIN_SESSION_MONITOR_CHECK = 30000;

    protected IMqttsnGatewayRuntimeRegistry getRegistry(){
        return (IMqttsnGatewayRuntimeRegistry) super.getRegistry();
    }

    @Override
    protected long doWork() {
        try {
            Iterator<ISession> itr = getRegistry().getSessionRegistry().iterator();
            while(itr.hasNext()){
                ISession session = itr.next();
                if(session == null) continue;

                //check keep alive timing
                if(session.getClientState() == ClientState.ACTIVE ||
                        session.getClientState() == ClientState.ASLEEP){
                    long time = System.currentTimeMillis();
                    if(session.getKeepAlive() > 0){
                        Date lastSeen = getLastSeen(session);
                        long expires = lastSeen.getTime() + (int) ((session.getKeepAlive() * 1000) * 1.5);
                        if(expires < time){
                            markSessionLost(session);
                        }
                    } else {
                        //This is a condition in case the sender was blocked when it attempted to send a message,
                        //we need something to ensure devices don't get stuck in the stale state (ie. ready to receive with messages
                        //in the queue but noone doing the work - this won't be needed 99% of the time
                        if(session.getClientState() == ClientState.ACTIVE){
                            try {
                                if(getRegistry().getMessageQueue().queueSize(session) > 0){
                                    getRegistry().getMessageStateService().scheduleFlush(session.getContext());
                                }
                            } catch(MqttsnException e){
                                logger.warn("error scheduling flush", e);
                            }
                        }
                    }
                }
                else if(MqttsnUtils.in(session.getClientState(), ClientState.DISCONNECTED, ClientState.LOST)){
                    // check last seen time
                    long time = System.currentTimeMillis();
                    Date lastSeen = getLastSeen(session);

                    long expires;
                    long expiresConfig;

                    if(MqttsnGatewayOptions.GATEWAY_MAX_SESSION_EXPIRY_INTERNAL > 0){
                        expiresConfig = session.getSessionExpiryInterval() > 0 ? Math.min(MqttsnGatewayOptions.GATEWAY_MAX_SESSION_EXPIRY_INTERNAL, session.getSessionExpiryInterval()) :
                                MqttsnGatewayOptions.GATEWAY_MAX_SESSION_EXPIRY_INTERNAL;
                    } else {
                        expiresConfig = session.getSessionExpiryInterval();
                    }

                    if(expiresConfig > 0){
                        expires = lastSeen.getTime() + (expiresConfig * 1000L);

                        //only expire sessions set to less than the max which means forever
                        if(expires < time){
                            logger.warn("removing session {} state last seen {} > allowed {} seconds ago", session.getContext(), lastSeen, expiresConfig);
                            getRegistry().getSessionRegistry().clear(session);
                        }
                    }
                }
            }
        } catch(Exception e){
            logger.error("error monitoring ongoing session state - handled;", e);
        }
        return MIN_SESSION_MONITOR_CHECK;
    }

    protected static Date getLastSeen(ISession state){
        Date lastSeen = state.getLastSeen();
        lastSeen = lastSeen == null ? state.getSessionStarted() : lastSeen;
        return lastSeen;
    }

    public void markSessionLost(ISession session) {
        logger.warn("session timeout or stale {}, mark lost", session.getContext());

        getRegistry().getSessionRegistry().modifyClientState(session, ClientState.LOST);

        try {
            IMqttsnMessage disconnect = getRegistry().getMessageFactory().createDisconnect(
                    MqttsnConstants.RETURN_CODE_REJECTED_CONGESTION, "Session LOST to timeout");
            getRegistry().getTransportLocator().writeToTransport(
                    getRegistry().getNetworkRegistry().getContext(session.getContext()), disconnect);
        } catch(Exception e){
            logger.warn("unable to send disconnect to timeout {}", e.getMessage());
        }


        if(getRegistry().getWillRegistry().hasWillMessage(session)){
            IWillData data = getRegistry().getWillRegistry().getWillMessage(session);
            logger.info("session expired or stale has will data to publish {}", data);
            IMqttsnMessage willPublish = getRegistry().getCodec().createMessageFactory().createPublish(data.getQos(), false, data.isRetained(),
                    "ab", data.getData());
            try {
                getRegistry().getBackendService().publish(session.getContext(), data.getTopicPath(), data.getQos(), data.isRetained(), data.getData(), willPublish);
                //per the MQTT spec, once published the will message should be discarded
                getRegistry().getWillRegistry().clear(session);
            } catch(MqttsnException e){
                logger.error("error publish will message for {} -> {}", session.getContext(), data, e);
            }
        }
    }

    @Override
    public ConnectResult connect(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {

        String clientId = null;
        boolean cleanStart = false;
        int keepAlive = 0;
        boolean will = false;
        long sessionExpiryInterval = MqttsnConstants.UNSIGNED_MAX_32;
        int maxPacketSize = MqttsnConstants.UNSIGNED_MAX_16;
        int protocolVersion = MqttsnConstants.PROTOCOL_VERSION_UNKNOWN;

        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnConnect connectMessage = (MqttsnConnect) message ;
            clientId = connectMessage.getClientId();
            cleanStart = connectMessage.isCleanSession();
            keepAlive = connectMessage.getDuration();
            will = connectMessage.isWill();
            protocolVersion = MqttsnConstants.PROTOCOL_VERSION_1_2;
        }
        else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnConnect_V2_0 connectMessage = (MqttsnConnect_V2_0) message ;
            clientId = connectMessage.getClientId();
            cleanStart = connectMessage.isCleanStart();
            will = connectMessage.isWill();
            keepAlive = connectMessage.getKeepAlive();
            sessionExpiryInterval = connectMessage.getSessionExpiryInterval();
            maxPacketSize = connectMessage.getMaxPacketSize();
            protocolVersion = MqttsnConstants.PROTOCOL_VERSION_2_0;
        }

        boolean cleanSession = getRegistry().getCodec().isCleanSession(message);

        ISession session = null;
        ConnectResult result = null;
        result = checkSessionSize();
        if(result == null){

            boolean existed = registry.getSessionRegistry().hasSession(context.getClientContext());
            synchronized (context){
                try {
                    session = createNewSession(context);
                    result = getRegistry().getBackendService().connect(session.getContext(), message);
                } finally {
                    if(result == null || !result.isError()){
                        //clear down all prior session state
                        long sessionExpiryIntervalRequested = sessionExpiryInterval;
                        if(sessionExpiryIntervalRequested >
                                registry.getOptions().getSessionExpiryInterval()){
                            sessionExpiryInterval = Math.min(sessionExpiryInterval,
                                    registry.getOptions().getSessionExpiryInterval());
                            result.setSessionExpiryInterval(sessionExpiryInterval);
                        }

                        notifyCluster(session.getContext());
                        getRegistry().getSessionRegistry().cleanSession(session.getContext(), cleanSession);
                        getRegistry().getSessionRegistry().modifyKeepAlive(session, (int) keepAlive);
                        getRegistry().getSessionRegistry().modifyClientState(session, ClientState.ACTIVE);
                        getRegistry().getSessionRegistry().modifySessionExpiryInterval(session, sessionExpiryInterval);
                        getRegistry().getSessionRegistry().modifyMaxPacketSize(session, maxPacketSize);
                        getRegistry().getSessionRegistry().modifyProtocolVersion(session, protocolVersion);

                        result.setSessionExisted(existed);
                        result.setAssignedClientIdentifier(clientId == null ? context.getClientContext().getId() : null);
                        result.setKeepAlive(keepAlive);
                        result.setProtocolVersion(protocolVersion);
                        result.setMaxPacketSize(maxPacketSize);
                    }
                }
            }
        }
        if(result.isError()){
            //-- connect was not successful ensure we
            //-- do not hold a reference to any session (but leave network to enable the CONNACK to go back - clean up the network after the response)
//            clear(session.getContext(), true, false);
            getRegistry().getSessionRegistry().clear(session, false);
        }

        if(result.isError()){
            logger.error("handled connection request for {} with cleanSession {} -> {}, {}", session.getContext(), cleanSession, result.getStatus(), result.getMessage());
        } else {
            logger.info("handled connection request for {} with cleanSession {} -> {}, {}", session.getContext(), cleanSession, result.getStatus(), result.getMessage());
        }
        return result;
    }

    @Override
    public DisconnectResult disconnect(ISession session, IMqttsnMessage message) throws MqttsnException {
        DisconnectResult result = null;
        synchronized (session.getContext()){

            long duration = getRegistry().getCodec().getDuration(message);
            result = getRegistry().getBackendService().disconnect(session.getContext(), message);
            if(!result.isError()){
                if(duration > 0){
                    logger.info("{} setting client state asleep for {}", session.getContext(), duration);

                    //TODO - the gateway should use the sei for sleep monitoring
                    getRegistry().getSessionRegistry().modifyKeepAlive(session, (int) duration);
                    getRegistry().getSessionRegistry().modifySessionExpiryInterval(session, duration);
                    getRegistry().getSessionRegistry().modifyClientState(session, ClientState.ASLEEP);
                    getRegistry().getTopicRegistry().clear(session,
                            getRegistry().getOptions().isSleepClearsRegistrations());
                } else {
                    logger.info("{} disconnecting client", session.getContext());
                    getRegistry().getSessionRegistry().modifyClientState(session, ClientState.DISCONNECTED);
                }
            }
        }
        return result;
    }

    @Override
    public SubscribeResult subscribe(ISession session, TopicInfo info, IMqttsnMessage message)
            throws MqttsnException {

        IClientIdentifierContext context = session.getContext();
        synchronized (context){
            int QoS = getRegistry().getCodec().getQoS(message, true);
            String topicPath = null;
            if(info.getType() == MqttsnConstants.TOPIC_TYPE.PREDEFINED){
                topicPath = registry.getTopicRegistry().lookupPredefined(session, info.getTopicId());
                info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.PREDEFINED, info.getTopicId());
            } else {
                topicPath = info.getTopicPath();
                if(!MqttsnSpecificationValidator.isValidSubscriptionTopic(topicPath)){
                    return new SubscribeResult(Result.STATUS.ERROR, MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID,
                            "invalid topic format");
                }
                if(!TopicPath.isWild(topicPath)){
                    TopicInfo lookupInfo = registry.getTopicRegistry().lookup(session, topicPath);
                    if(lookupInfo == null || info.getType() == MqttsnConstants.TOPIC_TYPE.NORMAL){
                        info = registry.getTopicRegistry().register(session, topicPath);
                    }
                } else {
                    info = TopicInfo.WILD;
                }
            }

            if(topicPath == null){
                //-- topic could not be found to lookup
                return new SubscribeResult(Result.STATUS.ERROR, MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID,
                        "no topic found by specification");

            } else {
                if(getRegistry().getAuthorizationService() != null){
                    if(!getRegistry().getAuthorizationService().allowedToSubscribe(context, topicPath)){
                        return new SubscribeResult(Result.STATUS.ERROR, MqttsnConstants.RETURN_CODE_REJECTED_CONGESTION,
                                "authorization service denied subscription");
                    }
                    QoS = Math.min(getRegistry().getAuthorizationService().allowedMaximumQoS(context, topicPath), QoS);
                }

                //-- ensure we call subscribe on the backend first - else the aggreating gw will never know we need to subscribe
                SubscribeResult result = getRegistry().
                        getBackendService().subscribe(context, new TopicPath(topicPath), message);

                try {
                    if(getRegistry().getSubscriptionRegistry().subscribe(session, topicPath, QoS)){
                        result.setTopicInfo(info);
                        result.setGrantedQoS(QoS);
                    } else {
                        result = new SubscribeResult(Result.STATUS.NOOP);
                        result.setTopicInfo(info);
                        result.setGrantedQoS(QoS);
                    }
                } catch(MqttsnIllegalFormatException e){
                    logger.warn("error in topic format", e);
                    result = new SubscribeResult(Result.STATUS.ERROR, MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID, "invalid topic format");
                }
                return result;
            }
        }
    }

    @Override
    public UnsubscribeResult unsubscribe(ISession session, TopicInfo info, IMqttsnMessage message) throws MqttsnException {

        IClientIdentifierContext context = session.getContext();
        synchronized (context){
            String topicPath = null;
            if(info.getType() == MqttsnConstants.TOPIC_TYPE.PREDEFINED){
                topicPath = registry.getTopicRegistry().lookupPredefined(session, info.getTopicId());
                info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.PREDEFINED, info.getTopicId());
            } else {
                topicPath = info.getTopicPath();
                if(!MqttsnSpecificationValidator.isValidSubscriptionTopic(topicPath)){
                    return new UnsubscribeResult(Result.STATUS.ERROR, MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID,
                            "invalid topic format");
                }
                if(!TopicPath.isWild(topicPath)){
                    TopicInfo lookupInfo = registry.getTopicRegistry().lookup(session, topicPath);
                    if(lookupInfo == null || info.getType() == MqttsnConstants.TOPIC_TYPE.NORMAL){
                        info = registry.getTopicRegistry().register(session, topicPath);
                    }
                } else {
                    info = TopicInfo.WILD;
                }
            }

            if(topicPath == null){
                //-- topic could not be found to lookup
                return new UnsubscribeResult(Result.STATUS.ERROR, MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID,
                        "no topic found by specification");
            } else {
                if(registry.getSubscriptionRegistry().unsubscribe(session, topicPath)){
                    UnsubscribeResult result = getRegistry().getBackendService().unsubscribe(context, new TopicPath(topicPath), message);
                    return result;
                } else {
                    return new UnsubscribeResult(Result.STATUS.NOOP);
                }
            }
        }
    }

    @Override
    public RegisterResult register(ISession session, String topicPath) throws MqttsnException {

        if(!MqttsnSpecificationValidator.isValidSubscriptionTopic(topicPath)){
            return new RegisterResult(Result.STATUS.ERROR, MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID, "invalid topic format");
        }
        synchronized (session.getContext()){
            TopicInfo info;
            if(!TopicPath.isWild(topicPath)){
                info = registry.getTopicRegistry().lookup(session, topicPath);
                if(info == null){
                    info = registry.getTopicRegistry().register(session, topicPath);
                }
            } else {
                info = TopicInfo.WILD;
            }
            return new RegisterResult(topicPath, info);
        }
    }

    public void notifyCluster(IClientIdentifierContext context) throws MqttsnException {
        if(getRegistry().getGatewayClusterService() != null){
            getRegistry().getGatewayClusterService().notifyConnection(context);
        }
    }

    protected ConnectResult checkSessionSize(){
        int maxConnectedClients = ((MqttsnGatewayOptions) registry.getOptions()).getMaxClientSessions();
        if(getRegistry().getSessionRegistry().countTotalSessions() >= maxConnectedClients){
            return new ConnectResult(Result.STATUS.ERROR, MqttsnConstants.RETURN_CODE_REJECTED_CONGESTION,
                    "gateway has reached capacity");
        }
        return null;
    }

    protected ISession createNewSession(IMqttsnMessageContext context) throws MqttsnException {
        //-- THIS IS WHERE A SESSION IS CREATED ON THE GATEWAY SIDE
        boolean stateExisted = context.getSession() != null;
        //ensure we update the context so the session is present for the rest of the message processing
        ISession session = getRegistry().getSessionRegistry().getSession(context.getClientContext(), true);
        if(!stateExisted){
            context.setSession(session);
        }
        return session;
    }

    @Override
    protected String getDaemonName() {
        return "gateway-session";
    }
}
