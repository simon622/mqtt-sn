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
import org.slj.mqtt.sn.MqttsnMessageRules;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.impl.AbstractMqttsnMessageHandler;
import org.slj.mqtt.sn.model.IMqttsnMessageContext;
import org.slj.mqtt.sn.model.ClientState;
import org.slj.mqtt.sn.model.TopicInfo;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.version1_2.payload.*;
import org.slj.mqtt.sn.wire.version2_0.payload.*;

public class MqttsnGatewayMessageHandler
        extends AbstractMqttsnMessageHandler {

    protected IMqttsnGatewayRuntimeRegistry getRegistry(){
        return (IMqttsnGatewayRuntimeRegistry) super.getRegistry();
    }

    protected ISession getActiveSession(IMqttsnMessageContext context)
            throws MqttsnInvalidSessionStateException {
        ISession session = context.getMqttsnSession();
        if(session == null || session.getClientState() == ClientState.DISCONNECTED)
            throw new MqttsnInvalidSessionStateException("session not available for context");
        return session;
    }

    @Override
    protected void beforeHandle(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {

        try {
            //see if the client has an active sessionx
            getActiveSession(context);
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
                logger.warn("detected invalid client session state for {} and inbound message {}", context, message);
                throw new MqttsnException(e);
            }
        }
    }

    @Override
    protected void afterHandle(IMqttsnMessageContext context, IMqttsnMessage messageIn, IMqttsnMessage messageOut) throws MqttsnException {

        super.afterHandle(context, messageIn, messageOut);

        try {
            ISession session = getActiveSession(context);
            if(session != null){
                getRegistry().getSessionRegistry().modifyLastSeen(session);
            }
        } catch(MqttsnInvalidSessionStateException e){
            //-- a disconnect will mean theres no session to update
        }
    }

    @Override
    protected void afterResponse(IMqttsnMessageContext context, IMqttsnMessage messageIn, IMqttsnMessage messageOut) throws MqttsnException {

        try {

            if(messageOut != null && messageOut.isErrorMessage() &&
                    registry.getCodec().isConnect(messageIn)){
                //-- this is an error in CONNECT - remove from network registry so we dont leak
                logger.warn("connect for {} was rejected, tidy up network layer after response is sent", context);
                registry.getNetworkRegistry().removeExistingClientId(context.getMqttsnContext().getId());
            }

            ISession session = context.getMqttsnSession();
            if(session != null){
                //active session means we can try and see if there is anything to flush here if its a terminal message
                if(messageIn != null && MqttsnMessageRules.isTerminalMessage(getRegistry().getCodec(), messageIn) && !messageIn.isErrorMessage() ||
                        messageOut != null && MqttsnMessageRules.isTerminalMessage(getRegistry().getCodec(), messageOut) && !messageOut.isErrorMessage() ){
                    if(MqttsnUtils.in(session.getClientState(),
                            ClientState.ACTIVE, ClientState.AWAKE)) {
                        logger.debug("scheduling flush based on outbound message {} -> inflight {}", messageOut == null ? messageIn : messageOut,
                                    getRegistry().getMessageStateService().countInflight(context.getMqttsnContext(), IMqttsnOriginatingMessageSource.LOCAL));
                        registry.getMessageStateService().scheduleFlush(context.getMqttsnContext());
                    }
                }
            }
        } catch(MqttsnInvalidSessionStateException e){
            //-- a disconnect will mean theres no session to update
        }
    }

    @Override
    protected IMqttsnMessage handleConnect(IMqttsnMessageContext context, IMqttsnMessage connect) throws MqttsnException, MqttsnCodecException {

        String clientId = null;
        boolean cleanStart = false;
        int keepAlive = 0;
        boolean will = false;
        long sessionExpiryInterval = MqttsnConstants.UNSIGNED_MAX_32;
        int maxPacketSize = MqttsnConstants.UNSIGNED_MAX_16;
        int protocolVersion = MqttsnConstants.PROTOCOL_VERSION_UNKNOWN;

        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnConnect connectMessage = (MqttsnConnect) connect ;
            clientId = connectMessage.getClientId();
            cleanStart = connectMessage.isCleanSession();
            keepAlive = connectMessage.getDuration();
            will = connectMessage.isWill();
            protocolVersion = MqttsnConstants.PROTOCOL_VERSION_1_2;
        }
        else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnConnect_V2_0 connectMessage = (MqttsnConnect_V2_0) connect ;
            clientId = connectMessage.getClientId();
            cleanStart = connectMessage.isCleanStart();
            will = connectMessage.isWill();
            keepAlive = connectMessage.getKeepAlive();
            sessionExpiryInterval = connectMessage.getSessionExpiryInterval();
            maxPacketSize = connectMessage.getMaxPacketSize();
            protocolVersion = MqttsnConstants.PROTOCOL_VERSION_2_0;
        }

        //-- just be careful here - the cliendId from the message may well not match the one on the context at this
        //-- point if it has been resolved by the clientIdFactory to be something different; however the inbound
        //-- message version should be used for atuh checking

        if(registry.getAuthenticationService() != null){
            if(!registry.getAuthenticationService().allowConnect(context.getMqttsnContext(), clientId)){
                logger.warn("authentication service rejected client {}", clientId);
                return registry.getMessageFactory().createConnack(MqttsnConstants.RETURN_CODE_SERVER_UNAVAILABLE);
            }
        }

        //-- THIS IS WHERE A SESSION IS CREATED ON THE GATEWAY SIDE
        ISession session = context.getMqttsnSession();
        String assignedClientId = context.getMqttsnContext().isAssignedClientId() ? context.getMqttsnContext().getId() : null;
        boolean stateExisted = session != null;
        if(session == null){
            //ensure we update the context so the session is present for the rest of the message processing
            session = getRegistry().getSessionRegistry().getSession(context.getMqttsnContext(), true);
            context.setMqttsnSession(session);
        }

        ConnectResult result = getRegistry().getGatewaySessionService().connect(context.getMqttsnSession(), connect);
        processSessionResult(result);
        if(result.isError()){
            return registry.getMessageFactory().createConnack(result.getReturnCode());
        }
        else {
            if(will){
                return registry.getMessageFactory().createWillTopicReq();
            } else {
                long sessionExpiryIntervalRequested = sessionExpiryInterval;
                if(sessionExpiryInterval >
                        registry.getOptions().getSessionExpiryInterval()){
                    sessionExpiryInterval = Math.min(sessionExpiryInterval,
                            registry.getOptions().getSessionExpiryInterval());
                }
                boolean changedFromRequested = sessionExpiryIntervalRequested != sessionExpiryInterval;
                getRegistry().getSessionRegistry().modifySessionExpiryInterval(session, sessionExpiryInterval);
                getRegistry().getSessionRegistry().modifyMaxPacketSize(session, maxPacketSize);
                getRegistry().getSessionRegistry().modifyProtocolVersion(session, protocolVersion);
                return registry.getMessageFactory().createConnack(
                        result.getReturnCode(), stateExisted, assignedClientId, changedFromRequested ? sessionExpiryInterval : 0);
            }
        }
    }

    @Override
    protected IMqttsnMessage handleDisconnect(IMqttsnMessageContext context, IMqttsnMessage initialDisconnect, IMqttsnMessage receivedDisconnect) throws MqttsnException, MqttsnCodecException, MqttsnInvalidSessionStateException {

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

        ISession state = getActiveSession(context);
        boolean needsResponse = initialDisconnect != null ||
                (state != null && !MqttsnUtils.in(state.getClientState(), ClientState.DISCONNECTED));
        if(state != null && !MqttsnUtils.in(state.getClientState(), ClientState.DISCONNECTED)){
            getRegistry().getGatewaySessionService().disconnect(state, receivedDisconnect);
        }
        return needsResponse ?
                super.handleDisconnect(context, initialDisconnect, receivedDisconnect) : null;
    }

    @Override
    protected IMqttsnMessage handlePingreq(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException, MqttsnCodecException, MqttsnInvalidSessionStateException {

        String clientId = null;
        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnPingreq ping = (MqttsnPingreq) message;
            clientId = ping.getClientId();
        }
        else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnPingreq_V2_0 ping = (MqttsnPingreq_V2_0) message;
            clientId = ping.getClientId();
        }


        if(clientId != null){
            //-- ensure the clientId matches the context
            if(!clientId.trim().equals(context.getMqttsnContext().getId())){
                logger.warn("ping-req contained clientId {} that did not match that from context {}",
                        clientId, context.getMqttsnContext().getId());
                return super.handlePingreq(context, message);
            }
        }


        ISession session = getActiveSession(context);
        if(MqttsnUtils.in(session.getClientState(), ClientState.ACTIVE)){
            if(getRegistry().getMessageQueue().queueSize(session) > 0){
                getRegistry().getMessageStateService().scheduleFlush(context.getMqttsnContext());
            }
        }
        if(MqttsnUtils.in(session.getClientState(), ClientState.ASLEEP, ClientState.AWAKE)){
            //-- only wake the client if there is messages outstanding
            if(registry.getMessageQueue().queueSize(session) > 0){
                if(session.getClientState() == ClientState.ASLEEP){
                    //-- this is the waking ping.. all is ok
                    getRegistry().getSessionRegistry().modifyClientState(session, ClientState.AWAKE);
                    getRegistry().getMessageStateService().scheduleFlush(context.getMqttsnContext());

                } else if(session.getClientState() == ClientState.AWAKE){
                    //-- this is the client issuing multiple pings when it should be waiting on the messages.. humph
                    logger.warn("multiple pings are being sent, clear up and try again the client is getting confused..");
                    registry.getMessageStateService().clearInflight(context.getMqttsnContext());
                    registry.getMessageStateService().scheduleFlush(context.getMqttsnContext());
                }

                return null;
            } else {
                return super.handlePingreq(context, message);
            }
        } else {
            return super.handlePingreq(context, message);
        }
    }

    @Override
    protected IMqttsnMessage handleSubscribe(IMqttsnMessageContext context, IMqttsnMessage message)
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
            logger.warn("supplied topic did not appear to be valid, return INVALID TOPIC ID typeId {} topicData {}", topicIdType,
                    MqttsnWireUtils.toBinary(topicData));
            return registry.getMessageFactory().createSuback(0, 0, MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID);
        }

        ISession state = getActiveSession(context);
        TopicInfo info = registry.getTopicRegistry().normalize((byte) topicIdType, topicData,
                topicIdType == 0);

        SubscribeResult result = getRegistry().getGatewaySessionService().subscribe(state, info, message);
        logger.info("subscribe message yielded info " + info + " and result " + result);
        processSessionResult(result);

        if(result.isError()){
            //-- send back an error return code
            return registry.getMessageFactory().createSuback(0, 0, result.getReturnCode());
        } else {
            //-- this is a flaw in the current spec, you should be able to send back the topicIdType in the response
            if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
                MqttsnSuback_V2_0 suback = (MqttsnSuback_V2_0) registry.getMessageFactory().createSuback(result.getGrantedQoS(),
                        result.getTopicInfo().getTopicId(), result.getReturnCode());
                suback.setTopicIdType(
                        result.getTopicInfo().getType().getFlag());
                return suback;
            } else {
                return registry.getMessageFactory().createSuback(result.getGrantedQoS(),
                        result.getTopicInfo().getTopicId(), result.getReturnCode());
            }
        }
    }

    @Override
    protected IMqttsnMessage handleUnsubscribe(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException, MqttsnCodecException, MqttsnInvalidSessionStateException {

        MqttsnUnsubscribe unsubscribe = (MqttsnUnsubscribe) message;

        if(!MqttsnUtils.validTopicScheme(unsubscribe.getTopicType(), unsubscribe.getTopicData(), true)){
            logger.warn("supplied topic did not appear to be valid, return INVALID TOPIC ID typeId {} topicData {}", unsubscribe.getTopicType(),
                    MqttsnWireUtils.toBinary(unsubscribe.getTopicData()));
            return registry.getMessageFactory().createUnsuback(MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID);
        }

        ISession state = getActiveSession(context);
        TopicInfo info = registry.getTopicRegistry().normalize((byte) unsubscribe.getTopicType(), unsubscribe.getTopicData(), true);
        UnsubscribeResult result = getRegistry().getGatewaySessionService().unsubscribe(state, info, message);
        processSessionResult(result);
        return registry.getMessageFactory().createUnsuback(MqttsnConstants.RETURN_CODE_ACCEPTED);
    }

    @Override
    protected IMqttsnMessage handleRegister(IMqttsnMessageContext context, IMqttsnMessage message)
            throws MqttsnException, MqttsnCodecException, MqttsnInvalidSessionStateException {

        MqttsnRegister register = (MqttsnRegister) message;

        if(!MqttsnSpecificationValidator.isValidPublishTopic(register.getTopicName())){
            logger.warn("invalid topic {} received during register, reply with error code", register.getTopicName());
            return registry.getMessageFactory().createRegack(MqttsnConstants.TOPIC_NORMAL, 0, MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID);
        } else {
            ISession state = getActiveSession(context);
            RegisterResult result = getRegistry().getGatewaySessionService().register(state, register.getTopicName());
            processSessionResult(result);

            //-- the codec will either support the return topicTypeId or not so pass it to the interface
            return registry.getMessageFactory().createRegack(
                    result.getTopicInfo().getType().getFlag(), result.getTopicInfo().getTopicId(),
                    MqttsnConstants.RETURN_CODE_ACCEPTED);
        }
    }

    @Override
    protected IMqttsnMessage handlePublish(IMqttsnMessageContext context, IMqttsnMessage message)
            throws MqttsnException, MqttsnCodecException, MqttsnInvalidSessionStateException {

        int QoS = 0;
        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnPublish publish = (MqttsnPublish) message;
            QoS = publish.getQoS();
        } else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnPublish_V2_0 publish = (MqttsnPublish_V2_0) message;
            QoS = publish.getQoS();
        }

        ISession state = null;
        try {
            state = getActiveSession(context);
        } catch(MqttsnInvalidSessionStateException e){
            //-- connectionless publish (m1)
            if(QoS != MqttsnConstants.QoSM1)
                throw e;
        }

        return super.handlePublish(context, message);
    }

    protected void processSessionResult(Result result){
        if(result.getStatus() == Result.STATUS.ERROR){
            logger.warn("error detected by session service {}", result);
        }
    }
}
