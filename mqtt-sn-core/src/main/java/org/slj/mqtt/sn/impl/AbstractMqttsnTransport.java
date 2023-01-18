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
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.IMqttsnMessageContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.IPacketTXRXJob;
import org.slj.mqtt.sn.spi.*;

import java.util.List;
import java.util.concurrent.Future;

/**
 * The abstract transport implementation provides many of the requisite behaviours required of the
 * transport layer, including message marshalling, thread handling (handoff), authority checking and traffic listener notification.
 * You should sub-class this base as a starting point for you implementations.
 */
public abstract class AbstractMqttsnTransport
        extends AbstractTransport implements IMqttsnTransport {


    protected void receiveFromTransportInternal(INetworkContext networkContext, byte[] data) {

        boolean isDisconnect = false;
        try {

            IMqttsnMessage message = getRegistry().getCodec().decode(data);
            isDisconnect = registry.getCodec().isDisconnect(message);
            logger.debug("receiving {} protocol bytes {} from {} on thread {}",
                        data.length, message.getMessageName(), networkContext, Thread.currentThread().getName());

            boolean authd = true;
            int protocolVersion = MqttsnConstants.PROTOCOL_VERSION_UNKNOWN;
            //-- if we detect an inbound id packet, we should authorise the context every time (even if the impl just reuses existing auth)
            if (message instanceof IMqttsnProtocolVersionPacket) {
                protocolVersion = ((IMqttsnProtocolVersionPacket)message).getProtocolVersion();
                if(!registry.getCodec().supportsVersion(protocolVersion)){
                    logger.warn("codec does not support presented protocol version {} for {}", protocolVersion, networkContext);
                    throw new MqttsnCodecException("unsupported codec version");
                }
            }


            boolean assignedClientId = false;
            if (message instanceof IMqttsnIdentificationPacket) {
                String clientId = ((IMqttsnIdentificationPacket) message).getClientId();
                clientId = getRegistry().getClientIdFactory().resolvedClientId(clientId);
                if(clientId == null){
                    if(protocolVersion == MqttsnConstants.PROTOCOL_VERSION_2_0){
                        clientId = getRegistry().getClientIdFactory().createClientId(null);
                        logger.info("detected <null> clientId, creating an assignedClient using clientId factory {}", clientId);
                    }
                }
                if (!registry.getNetworkRegistry().hasBoundSessionContext(networkContext)) {
                    authd = registry.getMessageHandler().authorizeContext(networkContext, clientId, protocolVersion, assignedClientId);
                } else {
                    //-- need to check the context from the network matches the supplied clientId in case of address reuse..
                    IClientIdentifierContext mqttsnContext = registry.getNetworkRegistry().getMqttsnContext(networkContext);
                    if(clientId == null || "".equals(clientId.trim()) && message.getMessageType() == MqttsnConstants.PINGREQ){
                        logger.info("{} received with no clientId, continue with previous clientId on network address {}",
                                message, mqttsnContext);
                    }
                    else if(!mqttsnContext.getId().equals(clientId)){
                        //-- the connecting device is presenting a different clientId to the previous one held against the
                        //-- network address - we must ensure they dont interfere..
                        if(checkNewClientIdMatchesClientIdFromExistingContext()){
                            logger.warn("detected mis-matched clientId for network address {} -> {} != {} invalidate, and re-auth",
                                    networkContext.getNetworkAddress(), mqttsnContext, clientId);
                            getRegistry().getRuntime().handleConnectionLost(mqttsnContext, null);
                            authd = registry.getMessageHandler().authorizeContext(networkContext, clientId, protocolVersion, assignedClientId);
                        } else {
                            logger.warn("detected mis-matched clientId for network address {} -> {} != {} implementation says this is OK, ignore",
                                    networkContext.getNetworkAddress(), mqttsnContext, clientId);
                        }
                    }
                }
            } else {
                //-- sort the case where publish -1 can be recieved without an authd context from the
                //-- network address alone
                boolean isPublish = registry.getCodec().isPublish(message);
                boolean qosM1 = false;
                if(isPublish){
                    qosM1 =  registry.getCodec().getQoS(message, false) == -1;
                }
                if (!registry.getNetworkRegistry().hasBoundSessionContext(networkContext) &&
                        isPublish && qosM1) {
                    logger.warn("detected non authorised publish -1, apply for temporary auth from network context {}", networkContext);
                    authd = registry.getMessageHandler().temporaryAuthorizeContext(networkContext);
                }
            }

            if (authd && registry.getNetworkRegistry().hasBoundSessionContext(networkContext)) {
                notifyTrafficReceived(networkContext, data, message);
                IMqttsnMessageContext messageContext = getRegistry().getContextFactory().createMessageContext(networkContext);
                registry.getMessageHandler().receiveMessage(messageContext, message);
            } else {
                if(!isDisconnect){
                    logger.warn("auth could not be established for {}, send disconnect that is not processed by application",
                            networkContext.getNetworkAddress());
                    writeMqttSnMessageToTransport(networkContext,
                            registry.getMessageFactory().createDisconnect(), false);
                } else {
                    logger.warn("received DISCONNECT from not authd context, ignore {}",
                            networkContext.getNetworkAddress());
                }
            }
        }
        catch(MqttsnCodecException e){
            logger.error("protocol error - sending payload format error disconnect;", e);
            if(!isDisconnect){
                writeMqttSnMessageToTransport(networkContext,
                        registry.getMessageFactory().createDisconnect(
                                MqttsnConstants.RETURN_CODE_PAYLOAD_FORMAT_INVALID, e.getMessage()), false);
            }
        }
        catch(MqttsnSecurityException e){
            logger.error("security exception encountered processing, drop packet;", e);
        }
        catch(Throwable t){
            logger.error("error, send generic error disconnect;", t);
            if(!isDisconnect){
                writeMqttSnMessageToTransport(networkContext,
                        registry.getMessageFactory().createDisconnect(
                                MqttsnConstants.RETURN_CODE_SERVER_UNAVAILABLE, t.getMessage()), false);
            }
        }
    }

    @Override
    public Future<IPacketTXRXJob> writeToTransportWithCallback(INetworkContext context, IMqttsnMessage message, Runnable r) {
        return writeMqttSnMessageToTransport(context, message, true, r);
    }

    @Override
    public Future<IPacketTXRXJob> writeToTransport(INetworkContext context, IMqttsnMessage message) {
        byte[] data = registry.getCodec().encode(message);
        return super.writeToTransport(context, data);
    }

    protected Future<IPacketTXRXJob> writeMqttSnMessageToTransport(INetworkContext context, IMqttsnMessage message, boolean notifyListeners){
        return writeMqttSnMessageToTransport(context, message, notifyListeners, null);
    }

    protected Future<IPacketTXRXJob> writeMqttSnMessageToTransport(INetworkContext context, IMqttsnMessage message, boolean notifyListeners, Runnable r){
        byte[] data = registry.getCodec().encode(message);
        Future<IPacketTXRXJob> c = super.writeToTransportWithCallback(context, data, r);
        if(notifyListeners) notifyTrafficSent(context, data, message);
        return c;
    }

    private void notifyTrafficReceived(final INetworkContext context, byte[] data, IMqttsnMessage message) {
        List<IMqttsnTrafficListener> list = getRegistry().getRuntime().getTrafficListeners();
        if(list != null && !list.isEmpty()){
            list.forEach(l -> l.trafficReceived(context, data, message));
        }
    }

    private void notifyTrafficSent(final INetworkContext context, byte[] data, IMqttsnMessage message) {
        List<IMqttsnTrafficListener> list = getRegistry().getRuntime().getTrafficListeners();
        if(list != null && !list.isEmpty()){
            list.forEach(l -> l.trafficSent(context, data, message));
        }
    }

    protected boolean checkNewClientIdMatchesClientIdFromExistingContext(){
        return true;
    }
}