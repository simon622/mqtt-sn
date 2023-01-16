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

package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.IMqttsnMessageContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.TopicInfo;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.model.session.IWillData;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.wire.version1_2.payload.*;
import org.slj.mqtt.sn.wire.version2_0.payload.*;

public abstract class AbstractMqttsnMessageHandler
        extends AbstractMqttsnService implements IMqttsnMessageHandler {

    public boolean temporaryAuthorizeContext(INetworkContext context) {
        try {
            IClientIdentifierContext mqttsnContext = registry.getContextFactory().createTemporaryApplicationContext(context,
                    getRegistry().getCodec().getProtocolVersion());
            if(mqttsnContext != null){
                logger.warn("temporary auth context created {}", mqttsnContext);
                registry.getNetworkRegistry().bindContexts(context, mqttsnContext);
                return true;
            }
            logger.warn("context factory did not provide temporary secured context, refuse auth");
            return false;
        }
        catch(MqttsnSecurityException e){
            logger.warn("security exception detected, refuse auth", e);
            return false;
        }
    }

    public boolean authorizeContext(INetworkContext context, String clientId, int protocolVersion, boolean assignedClientId) {
        try {
            boolean authorized = false;
            if(!MqttsnSpecificationValidator.validClientId(clientId, false)){
                authorized = false;
                logger.warn("clientId format not valid, refuse auth");
            } else {
                registry.getNetworkRegistry().removeExistingClientId(clientId);
                IClientIdentifierContext mqttsnContext = registry.getContextFactory().createInitialApplicationContext(context, clientId, protocolVersion);
                if(mqttsnContext != null){
                    registry.getNetworkRegistry().bindContexts(context, mqttsnContext);
                    mqttsnContext.setAssignedClientId(assignedClientId);
                    authorized = true;
                } else {
                    logger.warn("context factory did not provide secured context, refuse auth");
                }
            }
            return authorized;
        }
        catch(MqttsnSecurityException e){
            logger.warn("security exception detected, refuse auth", e);
            return false;
        }
    }

    @Override
    public boolean canHandle(IMqttsnMessageContext context, IMqttsnMessage message){
        return true;
    }

    @Override
    public void receiveMessage(IMqttsnMessageContext context, IMqttsnMessage message)
            throws MqttsnException {

        try {

            if(!canHandle(context, message)){
                logger.warn("mqtt-sn handler [{} <- {}] dropping message it could not handle {}",
                        context, message.getMessageName());
                return;
            }

            if(message.isErrorMessage()){
                logger.warn("mqtt-sn handler [{} <- {}] received error message {}",
                        registry.getOptions().getContextId(), context, message);
            }

            beforeHandle(context, message);

            logger.info("mqtt-sn handler [{} <- {}] handling inbound message {}",
                        registry.getOptions().getContextId(), context, message);

            boolean errord = false;
            IMqttsnMessage originatingMessage = null;

            if(registry.getMessageStateService() != null){
                try {
                    originatingMessage =
                            registry.getMessageStateService().notifyMessageReceived(context.getClientContext(), message);
                } catch(MqttsnException e){
                    errord = true;
                    logger.warn("mqtt-sn handler [{} <- {}] state service errord, allow message lifecycle to handle {} -> {}",
                            registry.getOptions().getContextId(), context.getClientContext().getId(), message, e.getMessage());
                }
            }

            IMqttsnMessage response = handle(context, originatingMessage, message, errord);

            //-- if the state service threw a wobbler but for some reason this didnt lead to an error message
            //-- we should just disconnect the device
            if(errord && !response.isErrorMessage()){
                logger.warn("mqtt-sn handler [{} <- {}] state service errord, message handler did not produce an error, so overrule and disconnect {} -> {}",
                        registry.getOptions().getContextId(), context, message);
                response = registry.getMessageFactory().createDisconnect();
            }

            //-- this tidies up inflight if there are errors
            afterHandle(context, message, response);

            if (response != null) {
                if (response.needsId() && response.getId() == 0) {
                    int msgId = message.getId();
                    response.setId(msgId);
                }

                handleResponse(context, response);
            }

            afterResponse(context, message, response);

        } catch(MqttsnException e){
            logger.warn("handled with disconnect error encountered during receive;", e);
            handleResponse(context,
                    registry.getMessageFactory().createDisconnect());
            if(!registry.getRuntime().handleLocalDisconnect(context.getClientContext(), e)) {
                throw e;
            }
        }
    }

    protected void beforeHandle(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {

    }

    protected IMqttsnMessage handle(IMqttsnMessageContext context, IMqttsnMessage originatingMessage, IMqttsnMessage message, boolean errord)
            throws MqttsnException {

        IMqttsnMessage response = null;
        int msgType = message.getMessageType();


        switch (msgType) {
            case MqttsnConstants.CONNECT:
                response = handleConnect(context, message);
                if(!errord && !response.isErrorMessage()){
                    registry.getRuntime().handleConnected(context.getClientContext());
                }
                break;
            case MqttsnConstants.CONNACK:
                if(validateOriginatingMessage(context, originatingMessage, message)){
                    handleConnack(context, originatingMessage, message);
                    if(!errord && !message.isErrorMessage()){
                        registry.getRuntime().handleConnected(context.getClientContext());
                    }
                }
                break;
            case MqttsnConstants.PUBLISH:
                if(errord){
                    response = getRegistry().getMessageFactory().createPuback(0,
                            MqttsnConstants.RETURN_CODE_SERVER_UNAVAILABLE);
                } else {
                    response = handlePublish(context, message);
                }
                break;
            case MqttsnConstants.PUBREC:
                response = handlePubrec(context, message);
                break;
            case MqttsnConstants.PUBREL:
                response = handlePubrel(context, message);
                break;
            case MqttsnConstants.PUBACK:
                if(validateOriginatingMessage(context, originatingMessage, message)){
                    handlePuback(context, originatingMessage, message);
                }
                break;
            case MqttsnConstants.PUBCOMP:
                if(validateOriginatingMessage(context, originatingMessage, message)){
                    handlePubcomp(context, originatingMessage, message);
                }
                break;
            case MqttsnConstants.SUBSCRIBE:
                if(errord){
                    response = getRegistry().getMessageFactory().createSuback(0, 0,
                            MqttsnConstants.RETURN_CODE_SERVER_UNAVAILABLE);
                } else {
                    response = handleSubscribe(context, message);
                }
                break;
            case MqttsnConstants.UNSUBSCRIBE:
                response = handleUnsubscribe(context, message);
                break;
            case MqttsnConstants.UNSUBACK:
                if(validateOriginatingMessage(context, originatingMessage, message)){
                    if(!errord){
                        handleUnsuback(context, originatingMessage, message);
                    }
                }
                break;
            case MqttsnConstants.SUBACK:
                if(validateOriginatingMessage(context, originatingMessage, message)){
                    if(!errord){
                        handleSuback(context, originatingMessage, message);
                    }
                }
                break;
            case MqttsnConstants.REGISTER:
                if(errord){
                    response = getRegistry().getMessageFactory().createRegack(MqttsnConstants.TOPIC_NORMAL, 0,
                            MqttsnConstants.RETURN_CODE_SERVER_UNAVAILABLE);
                } else {
                    response = handleRegister(context, message);
                }
                break;
            case MqttsnConstants.REGACK:
                if(validateOriginatingMessage(context, originatingMessage, message)){
                    if(!errord){
                        handleRegack(context, originatingMessage, message);
                    }
                }
                break;
            case MqttsnConstants.PINGREQ:
                response = handlePingreq(context, message);
                break;
            case MqttsnConstants.PINGRESP:
                if(validateOriginatingMessage(context, originatingMessage, message)){
                    handlePingresp(context, originatingMessage, message);
                }
                break;
            case MqttsnConstants.DISCONNECT:
                response = handleDisconnect(context, originatingMessage, message);
                break;
            case MqttsnConstants.ADVERTISE:
                handleAdvertise(context, message);
                break;
            case MqttsnConstants.ENCAPSMSG:
                handleEncapsmsg(context, message);
                break;
            case MqttsnConstants.GWINFO:
                handleGwinfo(context, message);
                break;
            case MqttsnConstants.HELO:
                response = handleHelo(context, message);
                break;
            case MqttsnConstants.SEARCHGW:
                response = handleSearchGw(context, message);
                break;
            case MqttsnConstants.WILLMSGREQ:
                response = handleWillmsgreq(context, message);
                break;
            case MqttsnConstants.WILLMSG:
                response = handleWillmsg(context, message);
                break;
            case MqttsnConstants.WILLMSGUPD:
                response = handleWillmsgupd(context, message);
                break;
            case MqttsnConstants.WILLMSGRESP:
                handleWillmsgresp(context, message);
                break;
            case MqttsnConstants.WILLTOPICREQ:
                response = handleWilltopicreq(context, message);
                break;
            case MqttsnConstants.WILLTOPIC:
                response = handleWilltopic(context, message);
                break;
            case MqttsnConstants.WILLTOPICUPD:
                response = handleWilltopicupd(context, message);
                break;
            case MqttsnConstants.WILLTOPICRESP:
                handleWilltopicresp(context, message);
                break;
            default:
                throw new MqttsnException("unable to handle unknown message type " + msgType);
        }

        return response;
    }


    protected void afterHandle(IMqttsnMessageContext context, IMqttsnMessage message, IMqttsnMessage response) throws MqttsnException {

        if(response != null && response.isErrorMessage()){
            //we need to remove any message that was marked inflight
            if(message.needsId()){
                if(registry.getMessageStateService().removeInflight(context.getClientContext(), IMqttsnOriginatingMessageSource.REMOTE, message.getId()) != null){
                    logger.warn("tidied up bad message that was marked inflight and yeilded error response");
                }
            }
        }
    }

    protected void afterResponse(IMqttsnMessageContext context, IMqttsnMessage message, IMqttsnMessage response) throws MqttsnException {
    }

    protected boolean validateOriginatingMessage(IMqttsnMessageContext context, IMqttsnMessage originatingMessage, IMqttsnMessage message) {
        if(originatingMessage == null){
            logger.warn("{} no originating message found for acknowledgement {}; reaper probably moved this back to queue", context, message);
            return false;
        }
        return true;
    }

    protected void handleResponse(IMqttsnMessageContext context, IMqttsnMessage response)
            throws MqttsnException {

        logger.info("mqtt-sn handler [{} -> {}] sending outbound message {}",
                    registry.getOptions().getContextId(), context, response);
        context.getNetworkContext().getTransport().writeToTransport(context.getNetworkContext(), response);
    }

    protected IMqttsnMessage handleConnect(IMqttsnMessageContext messageContext, IMqttsnMessage connect) throws MqttsnException {

        boolean will = false;

        if(messageContext.getClientContext().getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnConnect_V2_0 connectMessage = (MqttsnConnect_V2_0) connect ;
            will = connectMessage.isWill();
        } else {
            MqttsnConnect connectMessage = (MqttsnConnect) connect ;
            will = connectMessage.isWill();
        }

        if(will){
            return registry.getMessageFactory().createWillTopicReq();
        } else {
            return registry.getMessageFactory().createConnack(MqttsnConstants.RETURN_CODE_ACCEPTED);
        }
    }

    protected void handleConnack(IMqttsnMessageContext context, IMqttsnMessage connect, IMqttsnMessage connack) throws MqttsnException {
    }

    protected IMqttsnMessage handleDisconnect(IMqttsnMessageContext context, IMqttsnMessage originatingMessage, IMqttsnMessage message) throws MqttsnException, MqttsnCodecException {

        //-- if the disconnect is received in response to a disconnect we sent, lets not send another!
        if(originatingMessage != null){
            logger.info("disconnect received in response to my disconnect, dont send another!");
            return null;
        } else {
            if(registry.getRuntime().handleRemoteDisconnect(context.getClientContext())){
                return registry.getMessageFactory().createDisconnect();
            }
            return null;
        }
    }

    protected IMqttsnMessage handlePingreq(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException, MqttsnCodecException {
        return registry.getMessageFactory().createPingresp();
    }

    protected void handlePingresp(IMqttsnMessageContext context, IMqttsnMessage originatingMessage, IMqttsnMessage message) throws MqttsnException {
    }

    protected IMqttsnMessage handleSubscribe(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException, MqttsnCodecException {

        int QoS = 0;

        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnSubscribe_V2_0 subscribe = (MqttsnSubscribe_V2_0) message;
            QoS = subscribe.getQoS();
        } else {
            MqttsnSubscribe subscribe = (MqttsnSubscribe) message;
            QoS = subscribe.getQoS();
        }
        return registry.getMessageFactory().createSuback(QoS, 0x00, MqttsnConstants.RETURN_CODE_ACCEPTED);
    }

    protected IMqttsnMessage handleUnsubscribe(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException, MqttsnCodecException {

        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnUnsubscribe unsub = (MqttsnUnsubscribe) message;

        } else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnUnsubscribe_V2_0 unsub = (MqttsnUnsubscribe_V2_0) message;
        }

        return registry.getMessageFactory().createUnsuback(MqttsnConstants.RETURN_CODE_ACCEPTED);
    }

    protected void handleSuback(IMqttsnMessageContext context, IMqttsnMessage initial, IMqttsnMessage message) throws MqttsnException {

        boolean isError = false;
        int topicId = 0;
        int topicIdType = 0;
        int grantedQoS = 0;
        String topicPath = null;
        byte[] topicData = null;

        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnSuback_V2_0 suback = (MqttsnSuback_V2_0) message;
            MqttsnSubscribe_V2_0 subscribe = (MqttsnSubscribe_V2_0) initial;

            isError = suback.isErrorMessage();
            topicId = suback.getTopicId();
            topicIdType = suback.getTopicIdType();
            grantedQoS = suback.getQoS();
            topicData = subscribe.getTopicData();
            topicPath = topicIdType == MqttsnConstants.TOPIC_PREDEFINED ? null : subscribe.getTopicName();
        } else {
            MqttsnSuback suback = (MqttsnSuback) message;
            MqttsnSubscribe subscribe = (MqttsnSubscribe) initial;

            isError = suback.isErrorMessage();
            topicId = suback.getTopicId();
            grantedQoS = suback.getQoS();
            //NB: note the type is derived from the subscribe as v1.2 doesnt allow for the transmission on the suback
            topicIdType = subscribe.getTopicType();
            topicData = subscribe.getTopicData();
            topicPath = topicIdType == MqttsnConstants.TOPIC_PREDEFINED ? null : subscribe.getTopicName();
        }

        if(!isError){
            ISession session = context.getSession();
            if(topicIdType == MqttsnConstants.TOPIC_NORMAL){
                registry.getTopicRegistry().register(session, topicPath, topicId);
            } else {
                topicPath = registry.getTopicRegistry().topicPath(session,
                        registry.getTopicRegistry().normalize(
                                (byte) topicIdType, topicData, false), false);
            }
            try {
                //-- add the subscription to the local engine
                registry.getSubscriptionRegistry().subscribe(session, topicPath, grantedQoS);
            } catch(MqttsnIllegalFormatException e){
                throw new MqttsnException("format error encountered in local runtime", e);
            }
        }
    }

    protected void handleUnsuback(IMqttsnMessageContext context, IMqttsnMessage unsubscribe, IMqttsnMessage unsuback) throws MqttsnException {
        String topicPath = ((MqttsnUnsubscribe)unsubscribe).getTopicName();
        registry.getSubscriptionRegistry().unsubscribe(context.getSession(), topicPath);
    }

    protected IMqttsnMessage handleRegister(IMqttsnMessageContext context, IMqttsnMessage message)
            throws MqttsnException, MqttsnCodecException {
        MqttsnRegister register = (MqttsnRegister) message;
        return registry.getMessageFactory().createRegack(MqttsnConstants.TOPIC_NORMAL, register.getTopicId(), MqttsnConstants.RETURN_CODE_ACCEPTED);
    }

    protected void handleRegack(IMqttsnMessageContext context, IMqttsnMessage register, IMqttsnMessage response) throws MqttsnException {
        String topicPath = ((MqttsnRegister)register).getTopicName();
        int topicId = 0;
        int topicIdType = MqttsnConstants.TOPIC_NORMAL;
        boolean isError = false;

        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnRegack_V2_0 regack = (MqttsnRegack_V2_0) response;
            topicId = regack.getTopicId();
            isError = regack.isErrorMessage();
            topicIdType = regack.getTopicIdType();
        } else {
            MqttsnRegack regack = (MqttsnRegack) response;
            topicId = regack.getTopicId();
            isError = regack.isErrorMessage();
        }

        if(!isError){
            if(topicIdType == MqttsnConstants.TOPIC_PREDEFINED){
                getRegistry().getOptions().getPredefinedTopics().put(topicPath, topicId);
                logger.warn("received PREDEFINED regack response (v2), registering {}; Msg={}", context, response);
            } else if(topicIdType == MqttsnConstants.TOPIC_NORMAL){
                registry.getTopicRegistry().register(context.getSession(), topicPath, topicId);
            }
        } else {
            logger.warn("received error regack response {}; Msg={}", context, response);
        }
    }

    protected IMqttsnMessage handlePublish(IMqttsnMessageContext context, IMqttsnMessage message)
            throws MqttsnException, MqttsnCodecException {

        int QoS = 0;
        int topicIdType = 0;
        int topicDataAsInt = 0;
        byte[] topicData = null;
        byte[] data = null;

        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnPublish_V2_0 publish = (MqttsnPublish_V2_0) message;
            QoS = publish.getQoS();
            topicIdType = publish.getTopicIdType();
            topicData = publish.getTopicData();
            data = publish.getData();
            topicDataAsInt = publish.readTopicDataAsInteger();
        } else {
            MqttsnPublish publish = (MqttsnPublish) message;
            QoS = publish.getQoS();
            topicIdType = publish.getTopicType();
            topicData = publish.getTopicData();
            data = publish.getData();
            topicDataAsInt = publish.readTopicDataAsInteger();
        }

        IMqttsnMessage response = null;

        ISession session = context.getSession();

        TopicInfo info = registry.getTopicRegistry().normalize((byte) topicIdType, topicData, false);
        String topicPath = registry.getTopicRegistry().topicPath(session, info, true);
        if(registry.getAuthorizationService() != null){
            if(!registry.getAuthorizationService().allowedToPublish(context.getClientContext(), topicPath, data.length, QoS)){
                logger.warn("authorization service rejected publish from {} to {}", context, topicPath);
                response = registry.getMessageFactory().createPuback(topicDataAsInt, MqttsnConstants.RETURN_CODE_REJECTED_CONGESTION);
            }
        }

        if(response == null){
            switch (QoS) {
                case MqttsnConstants.QoS1:
                    response = registry.getMessageFactory().createPuback(topicDataAsInt, MqttsnConstants.RETURN_CODE_ACCEPTED);
                    break;
                case MqttsnConstants.QoS2:
                    response = registry.getMessageFactory().createPubrec();
                    break;

                default:
                case MqttsnConstants.QoSM1:
                case MqttsnConstants.QoS0:
                    break;
            }
        }
        return response;
    }

    protected void handlePuback(IMqttsnMessageContext context, IMqttsnMessage originatingMessage, IMqttsnMessage message)
            throws MqttsnException {
    }

    protected IMqttsnMessage handlePubrel(IMqttsnMessageContext context, IMqttsnMessage message)
            throws MqttsnException, MqttsnCodecException {
        return registry.getMessageFactory().createPubcomp();
    }

    protected IMqttsnMessage handlePubrec(IMqttsnMessageContext context, IMqttsnMessage message)
            throws MqttsnException, MqttsnCodecException {
        return registry.getMessageFactory().createPubrel();
    }

    protected void handlePubcomp(IMqttsnMessageContext context, IMqttsnMessage originatingMessage, IMqttsnMessage message)
            throws MqttsnException {
    }

    protected void handleAdvertise(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {
    }

    protected void handleEncapsmsg(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {
    }

    protected IMqttsnMessage handleSearchGw(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {
        return null;
    }

    protected void handleGwinfo(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {
    }

    protected IMqttsnMessage handleHelo(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {
        MqttsnHelo helo = (MqttsnHelo) message;
        if(helo.getUserAgent() == null){
            //this is a request for a HELO reply
            helo = new MqttsnHelo();
            helo.setUserAgent(getRegistry().getRuntime().getUserAgent());
            return helo;

        } else {
            logger.info("received HELO reply from {} -> userAgent {}", context, helo.getUserAgent());
            return null;
        }
    }

    protected IMqttsnMessage handleWillmsgreq(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {

        IWillData willData = registry.getWillRegistry().getWillMessage(context.getSession());
        byte[] willMsg = willData.getData();
        return registry.getMessageFactory().createWillMsg(willMsg);
    }

    protected IMqttsnMessage handleWillmsg(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {

        MqttsnWillmsg update = (MqttsnWillmsg) message;
        byte[] data = update.getMsgData();
        registry.getWillRegistry().updateWillMessage(context.getSession(), data);
        return registry.getMessageFactory().createConnack(MqttsnConstants.RETURN_CODE_ACCEPTED);
    }

    protected IMqttsnMessage handleWillmsgupd(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {

        MqttsnWillmsgupd update = (MqttsnWillmsgupd) message;
        byte[] data = update.getMsgData();
        registry.getWillRegistry().updateWillMessage(context.getSession(), data);
        return registry.getMessageFactory().createWillMsgResp(MqttsnConstants.RETURN_CODE_ACCEPTED);
    }

    protected void handleWillmsgresp(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {
    }

    protected IMqttsnMessage handleWilltopicreq(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {

        IWillData willData = registry.getWillRegistry().getWillMessage(context.getSession());
        int QoS = willData.getQos();
        boolean retain = willData.isRetained();
        String topicPath = willData.getTopicPath().toString();
        return registry.getMessageFactory().createWillTopic(QoS, retain, topicPath);
    }

    protected IMqttsnMessage handleWilltopic(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {

        //when in connect interaction, the will topic should yield a response of will message req

        MqttsnWilltopic update = (MqttsnWilltopic) message;
        int qos = update.getQoS();
        boolean retain = update.isRetainedPublish();
        String topicPath = update.getWillTopicData();
        registry.getWillRegistry().updateWillTopic(context.getSession(), topicPath, qos, retain);
        return registry.getMessageFactory().createWillMsgReq();
    }

    protected IMqttsnMessage handleWilltopicupd(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {
        MqttsnWilltopicudp update = (MqttsnWilltopicudp) message;
        String topicPath = update.getWillTopicData();
        int qos = update.getQoS();
        boolean retain = update.isRetainedPublish();
        registry.getWillRegistry().updateWillTopic(context.getSession(), topicPath, qos, retain);
        return registry.getMessageFactory().createWillTopicResp(MqttsnConstants.RETURN_CODE_ACCEPTED);
    }

    protected void handleWilltopicresp(IMqttsnMessageContext context, IMqttsnMessage message) throws MqttsnException {
    }
}
