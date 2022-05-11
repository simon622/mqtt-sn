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
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.MqttsnWillData;
import org.slj.mqtt.sn.model.TopicInfo;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.wire.version1_2.payload.*;
import org.slj.mqtt.sn.wire.version2_0.payload.*;

import java.util.logging.Level;

public abstract class AbstractMqttsnMessageHandler<U extends IMqttsnRuntimeRegistry>
        extends MqttsnService<U> implements IMqttsnMessageHandler<U> {

    public boolean temporaryAuthorizeContext(INetworkContext context) {
        try {
            IMqttsnContext mqttsnContext = registry.getContextFactory().createTemporaryApplicationContext(context,
                    getRegistry().getCodec().getProtocolVersion());
            if(mqttsnContext != null){
                registry.getNetworkRegistry().bindContexts(context, mqttsnContext);
                return true;
            }
            logger.log(Level.WARNING, String.format("context factory did not provide temporary secured context, refuse auth"));
            return false;
        }
        catch(MqttsnSecurityException e){
            logger.log(Level.WARNING, String.format("security exception detected, refuse auth"), e);
            return false;
        }
    }

    public boolean authorizeContext(INetworkContext context, String clientId, int protocolVersion) {
        try {
            registry.getNetworkRegistry().removeExistingClientId(clientId);
            IMqttsnContext mqttsnContext = registry.getContextFactory().createInitialApplicationContext(context, clientId, protocolVersion);
            if(mqttsnContext != null){
                registry.getNetworkRegistry().bindContexts(context, mqttsnContext);
                return true;
            }
            logger.log(Level.WARNING, String.format("context factory did not provide secured context, refuse auth"));
            return false;
        }
        catch(MqttsnSecurityException e){
            logger.log(Level.WARNING, String.format("security exception detected, refuse auth"), e);
            return false;
        }
    }

    @Override
    public boolean canHandle(IMqttsnContext context, IMqttsnMessage message){
        return true;
    }

    @Override
    public boolean validResponse(IMqttsnMessage request, IMqttsnMessage response) {
        int[] clz = getResponseClasses(request);
        return MqttsnUtils.containsInt(clz, response.getMessageType());
    }

    private int[] getResponseClasses(IMqttsnMessage message) {

        if(!requiresResponse(message)){
            return new int[0];
        }
        switch(message.getMessageType()){
            case MqttsnConstants.AUTH:
                return new int[]{ MqttsnConstants.AUTH, MqttsnConstants.CONNACK };
            case MqttsnConstants.CONNECT:
                return new int[]{ MqttsnConstants.CONNACK };
            case MqttsnConstants.PUBLISH:
                return new int[]{ MqttsnConstants.PUBACK, MqttsnConstants.PUBREC, MqttsnConstants.PUBREL, MqttsnConstants.PUBCOMP };
            case MqttsnConstants.PUBREC:
                return new int[]{ MqttsnConstants.PUBREL };
            case MqttsnConstants.PUBREL:
                return new int[]{ MqttsnConstants.PUBCOMP };
            case MqttsnConstants.SUBSCRIBE:
                return new int[]{ MqttsnConstants.SUBACK};
            case MqttsnConstants.UNSUBSCRIBE:
                return new int[]{ MqttsnConstants.UNSUBACK };
            case MqttsnConstants.REGISTER:
                return new int[]{ MqttsnConstants.REGACK };
            case MqttsnConstants.PINGREQ:
                return new int[]{ MqttsnConstants.PINGRESP };
            case MqttsnConstants.DISCONNECT:
                return new int[]{ MqttsnConstants.DISCONNECT };
            case MqttsnConstants.SEARCHGW:
                return new int[]{ MqttsnConstants.GWINFO };
            case MqttsnConstants.WILLMSGREQ:
                return new int[]{ MqttsnConstants.WILLMSG };
            case MqttsnConstants.WILLTOPICREQ:
                return new int[]{ MqttsnConstants.WILLTOPIC };
            case MqttsnConstants.WILLTOPICUPD:
                return new int[]{ MqttsnConstants.WILLTOPICRESP };
            case MqttsnConstants.WILLMSGUPD:
                return new int[]{ MqttsnConstants.WILLMSGRESP };
            case MqttsnConstants.HELO:
                return new int[]{ MqttsnConstants.HELO };
            default:
                throw new MqttsnRuntimeException(
                        String.format("invalid message type detected [%s], non terminal and non response!", message.getMessageName()));
        }
    }

    @Override
    public boolean isTerminalMessage(IMqttsnMessage message) {
        switch(message.getMessageType()){
            case MqttsnConstants.PUBLISH:
                return getRegistry().getCodec().getQoS(message, false) <= 0;
            case MqttsnConstants.CONNACK:
            case MqttsnConstants.PUBACK:    //we delete QoS 1 sent PUBLISH on receipt of PUBACK
            case MqttsnConstants.PUBREL:    //we delete QoS 2 sent PUBLISH on receipt of PUBREL
            case MqttsnConstants.UNSUBACK:
            case MqttsnConstants.SUBACK:
            case MqttsnConstants.ADVERTISE:
            case MqttsnConstants.REGACK:
            case MqttsnConstants.PUBCOMP:   //we delete QoS 2 received PUBLISH on receipt of PUBCOMP
            case MqttsnConstants.PINGRESP:
            case MqttsnConstants.DISCONNECT:
            case MqttsnConstants.HELO:
            case MqttsnConstants.ENCAPSMSG:
            case MqttsnConstants.GWINFO:
            case MqttsnConstants.WILLMSG:
            case MqttsnConstants.WILLMSGRESP:
            case MqttsnConstants.WILLTOPIC:
            case MqttsnConstants.WILLTOPICRESP:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean requiresResponse(IMqttsnMessage message) {
        switch(message.getMessageType()){
            case MqttsnConstants.HELO:
                return ((MqttsnHelo)message).getUserAgent() == null;
            case MqttsnConstants.PUBLISH:
                return getRegistry().getCodec().getQoS(message, false) > 0;
            case MqttsnConstants.CONNECT:
            case MqttsnConstants.PUBREC:
            case MqttsnConstants.PUBREL:
            case MqttsnConstants.SUBSCRIBE:
            case MqttsnConstants.UNSUBSCRIBE:
            case MqttsnConstants.REGISTER:
            case MqttsnConstants.PINGREQ:
            case MqttsnConstants.DISCONNECT:
            case MqttsnConstants.SEARCHGW:
            case MqttsnConstants.WILLMSGREQ:
            case MqttsnConstants.WILLMSGUPD:
            case MqttsnConstants.WILLTOPICREQ:
            case MqttsnConstants.WILLTOPICUPD:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isPartOfOriginatingMessage(IMqttsnMessage message) {
        switch(message.getMessageType()){
            case MqttsnConstants.PUBLISH:
            case MqttsnConstants.CONNECT:
            case MqttsnConstants.SUBSCRIBE:
            case MqttsnConstants.UNSUBSCRIBE:
            case MqttsnConstants.REGISTER:
            case MqttsnConstants.PINGREQ:
            case MqttsnConstants.DISCONNECT:
            case MqttsnConstants.HELO:
            case MqttsnConstants.SEARCHGW:
            case MqttsnConstants.WILLMSGREQ:
            case MqttsnConstants.WILLMSGUPD:
            case MqttsnConstants.WILLTOPICREQ:
            case MqttsnConstants.WILLTOPICUPD:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void receiveMessage(IMqttsnContext context, IMqttsnMessage message)
            throws MqttsnException {

        try {

            if(!canHandle(context, message)){
                logger.log(Level.WARNING, String.format("mqtt-sn handler [%s <- %s] dropping message it could not handle [%s]",
                        context, message.getMessageName()));
                return;
            }

            if(message.isErrorMessage()){
                logger.log(Level.WARNING, String.format("mqtt-sn handler [%s <- %s] received error message [%s]",
                        registry.getOptions().getContextId(), context, message));
            }

            beforeHandle(context, message);

            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE, String.format("mqtt-sn handler [%s <- %s] handling inbound message [%s]",
                        registry.getOptions().getContextId(), context, message));
            }

            boolean errord = false;
            IMqttsnMessage originatingMessage = null;

            if(registry.getMessageStateService() != null){
                try {
                    originatingMessage =
                            registry.getMessageStateService().notifyMessageReceived(context, message);
                } catch(MqttsnException e){
                    errord = true;
                    logger.log(Level.WARNING, String.format("mqtt-sn handler [%s <- %s] state service errord, allow message lifecycle to handle [%s] -> [%s]",
                            registry.getOptions().getContextId(), context, e.getMessage()));
                }
            }

            IMqttsnMessage response = handle(context, originatingMessage, message, errord);

            //-- if the state service threw a wobbler but for some reason this didnt lead to an error message
            //-- we should just disconnect the device
            if(errord && !response.isErrorMessage()){
                logger.log(Level.WARNING, String.format("mqtt-sn handler [%s <- %s] state service errord, message handler did not produce an error, so overrule and disconnect [%s] -> [%s]",
                        registry.getOptions().getContextId(), context, message));
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
            logger.log(Level.WARNING,"handled with disconnect error encountered during receive;", e);
            handleResponse(context,
                    registry.getMessageFactory().createDisconnect());
            if(!registry.getRuntime().handleLocalDisconnect(context, e)) {
                throw e;
            }
        }
    }

    protected void beforeHandle(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {

    }

    protected IMqttsnMessage handle(IMqttsnContext context, IMqttsnMessage originatingMessage, IMqttsnMessage message, boolean errord)
            throws MqttsnException {

        IMqttsnMessage response = null;
        int msgType = message.getMessageType();

        switch (msgType) {
            case MqttsnConstants.CONNECT:
                response = handleConnect(context, message);
                if(!errord && !response.isErrorMessage()){
                    registry.getRuntime().handleConnected(context);
                }
                break;
            case MqttsnConstants.CONNACK:
                if(validateOriginatingMessage(context, originatingMessage, message)){
                    handleConnack(context, originatingMessage, message);
                    if(!errord && !message.isErrorMessage()){
                        registry.getRuntime().handleConnected(context);
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
                    response = getRegistry().getMessageFactory().createRegack(0,
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


    protected void afterHandle(IMqttsnContext context, IMqttsnMessage message, IMqttsnMessage response) throws MqttsnException {

        if(response != null && response.isErrorMessage()){
            //we need to remove any message that was marked inflight
            if(message.needsId()){
                if(registry.getMessageStateService().removeInflight(context, message.getId()) != null){
                    logger.log(Level.WARNING, "tidied up bad message that was marked inflight and yeilded error response");
                }
            }
        }
    }

    protected void afterResponse(IMqttsnContext context, IMqttsnMessage message, IMqttsnMessage response) throws MqttsnException {
    }

    protected boolean validateOriginatingMessage(IMqttsnContext context, IMqttsnMessage originatingMessage, IMqttsnMessage message) {
        if(originatingMessage == null){
            logger.log(Level.WARNING, String.format("[%s] no originating message found for acknowledgement [%s]; reaper probably moved this back to queue", context, message));
            return false;
        }
        return true;
    }

    protected void handleResponse(IMqttsnContext context, IMqttsnMessage response)
            throws MqttsnException {

        if(logger.isLoggable(Level.INFO)){
            logger.log(Level.INFO, String.format("mqtt-sn handler [%s -> %s] sending outbound message [%s]",
                    registry.getOptions().getContextId(), context, response));
        }

        registry.getTransport().writeToTransport(
                registry.getNetworkRegistry().getContext(context), response);
    }

    protected IMqttsnMessage handleConnect(IMqttsnContext context, IMqttsnMessage connect) throws MqttsnException {

        boolean will = false;
        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnConnect connectMessage = (MqttsnConnect) connect ;
            will = connectMessage.isWill();
        } else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnConnect_V2_0 connectMessage = (MqttsnConnect_V2_0) connect ;
            will = connectMessage.isWill();
        }

        if(will){
            return registry.getMessageFactory().createWillTopicReq();
        } else {
            return registry.getMessageFactory().createConnack(MqttsnConstants.RETURN_CODE_ACCEPTED);
        }
    }

    protected void handleConnack(IMqttsnContext context, IMqttsnMessage connect, IMqttsnMessage connack) throws MqttsnException {
    }

    protected IMqttsnMessage handleDisconnect(IMqttsnContext context, IMqttsnMessage originatingMessage, IMqttsnMessage message) throws MqttsnException, MqttsnCodecException {

        //-- if the disconnect is received in response to a disconnect we sent, lets not send another!
        if(originatingMessage != null){
            logger.log(Level.INFO, "disconnect received in response to my disconnect, dont send another!");
            return null;
        } else {
            if(registry.getRuntime().handleRemoteDisconnect(context)){
                return registry.getMessageFactory().createDisconnect();
            }
            return null;
        }
    }

    protected IMqttsnMessage handlePingreq(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException, MqttsnCodecException {
        return registry.getMessageFactory().createPingresp();
    }

    protected void handlePingresp(IMqttsnContext context, IMqttsnMessage originatingMessage, IMqttsnMessage message) throws MqttsnException {
    }

    protected IMqttsnMessage handleSubscribe(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException, MqttsnCodecException {

        int QoS = 0;
        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnSubscribe subscribe = (MqttsnSubscribe) message;
            QoS = subscribe.getQoS();
        } else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnSubscribe_V2_0 subscribe = (MqttsnSubscribe_V2_0) message;
            QoS = subscribe.getQoS();
        }
        return registry.getMessageFactory().createSuback(QoS, 0x00, MqttsnConstants.RETURN_CODE_ACCEPTED);
    }

    protected IMqttsnMessage handleUnsubscribe(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException, MqttsnCodecException {

        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnUnsubscribe unsub = (MqttsnUnsubscribe) message;

        } else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnUnsubscribe_V2_0 unsub = (MqttsnUnsubscribe_V2_0) message;
        }

        return registry.getMessageFactory().createUnsuback();
    }

    protected void handleSuback(IMqttsnContext context, IMqttsnMessage initial, IMqttsnMessage message) throws MqttsnException {

        boolean isError = false;
        int topicId = 0;
        int topicIdType = 0;
        int grantedQoS = 0;
        String topicPath = null;
        byte[] topicData = null;

        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnSuback suback = (MqttsnSuback) message;
            MqttsnSubscribe subscribe = (MqttsnSubscribe) initial;

            isError = suback.isErrorMessage();
            topicId = suback.getTopicId();
            grantedQoS = suback.getQoS();
            //NB: note the type is derived from the subscribe as v1.2 doesnt allow for the transmission on the suback
            topicIdType = subscribe.getTopicType();
            topicData = subscribe.getTopicData();
            topicPath = topicIdType == MqttsnConstants.TOPIC_PREDEFINED ? null : subscribe.getTopicName();

        } else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnSuback_V2_0 suback = (MqttsnSuback_V2_0) message;
            MqttsnSubscribe_V2_0 subscribe = (MqttsnSubscribe_V2_0) initial;

            isError = suback.isErrorMessage();
            topicId = suback.getTopicId();
            topicIdType = suback.getTopicIdType();
            grantedQoS = suback.getQoS();
            topicData = subscribe.getTopicData();
            topicPath = topicIdType == MqttsnConstants.TOPIC_PREDEFINED ? null : subscribe.getTopicName();
        }

        if(!isError){
            if(topicIdType == MqttsnConstants.TOPIC_NORMAL){
                registry.getTopicRegistry().register(context, topicPath, topicId);
            } else {
                topicPath = registry.getTopicRegistry().topicPath(context,
                        registry.getTopicRegistry().normalize(
                                (byte) topicIdType, topicData, false), false);
            }
            registry.getSubscriptionRegistry().subscribe(context, topicPath, grantedQoS);
        }
    }

    protected void handleUnsuback(IMqttsnContext context, IMqttsnMessage unsubscribe, IMqttsnMessage unsuback) throws MqttsnException {
        String topicPath = ((MqttsnUnsubscribe)unsubscribe).getTopicName();
        registry.getSubscriptionRegistry().unsubscribe(context, topicPath);
    }

    protected IMqttsnMessage handleRegister(IMqttsnContext context, IMqttsnMessage message)
            throws MqttsnException, MqttsnCodecException {
        MqttsnRegister register = (MqttsnRegister) message;
        return registry.getMessageFactory().createRegack(register.getTopicId(), MqttsnConstants.RETURN_CODE_ACCEPTED);
    }

    protected void handleRegack(IMqttsnContext context, IMqttsnMessage register, IMqttsnMessage msg) throws MqttsnException {
        String topicPath = ((MqttsnRegister)register).getTopicName();
        int topicId = 0;
        boolean isError = false;
        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnRegack regack = (MqttsnRegack) msg;
            topicId = regack.getTopicId();
            isError = regack.isErrorMessage();

        }
        else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnRegack_V2_0 regack = (MqttsnRegack_V2_0) msg;
            topicId = regack.getTopicId();
            isError = regack.isErrorMessage();
        }

        if(!isError){
            registry.getTopicRegistry().register(context, topicPath, topicId);
        } else {
            logger.log(Level.WARNING, String.format("received error regack response [%s]; Msg=%s", context, msg));
        }
    }

    protected IMqttsnMessage handlePublish(IMqttsnContext context, IMqttsnMessage message)
            throws MqttsnException, MqttsnCodecException {

        int QoS = 0;
        int topicIdType = 0;
        int topicDataAsInt = 0;
        byte[] topicData = null;
        byte[] data = null;
        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2){
            MqttsnPublish publish = (MqttsnPublish) message;
            QoS = publish.getQoS();
            topicIdType = publish.getTopicType();
            topicData = publish.getTopicData();
            data = publish.getData();
            topicDataAsInt = publish.readTopicDataAsInteger();
        } else if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0){
            MqttsnPublish_V2_0 publish = (MqttsnPublish_V2_0) message;
            QoS = publish.getQoS();
            topicIdType = publish.getTopicIdType();
            topicData = publish.getTopicData();
            data = publish.getData();
            topicDataAsInt = publish.readTopicDataAsInteger();
        }

        IMqttsnMessage response = null;

        TopicInfo info = registry.getTopicRegistry().normalize((byte) topicIdType, topicData, false);
        String topicPath = registry.getTopicRegistry().topicPath(context, info, true);
        if(registry.getAuthorizationService() != null){
            if(!registry.getAuthorizationService().allowedToPublish(context, topicPath, data.length, QoS)){
                logger.log(Level.WARNING, String.format("authorization service rejected publish from [%s] to [%s]", context, topicPath));
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

    protected void handlePuback(IMqttsnContext context, IMqttsnMessage originatingMessage, IMqttsnMessage message)
            throws MqttsnException {
    }

    protected IMqttsnMessage handlePubrel(IMqttsnContext context, IMqttsnMessage message)
            throws MqttsnException, MqttsnCodecException {
        return registry.getMessageFactory().createPubcomp();
    }

    protected IMqttsnMessage handlePubrec(IMqttsnContext context, IMqttsnMessage message)
            throws MqttsnException, MqttsnCodecException {
        return registry.getMessageFactory().createPubrel();
    }

    protected void handlePubcomp(IMqttsnContext context, IMqttsnMessage originatingMessage, IMqttsnMessage message)
            throws MqttsnException {
    }

    protected void handleAdvertise(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {
    }

    protected void handleEncapsmsg(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {
    }

    protected IMqttsnMessage handleSearchGw(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {
        return null;
    }

    protected void handleGwinfo(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {
    }

    protected IMqttsnMessage handleHelo(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {
        MqttsnHelo helo = (MqttsnHelo) message;
        if(helo.getUserAgent() == null){
            //this is a request for a HELO reply
            helo = new MqttsnHelo();
            helo.setUserAgent(getRegistry().getRuntime().getUserAgent());
            return helo;

        } else {
            logger.log(Level.INFO, String.format("received HELO reply from [%s] -> userAgent [%s]", context, helo.getUserAgent()));
            return null;
        }
    }

    protected IMqttsnMessage handleWillmsgreq(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {

        MqttsnWillData willData = registry.getWillRegistry().getWillMessage(context);
        byte[] willMsg = willData.getData();
        return registry.getMessageFactory().createWillMsg(willMsg);
    }

    protected IMqttsnMessage handleWillmsg(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {

        MqttsnWillmsg update = (MqttsnWillmsg) message;
        byte[] data = update.getMsgData();
        registry.getWillRegistry().updateWillMessage(context, data);
        return registry.getMessageFactory().createConnack(MqttsnConstants.RETURN_CODE_ACCEPTED);
    }

    protected IMqttsnMessage handleWillmsgupd(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {

        MqttsnWillmsgupd update = (MqttsnWillmsgupd) message;
        byte[] data = update.getMsgData();
        registry.getWillRegistry().updateWillMessage(context, data);
        return registry.getMessageFactory().createWillMsgResp(MqttsnConstants.RETURN_CODE_ACCEPTED);
    }

    protected void handleWillmsgresp(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {
    }

    protected IMqttsnMessage handleWilltopicreq(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {

        MqttsnWillData willData = registry.getWillRegistry().getWillMessage(context);
        int QoS = willData.getQos();
        boolean retain = willData.isRetain();
        String topicPath = willData.getTopicPath().toString();
        return registry.getMessageFactory().createWillTopic(QoS, retain, topicPath);
    }

    protected IMqttsnMessage handleWilltopic(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {

        //when in connect interaction, the will topic should yield a response of will message req

        MqttsnWilltopic update = (MqttsnWilltopic) message;
        int qos = update.getQoS();
        boolean retain = update.isRetainedPublish();
        String topicPath = update.getWillTopicData();
        registry.getWillRegistry().updateWillTopic(context, topicPath, qos, retain);
        return registry.getMessageFactory().createWillMsgReq();
    }

    protected IMqttsnMessage handleWilltopicupd(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {
        MqttsnWilltopicudp update = (MqttsnWilltopicudp) message;
        String topicPath = update.getWillTopicData();
        int qos = update.getQoS();
        boolean retain = update.isRetainedPublish();
        registry.getWillRegistry().updateWillTopic(context, topicPath, qos, retain);
        return registry.getMessageFactory().createWillTopicResp(MqttsnConstants.RETURN_CODE_ACCEPTED);
    }

    protected void handleWilltopicresp(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {
    }
}
