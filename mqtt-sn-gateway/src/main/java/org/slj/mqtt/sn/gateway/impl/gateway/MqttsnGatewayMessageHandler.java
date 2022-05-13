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
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.impl.AbstractMqttsnMessageHandler;
import org.slj.mqtt.sn.model.*;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.version1_2.payload.*;
import org.slj.mqtt.sn.wire.version2_0.payload.*;

import java.util.logging.Level;

public class MqttsnGatewayMessageHandler
        extends AbstractMqttsnMessageHandler<IMqttsnGatewayRuntimeRegistry> {

    protected IMqttsnSessionState getSessionState(IMqttsnContext context) throws MqttsnException, MqttsnInvalidSessionStateException {
        IMqttsnSessionState state = registry.getGatewaySessionService().getSessionState(context, false);
        if(state == null || state.getClientState() == MqttsnClientState.DISCONNECTED)
            throw new MqttsnInvalidSessionStateException("session not available for context");
        return state;
    }

    protected IMqttsnSessionState getSessionState(IMqttsnContext context, boolean createIfNotExists) throws MqttsnException {
        IMqttsnSessionState state = registry.getGatewaySessionService().getSessionState(context, createIfNotExists);
        return state;
    }

    protected boolean hasSessionState(IMqttsnContext context) throws MqttsnException {
        IMqttsnSessionState state = registry.getGatewaySessionService().getSessionState(context, false);
        return state != null;
    }

    @Override
    protected void beforeHandle(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {

        try {
            //see if the client has an active sessionx
            getSessionState(context);
        } catch(MqttsnInvalidSessionStateException e){

            //if they do NOT, the only time we can process messages
            //on their behalf is if its a CONNECT or a PUBLISH M 1
            boolean shouldContinue = false;
            if(message instanceof IMqttsnConnectPacket){
                //this is ok
                shouldContinue = true;
            } else if(message instanceof IMqttsnPublishPacket){
                IMqttsnPublishPacket p = (IMqttsnPublishPacket) message;
                if(p.getQoS() == MqttsnConstants.QoSM1){
                    //this is ok
                    shouldContinue = true;
                }
            } else if(message instanceof IMqttsnDisconnectPacket){
                shouldContinue = true;
            }
            if(!shouldContinue){
                logger.log(Level.WARNING, String.format("detected invalid client session state for [%s] and inbound message [%s]", context, message));
                throw new MqttsnException(e);
            }
        }
    }

    @Override
    protected void afterHandle(IMqttsnContext context, IMqttsnMessage messageIn, IMqttsnMessage messageOut) throws MqttsnException {

        super.afterHandle(context, messageIn, messageOut);

        try {
            IMqttsnSessionState sessionState = getSessionState(context);
            if(sessionState != null){
                registry.getGatewaySessionService().updateLastSeen(sessionState);
            }
        } catch(MqttsnInvalidSessionStateException e){
            //-- a disconnect will mean theres no session to update
        }
    }

    @Override
    protected void afterResponse(IMqttsnContext context, IMqttsnMessage messageIn, IMqttsnMessage messageOut) throws MqttsnException {

        try {

            if(messageOut != null && messageOut.isErrorMessage() &&
                    registry.getCodec().isConnect(messageIn)){
                //-- this is an error in CONNECT - remove from network registry so we dont leak
                logger.log(Level.WARNING, String.format("connect for [%s] was rejected, tidy up network layer after response is sent", context));
                registry.getNetworkRegistry().removeExistingClientId(context.getId());
            }

            IMqttsnSessionState sessionState = getSessionState(context);
            if(sessionState != null){
                //active session means we can try and see if there is anything to flush here if its a terminal message
                if(messageIn != null && isTerminalMessage(messageIn) && !messageIn.isErrorMessage() ||
                        messageOut != null && isTerminalMessage(messageOut) && !messageOut.isErrorMessage() ){
                    if(MqttsnUtils.in(sessionState.getClientState(),
                            MqttsnClientState.CONNECTED, MqttsnClientState.AWAKE)) {
                        if(logger.isLoggable(Level.FINE)){
                            logger.log(Level.FINE, String.format("scheduling flush based on outbound message [%s] -> inflight [%s]", messageOut == null ? messageIn : messageOut, getRegistry().getMessageStateService().countInflight(context, InflightMessage.DIRECTION.SENDING)));
                        }
                        registry.getMessageStateService().scheduleFlush(context);
                    }
                }
            }
        } catch(MqttsnInvalidSessionStateException e){
            //-- a disconnect will mean theres no session to update
        }
    }

    @Override
    protected IMqttsnMessage handleConnect(IMqttsnContext context, IMqttsnMessage connect) throws MqttsnException, MqttsnCodecException {

        String clientId = null;
        boolean cleanStart = false;
        int keepAlive = 0;
        boolean will = false;
        long sessionExpiryInterval = MqttsnConstants.UNSIGNED_MAX_32;
        int maxPacketSize = MqttsnConstants.UNSIGNED_MAX_16;

        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnConnect connectMessage = (MqttsnConnect) connect ;
            clientId = connectMessage.getClientId();
            cleanStart = connectMessage.isCleanSession();
            keepAlive = connectMessage.getDuration();
            will = connectMessage.isWill();
        }
        else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnConnect_V2_0 connectMessage = (MqttsnConnect_V2_0) connect ;
            clientId = connectMessage.getClientId();
            cleanStart = connectMessage.isCleanStart();
            will = connectMessage.isWill();
            keepAlive = connectMessage.getKeepAlive();
            sessionExpiryInterval = connectMessage.getSessionExpiryInterval();
            maxPacketSize = connectMessage.getMaxPacketSize();
        }


        if(registry.getAuthenticationService() != null){
            if(!registry.getAuthenticationService().allowConnect(context, clientId)){
                logger.log(Level.WARNING, String.format("authentication service rejected client [%s]", clientId));
                return registry.getMessageFactory().createConnack(MqttsnConstants.RETURN_CODE_SERVER_UNAVAILABLE);
            }
        }

        String assignedClientId = context.isAssignedClientId() ? context.getId() : null;
        IMqttsnSessionState state = getSessionState(context, true);
        ConnectResult result = registry.getGatewaySessionService().connect(state, connect);
        processSessionResult(result);
        if(result.isError()){
            return registry.getMessageFactory().createConnack(result.getReturnCode());
        }
        else {
            if(will){
                return registry.getMessageFactory().createWillTopicReq();
            } else {
                boolean stateExisted = hasSessionState(context);
                long sessionExpiryIntervalRequested = sessionExpiryInterval;
                if(sessionExpiryInterval >
                        registry.getOptions().getRemoveDisconnectedSessionsSeconds()){
                    sessionExpiryInterval = Math.min(sessionExpiryInterval,
                            registry.getOptions().getRemoveDisconnectedSessionsSeconds());
                }
                boolean changedFromRequested = sessionExpiryIntervalRequested != sessionExpiryInterval;
                state.setSessionExpiryInterval(sessionExpiryInterval);
                state.setMaxPacketSize(maxPacketSize);
                return registry.getMessageFactory().createConnack(
                        result.getReturnCode(), stateExisted, assignedClientId, changedFromRequested ? sessionExpiryInterval : 0);
            }
        }
    }

    @Override
    protected IMqttsnMessage handleDisconnect(IMqttsnContext context, IMqttsnMessage initialDisconnect, IMqttsnMessage receivedDisconnect) throws MqttsnException, MqttsnCodecException, MqttsnInvalidSessionStateException {

        long keepAlive = 0;
        String reasonString = null;

        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnDisconnect d = (MqttsnDisconnect) receivedDisconnect;
            keepAlive = d.getDuration();
        }
        else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnDisconnect_V2_0 d = (MqttsnDisconnect_V2_0) receivedDisconnect ;
            keepAlive = d.getSessionExpiryInterval();
            reasonString = d.getReasonString();
        }

        IMqttsnSessionState state = getSessionState(context, false);
        boolean needsResponse = initialDisconnect != null ||
                (state != null && !MqttsnUtils.in(state.getClientState(), MqttsnClientState.DISCONNECTED));
        if(state != null && !MqttsnUtils.in(state.getClientState(), MqttsnClientState.DISCONNECTED)){
            registry.getGatewaySessionService().disconnect(state, receivedDisconnect);
        }
        return needsResponse ?
                super.handleDisconnect(context, initialDisconnect, receivedDisconnect) : null;
    }

    @Override
    protected IMqttsnMessage handlePingreq(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException, MqttsnCodecException, MqttsnInvalidSessionStateException {

        String clientId = null;
        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnPingreq ping = (MqttsnPingreq) message;
            clientId = ping.getClientId();
        }
        else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnPingreq_V2_0 ping = (MqttsnPingreq_V2_0) message;
            clientId = ping.getClientId();
        }

        IMqttsnSessionState state = getSessionState(context);
        if(clientId != null){
            //-- ensure the clientId matches the context
            if(!clientId.trim().equals(context.getId())){
                logger.log(Level.WARNING, String.format("ping-req contained clientId [%s] that did not match that from context [%s]",
                        clientId, context.getId()));
                return super.handlePingreq(context, message);
            }
        }

        if(MqttsnUtils.in(state.getClientState(), MqttsnClientState.CONNECTED)){
            if(registry.getMessageQueue().size(context) > 0){
                registry.getMessageStateService().scheduleFlush(context);
            }
        }
        if(MqttsnUtils.in(state.getClientState(), MqttsnClientState.ASLEEP, MqttsnClientState.AWAKE)){
            //-- only wake the client if there is messages outstanding
            if(registry.getMessageQueue().size(context) > 0){
                if(state.getClientState() == MqttsnClientState.ASLEEP){
                    //-- this is the waking ping.. all is ok
                    registry.getGatewaySessionService().wake(state);
                    registry.getMessageStateService().scheduleFlush(context);

                } else if(state.getClientState() == MqttsnClientState.AWAKE){
                    //-- this is the client issuing multiple pings when it should be waiting on the messages.. humph
                    logger.log(Level.INFO, "multiple pings are being sent, clear up and try again the client is getting confused..");
                    registry.getMessageStateService().clearInflight(context);
                    registry.getMessageStateService().scheduleFlush(context);
                }

                return null;
            } else {
                return super.handlePingreq(context, message);
            }
        } else {
            registry.getGatewaySessionService().ping(state);
            return super.handlePingreq(context, message);
        }
    }

    @Override
    protected IMqttsnMessage handleSubscribe(IMqttsnContext context, IMqttsnMessage message)
            throws MqttsnException, MqttsnCodecException, MqttsnInvalidSessionStateException {

        int topicIdType = 0;
        byte[] topicData = null;
        int QoS = 0;

        if(context.getProtocolVersion() <= MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnSubscribe subscribe = (MqttsnSubscribe) message;
            topicIdType = subscribe.getTopicType();
            topicData = subscribe.getTopicData();
            QoS = subscribe.getQoS();
        }
        else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnSubscribe_V2_0 subscribe = (MqttsnSubscribe_V2_0) message;
            topicIdType = subscribe.getTopicIdType();
            topicData = subscribe.getTopicData();
            QoS = subscribe.getQoS();
        }

        if(!MqttsnUtils.validTopicScheme(topicIdType, topicData, true)){
            logger.log(Level.WARNING, String.format("supplied topic did not appear to be valid, return INVALID TOPIC ID typeId [%s] topicData [%s]", topicIdType,
                    MqttsnWireUtils.toBinary(topicData)));
            return registry.getMessageFactory().createSuback(0, 0, MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID);
        }

        IMqttsnSessionState state = getSessionState(context);
        TopicInfo info = registry.getTopicRegistry().normalize((byte) topicIdType, topicData,
                topicIdType == 0);

        SubscribeResult result = registry.getGatewaySessionService().subscribe(state, info, message);
        logger.log(Level.INFO, "subscribe message yielded info " + info + " and result " + result);
        processSessionResult(result);
        if(result.isError()){
            return registry.getMessageFactory().createSuback(0, 0, result.getReturnCode());
        } else {
            //-- this is a flaw in the current spec, you should be able to send back the topicIdType in the response
            IMqttsnMessage suback = registry.getMessageFactory().createSuback(result.getGrantedQoS(),
                    result.getTopicInfo().getTopicId(), result.getReturnCode());
            return suback;
        }
    }

    @Override
    protected IMqttsnMessage handleUnsubscribe(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException, MqttsnCodecException, MqttsnInvalidSessionStateException {

        MqttsnUnsubscribe unsubscribe = (MqttsnUnsubscribe) message;

        if(!MqttsnUtils.validTopicScheme(unsubscribe.getTopicType(), unsubscribe.getTopicData(), true)){
            logger.log(Level.WARNING, String.format("supplied topic did not appear to be valid, return INVALID TOPIC ID typeId [%s] topicData [%s]", unsubscribe.getTopicType(),
                    MqttsnWireUtils.toBinary(unsubscribe.getTopicData())));
            return registry.getMessageFactory().createUnsuback();
        }

        IMqttsnSessionState state = getSessionState(context);
        TopicInfo info = registry.getTopicRegistry().normalize((byte) unsubscribe.getTopicType(), unsubscribe.getTopicData(), true);
        UnsubscribeResult result = registry.getGatewaySessionService().unsubscribe(state, info, message);
        processSessionResult(result);
        return registry.getMessageFactory().createUnsuback();
    }

    @Override
    protected IMqttsnMessage handleRegister(IMqttsnContext context, IMqttsnMessage message)
            throws MqttsnException, MqttsnCodecException, MqttsnInvalidSessionStateException {

        MqttsnRegister register = (MqttsnRegister) message;

        if(!MqttsnSpecificationValidator.validTopicPath(register.getTopicName())){
            logger.log(Level.WARNING,
                    String.format("invalid topic [%s] received during register, reply with error code", register.getTopicName()));
            return registry.getMessageFactory().createRegack(0, MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID);
        } else {
            IMqttsnSessionState state = getSessionState(context);
            RegisterResult result = registry.getGatewaySessionService().register(state, register.getTopicName());
            processSessionResult(result);
            return registry.getMessageFactory().createRegack(result.getTopicInfo().getTopicId(), MqttsnConstants.RETURN_CODE_ACCEPTED);
        }
    }

    @Override
    protected IMqttsnMessage handlePublish(IMqttsnContext context, IMqttsnMessage message)
            throws MqttsnException, MqttsnCodecException, MqttsnInvalidSessionStateException {

        int QoS = 0;
        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnPublish publish = (MqttsnPublish) message;
            QoS = publish.getQoS();
        } else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnPublish_V2_0 publish = (MqttsnPublish_V2_0) message;
            QoS = publish.getQoS();
        }

        IMqttsnSessionState state = null;
        try {
            state = getSessionState(context);
        } catch(MqttsnInvalidSessionStateException e){
            //-- connectionless publish (m1)
            if(QoS != MqttsnConstants.QoSM1)
                throw e;
        }

        return super.handlePublish(context, message);
    }

    protected void processSessionResult(Result result){
        if(result.getStatus() == Result.STATUS.ERROR){
            logger.log(Level.WARNING, String.format("error detected by session service [%s]", result));
        }
    }
}