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
import org.slj.mqtt.sn.PublishData;
import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewaySessionService;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.impl.AbstractMqttsnBackoffThreadService;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnClientState;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.model.TopicInfo;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.IMqttsnWillData;
import org.slj.mqtt.sn.model.session.impl.MqttsnQueuedPublishMessageImpl;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnIllegalFormatException;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.*;
import java.util.logging.Level;

public class MqttsnGatewaySessionService extends AbstractMqttsnBackoffThreadService
        implements IMqttsnGatewaySessionService {
    private static final int MIN_SESSION_MONITOR_CHECK = 30000;

    protected IMqttsnGatewayRuntimeRegistry getRegistry(){
        return (IMqttsnGatewayRuntimeRegistry) super.getRegistry();
    }

    @Override
    protected long doWork() {
        try {
            Iterator<IMqttsnSession> itr = getRegistry().getSessionRegistry().iterator();
            while(itr.hasNext()){
                IMqttsnSession session = itr.next();
                if(session == null) continue;

                //check keep alive timing
                if(session.getClientState() == MqttsnClientState.ACTIVE ||
                        session.getClientState() == MqttsnClientState.ASLEEP){
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
                        if(session.getClientState() == MqttsnClientState.ACTIVE){
                            try {
                                if(getRegistry().getMessageQueue().size(session) > 0){
                                    getRegistry().getMessageStateService().scheduleFlush(session.getContext());
                                }
                            } catch(MqttsnException e){
                                logger.log(Level.WARNING, "error scheduling flush", e);
                            }
                        }
                    }
                }
                else if(MqttsnUtils.in(session.getClientState(), MqttsnClientState.DISCONNECTED, MqttsnClientState.LOST)){
                    // check last seen time
                    long time = System.currentTimeMillis();
                    if(session.getSessionExpiryInterval() > 0 && //-- it may have literally just been initialised so if 0 ignore
                            session.getSessionExpiryInterval() < MqttsnConstants.UNSIGNED_MAX_32){
                        Date lastSeen = getLastSeen(session);
                        //TODO allow the % grace per the spec
                        long expires = lastSeen.getTime() + (session.getSessionExpiryInterval() * 1000);
                        //only expire sessions set to less than the max which means forever
                        if(expires < time){
                            logger.log(Level.WARNING, String.format("removing session [%s] state last seen [%s] > allowed [%s] seconds ago", session.getContext(), lastSeen, session.getSessionExpiryInterval()));
                            getRegistry().getSessionRegistry().clear(session);
                        }
                    } else if(session.getSessionExpiryInterval() == 0){
                        //TODO options should control whether to allow persist forever sessions
                        logger.log(Level.WARNING, String.format("detected session [%s] with expiry interval 0", session.getContext()));
                    }
                }
            }
        } catch(Exception e){
            logger.log(Level.SEVERE, String.format("error monitoring ongoing session state - handled;"), e);
        }
        return MIN_SESSION_MONITOR_CHECK;
    }

    protected static Date getLastSeen(IMqttsnSession state){
        Date lastSeen = state.getLastSeen();
        lastSeen = lastSeen == null ? state.getSessionStarted() : lastSeen;
        return lastSeen;
    }

    public void markSessionLost(IMqttsnSession session) {
        logger.log(Level.WARNING, String.format("session timeout or stale [%s], mark lost", session.getContext()));

        getRegistry().getSessionRegistry().modifyClientState(session, MqttsnClientState.LOST);

        if(getRegistry().getWillRegistry().hasWillMessage(session)){
            IMqttsnWillData data = getRegistry().getWillRegistry().getWillMessage(session);
            logger.log(Level.INFO, String.format("session expired or stale has will data to publish [%s]", data));
            IMqttsnMessage willPublish = getRegistry().getCodec().createMessageFactory().createPublish(data.getQos(), false, data.isRetained(),
                    "ab", data.getData());
            try {
                getRegistry().getBackendService().publish(session.getContext(), data.getTopicPath(), data.getQos(), data.isRetained(), data.getData(), willPublish);
                //per the MQTT spec, once published the will message should be discarded
                getRegistry().getWillRegistry().clear(session);
            } catch(MqttsnException e){
                logger.log(Level.SEVERE, String.format("error publish will message for [%s] -> [%s]", session.getContext(), data), e);
            }
        }
    }

    @Override
    public ConnectResult connect(IMqttsnSession session, IMqttsnMessage message) throws MqttsnException {

        String clientId = getRegistry().getCodec().getClientId(message);
        boolean cleanSession = getRegistry().getCodec().isCleanSession(message);
        long keepAlive = getRegistry().getCodec().getKeepAlive(message);

        ConnectResult result = null;
        result = checkSessionSize();
        if(result == null){
            synchronized (session.getContext()){
                try {
                    result = getRegistry().getBackendService().connect(session.getContext(), message);
                } finally {
                    if(result == null || !result.isError()){
                        //clear down all prior session state
                        notifyCluster(session.getContext());
                        getRegistry().getSessionRegistry().cleanSession(session.getContext(), cleanSession);
                        getRegistry().getSessionRegistry().modifyKeepAlive(session, (int) keepAlive);
                        getRegistry().getSessionRegistry().modifyClientState(session, MqttsnClientState.ACTIVE);
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
        logger.log(result.isError() ? Level.WARNING : Level.INFO, String.format("handled connection request for [%s] with cleanSession [%s] -> [%s], [%s]", session.getContext(), cleanSession, result.getStatus(), result.getMessage()));
        return result;
    }

    @Override
    public DisconnectResult disconnect(IMqttsnSession session, IMqttsnMessage message) throws MqttsnException {
        DisconnectResult result = null;
        synchronized (session.getContext()){

            long duration = getRegistry().getCodec().getDuration(message);
            result = getRegistry().getBackendService().disconnect(session.getContext(), message);
            if(!result.isError()){
                if(duration > 0){
                    logger.log(Level.INFO, String.format("[%s] setting client state asleep for [%s]", session.getContext(), duration));

                    //TODO - the gateway should use the sei for sleep monitoring
                    getRegistry().getSessionRegistry().modifyKeepAlive(session, (int) duration);
                    getRegistry().getSessionRegistry().modifySessionExpiryInterval(session, duration);
                    getRegistry().getSessionRegistry().modifyClientState(session, MqttsnClientState.ASLEEP);
                    getRegistry().getTopicRegistry().clear(session,
                            getRegistry().getOptions().isSleepClearsRegistrations());
                } else {
                    logger.log(Level.INFO, String.format("[%s] disconnecting client", session.getContext()));
                    getRegistry().getSessionRegistry().modifyClientState(session, MqttsnClientState.DISCONNECTED);
                }
            }
        }
        return result;
    }

    @Override
    public SubscribeResult subscribe(IMqttsnSession session, TopicInfo info, IMqttsnMessage message)
            throws MqttsnException {

        IMqttsnContext context = session.getContext();
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
                    logger.log(Level.WARNING, String.format("error in topic format"), e);
                    result = new SubscribeResult(Result.STATUS.ERROR, MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID, "invalid topic format");
                }
                return result;
            }
        }
    }

    @Override
    public UnsubscribeResult unsubscribe(IMqttsnSession session, TopicInfo info, IMqttsnMessage message) throws MqttsnException {

        IMqttsnContext context = session.getContext();
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
    public RegisterResult register(IMqttsnSession session, String topicPath) throws MqttsnException {

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

    public void notifyCluster(IMqttsnContext context) throws MqttsnException {
        if(getRegistry().getGatewayClusterService() != null){
            getRegistry().getGatewayClusterService().notifyConnection(context);
        }
    }

    protected ConnectResult checkSessionSize(){
        int maxConnectedClients = ((MqttsnGatewayOptions) registry.getOptions()).getMaxConnectedClients();
        if(getRegistry().getSessionRegistry().countTotalSessions() >= maxConnectedClients){
            return new ConnectResult(Result.STATUS.ERROR, MqttsnConstants.RETURN_CODE_REJECTED_CONGESTION,
                    "gateway has reached capacity");
        }
        return null;
    }

    @Override
    public void receiveToSessions(String topicPath, int qos, boolean retained, byte[] payload) throws MqttsnException {
        //-- expand the message onto the gateway connected device queues
        Set<IMqttsnContext> recipients = null;
        try {
            recipients = getRegistry().getSubscriptionRegistry().matches(topicPath);
        } catch(MqttsnIllegalFormatException e){
            throw new MqttsnException("illegal format supplied", e);
        }

        logger.log(Level.FINE, String.format("receiving broker side message into [%s] sessions", recipients.size()));

        //if we only have 1 receiver remove message after read
        UUID messageId = recipients.size() > 1 ?
                getRegistry().getMessageRegistry().add(payload, calculateExpiry()) :
                getRegistry().getMessageRegistry().add(payload, true) ;

        int successfulExpansion = 0;
        for (IMqttsnContext context : recipients){
            try {
                IMqttsnSession session = getRegistry().getSessionRegistry().getSession(context, false);
                if(session != null){
                    if(session.getMaxPacketSize() != 0 &&
                            payload.length + 9 > session.getMaxPacketSize()){
                        logger.log(Level.WARNING, String.format("payload exceeded max size (%s) bytes configured by client, ignore this client [%s]", payload.length, context));
                    } else {

                        int grantedQos = registry.getSubscriptionRegistry().getQos(session, topicPath);
                        int q = Math.min(grantedQos,qos);

                        PublishData data = new PublishData(topicPath, q, retained);
                        try {
                            registry.getMessageQueue().offer(session, new MqttsnQueuedPublishMessageImpl(messageId, data));
                            successfulExpansion++;
                        } catch(MqttsnQueueAcceptException e){
                            //-- the queue was full nothing to be done here
                        }
                    }
                } else {
                    logger.log(Level.WARNING, String.format("detected <null> session state for subscription (%s)", context));
                }
            } catch(MqttsnException e){
                logger.log(Level.WARNING, String.format("detected subscription issue for session receipt.. ignore client (%s)", context));
            }
        }
        if(successfulExpansion == 0){
            registry.getMessageRegistry().remove(messageId);
        }
    }

    protected Date calculateExpiry(){
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.YEAR, 1);
        return cal.getTime();
    }

    @Override
    protected String getDaemonName() {
        return "gateway-session";
    }
}
