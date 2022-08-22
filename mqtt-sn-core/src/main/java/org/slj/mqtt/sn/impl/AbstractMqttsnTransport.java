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
import org.slj.mqtt.sn.model.IMqttsnSessionState;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.MqttsnSecurityOptions;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.Security;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;

/**
 * The abstract transport implementation provides many of the requisite behaviours required of the
 * transport layer, including message marshalling, thread handling (handoff), authority checking and traffic listener notification.
 * You should sub-class this base as a starting point for you implementations.
 */
public abstract class AbstractMqttsnTransport<U extends IMqttsnRuntimeRegistry>
        extends MqttsnService<U> implements IMqttsnTransport {

    protected ExecutorService protocolProcessor;
    protected ExecutorService egressPublishProcessor;

    public void connectionLost(INetworkContext context, Throwable t){
        if(registry != null && context != null){
            registry.getRuntime().handleConnectionLost(
                    registry.getNetworkRegistry().getSessionContext(context), t);
        }
    }

    @Override
    public void start(U runtime) throws MqttsnException {
        super.start(runtime);
        protocolProcessor = runtime.getRuntime().createManagedExecutorService(String.format("mqtt-sn-transport-%s-", System.identityHashCode(runtime)),
                runtime.getOptions().getTransportProtocolHandoffThreadCount());
        egressPublishProcessor = runtime.getRuntime().createManagedExecutorService(String.format("mqtt-sn-transport-egress-%s-", System.identityHashCode(runtime)),
                runtime.getOptions().getTransportPublishHandoffThreadCount());
    }

    @Override
    public void stop() throws MqttsnException {
        super.stop();
        try {
            if(protocolProcessor != null){
                registry.getRuntime().closeManagedExecutorService(protocolProcessor);
            }
        } finally {
            if(egressPublishProcessor != null){
                registry.getRuntime().closeManagedExecutorService(egressPublishProcessor);
            }
        }
    }

    public boolean restartOnLoss(){
        return true;
    }

    @Override
    public void receiveFromTransport(INetworkContext context, byte[] data) {
        if(protocolProcessor != null){
            getRegistry().getRuntime().async(protocolProcessor,
                    () -> receiveFromTransportInternal(context, data));
        }
    }

    @Override
    public Future<INetworkContext> writeToTransport(INetworkContext context, IMqttsnMessage message) {
        return getRegistry().getRuntime().async(getRegistry().getCodec().isPublish(message) ?
                        egressPublishProcessor : protocolProcessor,
                    () -> writeToTransportInternal(context, message, true), context);
    }

    public void writeToTransportWithWork(INetworkContext context, IMqttsnMessage message, Runnable callback) {
        getRegistry().getRuntime().asyncWithCallback(getRegistry().getCodec().isPublish(message) ?
                egressPublishProcessor : protocolProcessor,
                () -> writeToTransportInternal(context, message, true), callback);
    }

    protected void receiveFromTransportInternal(INetworkContext networkContext, byte[] data) {
        try {
            if (!registry.getMessageHandler().running()) {
                return;
            }
            if (data.length > registry.getOptions().getMaxProtocolMessageSize()) {
                logger.log(Level.SEVERE, String.format("receiving [%s] bytes - max allowed message size [%s] - error",
                        data.length, registry.getOptions().getMaxProtocolMessageSize()));
                throw new MqttsnRuntimeException("received message was larger than allowed max");
            }

            if (registry.getOptions().isWireLoggingEnabled()) {
                logger.log(Level.INFO, String.format("receiving [%s] ",
                        MqttsnWireUtils.toBinary(data)));
            }

            if(registry.getSecurityService().protocolIntegrityEnabled()){
                data = registry.getSecurityService().readVerified(data);
            }

            IMqttsnMessage message = getRegistry().getCodec().decode(data);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, String.format("receiving [%s] protocol bytes (%s) from [%s] on thread [%s]",
                        data.length, message.getMessageName(), networkContext, Thread.currentThread().getName()));
            }

            boolean authd = true;
            int protocolVersion = MqttsnConstants.PROTOCOL_VERSION_UNKNOWN;
            //-- if we detect an inbound id packet, we should authorise the context every time (even if the impl just reuses existing auth)
            if (message instanceof IMqttsnProtocolVersionPacket) {
                protocolVersion = ((IMqttsnProtocolVersionPacket)message).getProtocolVersion();
                if(!registry.getCodec().supportsVersion(protocolVersion)){
                    logger.log(Level.WARNING, String.format("codec does not support presented protocol version [%s] for [%s]", protocolVersion, networkContext));
                    throw new MqttsnCodecException("unsupported codec version");
                }
            }

            boolean assignedClientId = false;
            if (message instanceof IMqttsnIdentificationPacket) {
                String clientId = ((IMqttsnIdentificationPacket) message).getClientId();
                if(clientId == null){
                    if(protocolVersion == MqttsnConstants.PROTOCOL_VERSION_2_0){
                        clientId = generateAssignedClientId();
                    }
                }
                if (!registry.getNetworkRegistry().hasBoundSessionContext(networkContext)) {
                    authd = registry.getMessageHandler().authorizeContext(networkContext, clientId, protocolVersion, assignedClientId);
                } else {
                    //-- need to check the context from the network matches the supplied clientId in case of address reuse..
                    IMqttsnContext mqttsnContext = registry.getNetworkRegistry().getSessionContext(networkContext);
                    if(clientId == null || "".equals(clientId.trim()) && message.getMessageType() == MqttsnConstants.PINGREQ){
                        logger.log(Level.INFO, String.format("%s received with no clientId, continue with previous clientId on network address [%s]",
                                message, mqttsnContext));
                    }
                    else if(!mqttsnContext.getId().equals(clientId)){
                        //-- the connecting device is presenting a different clientId to the previous one held against the
                        //-- network address - we must ensure they dont interfere..
                        logger.log(Level.WARNING, String.format("detected mis-matched clientId for network address [%s] -> [%s] != [%s] invalidate, and re-auth",
                                networkContext.getNetworkAddress(), mqttsnContext, clientId));
                        getRegistry().getRuntime().handleConnectionLost(mqttsnContext, null);
                        authd = registry.getMessageHandler().authorizeContext(networkContext, clientId, protocolVersion, assignedClientId);
                    }
                }
            } else {
                //-- sort the case where publish -1 can be recieved without an authd context from the
                //-- network address alone
                if (!registry.getNetworkRegistry().hasBoundSessionContext(networkContext) &&
                        registry.getCodec().isPublish(message) &&
                        registry.getCodec().getData(message).getQos() == -1) {
                    logger.log(Level.INFO, String.format("detected non authorised publish -1, apply for temporary auth from network context [%s]", networkContext));
                    authd = registry.getMessageHandler().temporaryAuthorizeContext(networkContext);
                }
            }

            if (authd && registry.getNetworkRegistry().hasBoundSessionContext(networkContext)) {
                notifyTrafficReceived(networkContext, data, message);
                IMqttsnContext context = registry.getNetworkRegistry().getSessionContext(networkContext);
                registry.getMessageHandler().receiveMessage(context, message);
            } else {
                logger.log(Level.WARNING, "auth could not be established, send disconnect that is not processed by application");
                writeToTransportInternal(networkContext,
                        registry.getMessageFactory().createDisconnect(), false);
            }
        }
        catch(MqttsnCodecException e){
            logger.log(Level.SEVERE, "protocol error - sending payload format error disconnect;", e);
            writeToTransportInternal(networkContext,
                    registry.getMessageFactory().createDisconnect(MqttsnConstants.RETURN_CODE_PAYLOAD_FORMAT_INVALID, e.getMessage()), false);
        }
        catch(MqttsnSecurityException e){
            logger.log(Level.SEVERE, "security exception encountered processing, drop packet;", e);
        }
        catch(Throwable t){
            logger.log(Level.SEVERE, "unknown error;", t);
        }
    }

    protected String generateAssignedClientId(){
        String assignedClientId = UUID.randomUUID().toString();
        logger.log(Level.INFO, String.format("detected <null> clientId, creating an assignedClient [%s]", assignedClientId));
        return assignedClientId;
    }

    protected boolean writeToTransportInternal(INetworkContext context, IMqttsnMessage message, boolean notifyListeners){
        try {
            byte[] data = registry.getCodec().encode(message);

            if(registry.getSecurityService().protocolIntegrityEnabled()){
                data = registry.getSecurityService().writeVerified(data);
            }

            if(data.length > registry.getOptions().getMaxProtocolMessageSize()){
                logger.log(Level.SEVERE, String.format("cannot send [%s] bytes - max allowed message size [%s]",
                        data.length, registry.getOptions().getMaxProtocolMessageSize()));
                throw new MqttsnRuntimeException("cannot send messages larger than allowed max");
            }

            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE, String.format("writing [%s] bytes (%s) to [%s] on thread [%s]",
                        data.length, message.getMessageName(), context, Thread.currentThread().getName()));
            }

            if(registry.getOptions().isWireLoggingEnabled()){
                logger.log(Level.INFO, String.format("writing [%s] ",
                        MqttsnWireUtils.toBinary(data)));
            }

            writeToTransport(context, data);
            if(notifyListeners) notifyTrafficSent(context, data, message);
            return true;
        } catch(Throwable e){
            logger.log(Level.SEVERE, String.format("[%s] transport layer error sending buffer", context), e);
            return false;
        }
    }

    private void notifyTrafficReceived(final INetworkContext context, byte[] data, IMqttsnMessage message) {
        List<IMqttsnTrafficListener> list = getRegistry().getTrafficListeners();
        if(list != null && !list.isEmpty()){
            list.forEach(l -> l.trafficReceived(context, data, message));
        }
    }

    private void notifyTrafficSent(final INetworkContext context, byte[] data, IMqttsnMessage message) {
        List<IMqttsnTrafficListener> list = getRegistry().getTrafficListeners();
        if(list != null && !list.isEmpty()){
            list.forEach(l -> l.trafficSent(context, data, message));
        }
    }

    protected abstract void writeToTransport(INetworkContext context, byte[] data) throws MqttsnException ;

    protected static ByteBuffer wrap(byte[] arr){
        return wrap(arr, arr.length);
    }

    protected static ByteBuffer wrap(byte[] arr, int length){
        return ByteBuffer.wrap(arr, 0 , length);
    }

    protected static byte[] drain(ByteBuffer buffer){
        byte[] arr = new byte[buffer.remaining()];
        buffer.get(arr, 0, arr.length);
        return arr;
    }
}
