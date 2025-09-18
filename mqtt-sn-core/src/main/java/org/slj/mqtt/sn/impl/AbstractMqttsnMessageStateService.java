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
import org.slj.mqtt.sn.MqttsnMessageRules;
import org.slj.mqtt.sn.PublishData;
import org.slj.mqtt.sn.model.*;
import org.slj.mqtt.sn.model.session.IQueuedPublishMessage;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.TopicPath;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnPublish;
import org.slj.mqtt.sn.wire.version2_0.payload.MqttsnPublish_V2_0;

import java.util.*;
import java.util.concurrent.*;

public abstract class AbstractMqttsnMessageStateService
        extends AbstractMqttsnBackoffThreadService implements IMqttsnMessageStateService {

    protected static final Integer WEAK_ATTACH_ID = new Integer(MqttsnConstants.UNSIGNED_MAX_16 + 1);
    protected boolean clientMode;
    protected Map<IClientIdentifierContext, Long> lastActiveMessage;
    protected Map<IClientIdentifierContext, Long> lastMessageSent;
    protected Map<IClientIdentifierContext, Long> lastMessageReceived;
    protected Map<LastIdContext, Integer> lastUsedMsgIds;
    protected Map<IClientIdentifierContext, ScheduledFuture<IMqttsnMessageQueueProcessor.RESULT>> flushOperations;
    protected ScheduledExecutorService executorService = null;
    protected int loopTimeout;

    public AbstractMqttsnMessageStateService(boolean clientMode) {
        this.clientMode = clientMode;
    }

    @Override
    public synchronized void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        flushOperations = new HashMap<>();
        executorService = runtime.getRuntime().createManagedScheduledExecutorService("mqtt-sn-scheduled-queue-flush-",
                runtime.getOptions().getQueueProcessorThreadCount());
        lastUsedMsgIds = Collections.synchronizedMap(new HashMap<>());
        lastMessageReceived = Collections.synchronizedMap(new HashMap<>());
        lastMessageSent = Collections.synchronizedMap(new HashMap<>());
        lastActiveMessage = Collections.synchronizedMap(new HashMap<>());
        loopTimeout  = runtime.getOptions().getStateLoopTimeout();
        super.start(runtime);
    }

    protected void scheduleWork(IClientIdentifierContext context, int time, TimeUnit unit){
        boolean process = !flushOperations.containsKey(context) ||
                !flushOperations.get(context).isDone();
        ScheduledFuture<IMqttsnMessageQueueProcessor.RESULT> future = null;
        if(process){
            future = executorService.schedule(() -> {
                        IMqttsnMessageQueueProcessor.RESULT result =
                                IMqttsnMessageQueueProcessor.RESULT.REMOVE_PROCESS;
                        boolean shouldProcess = !flushOperations.containsKey(context) ||
                                !flushOperations.get(context).isDone();

                        logger.debug("!! processing scheduled work for context {} -> {}", context, shouldProcess);

                        if(shouldProcess){
                            result = processQueue(context);
                            switch(result){
                                case REMOVE_PROCESS:
                                    synchronized (flushOperations){
                                        flushOperations.remove(context);
                                    }
                                    logger.debug("removed context from work list {}", context);
                                    break;
                                case BACKOFF_PROCESS:
                                    Long lastReceived = lastMessageReceived.get(context);
                                    long delta = lastReceived == null ? 0 : System.currentTimeMillis() - lastReceived;
                                    boolean remove = registry.getOptions().getActiveContextTimeout() < delta;
                                    logger.debug("backoff requested for {}, activity delta is {}, remove work ? {}", context, delta, remove);
                                    if(remove){
                                        synchronized (flushOperations){
                                            flushOperations.remove(context);
                                        }
                                    }
                                    else {
                                        scheduleWork(context, Math.max(100, registry.getOptions().getMinFlushTime()), TimeUnit.MILLISECONDS);
                                    }
                                    break;
                                case REPROCESS:
                                    scheduleWork(context, Math.max(1, registry.getOptions().getMinFlushTime()), TimeUnit.MILLISECONDS);
                            }
                        } else {
                            flushOperations.remove(context);
                        }
                        logger.debug("context {} flush completed with {}", context, result);
                        return result;
                    }, time, unit);
        }

        if(future != null){
            synchronized (flushOperations){
                flushOperations.put(context, future);
            }
        }

    }

    protected IMqttsnMessageQueueProcessor.RESULT processQueue(IClientIdentifierContext context){
        try {
            return registry.getQueueProcessor().process(context);
        } catch(Exception e){
            logger.error("error encountered processing queue", e);
            return IMqttsnMessageQueueProcessor.RESULT.REMOVE_PROCESS;
        }
    }

    @Override
    public void unscheduleFlush(IClientIdentifierContext context) {
        ScheduledFuture<?> future = null;
        if(flushOperations.containsKey(context)) {
            synchronized (flushOperations) {
                future = flushOperations.remove(context);
            }
        }
        if(future != null){
            future.cancel(false);
        }
    }

    @Override
    public void scheduleFlush(IClientIdentifierContext context)  {
        if(!flushOperations.containsKey(context) ||
                flushOperations.get(context).isDone()){
            
            logger.debug("scheduling flush for {}", context);
            if(executorService != null &&
                    !executorService.isTerminated() && !executorService.isShutdown()){
                scheduleWork(context,
                        ThreadLocalRandom.current().nextInt(1, 10), TimeUnit.MILLISECONDS);
            }

        }
    }

    @Override
    protected long doWork() {

        //-- monitor active context timeouts
        int activeMessageTimeout = registry.getOptions().getActiveContextTimeout();
        if(activeMessageTimeout > 0){
            Set<IClientIdentifierContext> copy = null;
            synchronized (lastActiveMessage){
                copy = new HashSet<>(lastActiveMessage.keySet());
            }
            Iterator<IClientIdentifierContext> itr = copy.iterator();
            while(itr.hasNext()){
                IClientIdentifierContext context = itr.next();
                Long time = lastActiveMessage.get(context);
                if((time + activeMessageTimeout) < System.currentTimeMillis()){
                    //-- context is timedout
                    registry.getRuntime().handleActiveTimeout(context);
                    lastActiveMessage.remove(context);
                }
            }
        }

        try {
            registry.getMessageRegistry().tidy();
        } catch(Exception e){
            logger.error("error tidying message registry on state thread;", e);
        }
        return Math.max(loopTimeout, 1);
    }

    protected boolean allowedToSend(IClientIdentifierContext context, IMqttsnMessage message) throws MqttsnException {
        return true;
    }

    @Override
    public MqttsnWaitToken sendMessage(IClientIdentifierContext context, IMqttsnMessage message) throws MqttsnException {
        return sendMessageInternal(context, message, null);
    }

    @Override
    public MqttsnWaitToken sendPublishMessage(IClientIdentifierContext context, TopicInfo info, IQueuedPublishMessage queuedPublishMessage) throws MqttsnException {

        byte[] payload = registry.getMessageRegistry().get(queuedPublishMessage.getDataRefId());

        if(registry.getSecurityService().payloadIntegrityEnabled()){
            INetworkContext networkContext = registry.getNetworkRegistry().getContext(context);
            payload = registry.getSecurityService().writeVerified(networkContext, payload);
        }
        MqttsnConstants.TOPIC_TYPE type = info.getType();
        int topicId = info.getTopicId();
        if(type == MqttsnConstants.TOPIC_TYPE.SHORT && topicId == 0){
            String topicPath = info.getTopicPath();
            int length = topicPath.length();
            if(length == 0 || length > 2){
                throw new MqttsnException("short topics should be 1 or 2 chars in the length");
            }
            topicId = MqttsnWireUtils.read16bit((byte) topicPath.charAt(0),
                    length == 2 ? (byte) topicPath.charAt(1) : 0x00);
        }

        IMqttsnMessage publish = registry.getMessageFactory().createPublish(queuedPublishMessage.getGrantedQoS(),
                isDUPDelivery(queuedPublishMessage),
                queuedPublishMessage.getData().isRetained(), type, topicId,
                payload);
        if(queuedPublishMessage.getPacketId() != 0){
            //-- ensure if we have sent a message before we use the same ID again
            publish.setId(queuedPublishMessage.getPacketId());
        }
        return sendMessageInternal(context, publish, queuedPublishMessage);
    }

    protected boolean isDUPDelivery(IQueuedPublishMessage message){
        return message.getRetryCount() > 1 || message.getPacketId() > 0;
    }

    protected MqttsnWaitToken sendMessageInternal(IClientIdentifierContext context, IMqttsnMessage message, IQueuedPublishMessage queuedPublishMessage) throws MqttsnException {

        if(!allowedToSend(context, message)){
            logger.warn("allowed to send {} check failed {}", message, context);
            throw new MqttsnExpectationFailedException("allowed to send check failed");
        }

        //if I send a message that is an ACK it is in response to something received to the remote space
        IMqttsnOriginatingMessageSource source = MqttsnMessageRules.isAck(message, true) ?
                IMqttsnOriginatingMessageSource.REMOTE : IMqttsnOriginatingMessageSource.LOCAL;

        int count = countInflight(context, source);
        if(count >= registry.getOptions().getMaxMessagesInflight()) {
            if (clientMode) {
                int retry = 0;
                while ((count = countInflight(context, source)) >=
                        registry.getOptions().getMaxMessagesInflight()
                        && ++retry <
                        registry.getOptions().getMaxErrorRetries()) {
                    try {
                        long sleep = MqttsnUtils.getExponentialBackoff(retry, true);
                        logger.debug("exp. backoff ({}) for client send {}, max inflight reached for direction {} ",
                                sleep, message, source);
                        Thread.sleep(sleep);
                    } catch(Exception e){
                    }
                }

                Optional<InflightMessage> blockingMessage =
                        getInflightMessages(context, source).values().stream().findFirst();
                if (blockingMessage.isPresent()) {
                    MqttsnWaitToken token = blockingMessage.get().getToken();
                    if (token != null) {
                        waitForCompletion(context, token);
                        if (!token.isError() && token.isComplete()) {
                            //-- recurse point
                            return sendMessageInternal(context, message, queuedPublishMessage);
                        } else {
                            logger.warn("unable to send, partial send in progress with token {}", token);
                            throw new MqttsnExpectationFailedException("unable to send message, partial send in progress");
                        }
                    }
                }
            } else {
                logger.warn("{} max inflight message number reached ({}), fail-fast for sending {} - {}", context, count, source, message);
                throw new MqttsnExpectationFailedException("max number of inflight messages reached for " + source);
            }
        }

        try {

            MqttsnWaitToken token = null;
            boolean requiresResponse;
            if((requiresResponse = MqttsnMessageRules.requiresResponse(getRegistry().getCodec(), message))){
                token = markInflight(source, context, message, queuedPublishMessage);
            }

            logger.debug("mqtt-sn state [{} -> {}] sending message {}, marking inflight ? {}",
                                registry.getOptions().getContextId(), context, message, requiresResponse);

            Runnable callback = null;
            if(!requiresResponse && registry.getCodec().isPublish(message)){
                PublishData data = registry.getCodec().getData(message);
                CommitOperation op = CommitOperation.outbound(context,
                        queuedPublishMessage.getDataRefId(), data, message);
                op.data.setTopicPath(queuedPublishMessage.getData().getTopicPath());

                //-- wait until the transport confirms the send else the confirm could happen before the backpressure is relieved
                callback = () -> {
                    long time = System.currentTimeMillis();
                    if(registry.getCodec().isActiveMessage(message) &&
                            !message.isErrorMessage()){
                        lastActiveMessage.put(context, time);
                    }
                    lastMessageSent.put(context, time);
                    confirmPublish(op);
                };
            } else {
                callback = () -> {
                    long time = System.currentTimeMillis();
                    if(registry.getCodec().isActiveMessage(message) &&
                            !message.isErrorMessage()){
                        lastActiveMessage.put(context, time);
                    }
                    lastMessageSent.put(context, time);
                };
            }
            INetworkContext networkContext = registry.getNetworkRegistry().getContext(context);
            Future<IPacketTXRXJob> f = ((IMqttsnTransport)networkContext.getTransport()).writeToTransportWithCallback(networkContext, message, callback);
            if(f == null){
                token.markError("unable to send packet");
            }
            return token;

        } catch(Exception e){
            throw new MqttsnException("error sending message with confirmations", e);
        }
    }

    @Override
    public Optional<IMqttsnMessage> waitForCompletion(IClientIdentifierContext context, final MqttsnWaitToken token) throws MqttsnExpectationFailedException {
        return waitForCompletion(context, token, registry.getOptions().getMaxWait());
    }

    @Override
    public Optional<IMqttsnMessage> waitForCompletion(IClientIdentifierContext context, final MqttsnWaitToken token, int waitTime) throws MqttsnExpectationFailedException {
        try {
            if(token == null){
                logger.warn("cannot wait for a <null> token");
                return Optional.empty();
            }

            IMqttsnMessage message = token.getMessage();
            if(token.isComplete()){
                return Optional.ofNullable(message);
            }
            IMqttsnMessage response = null;

            long start = System.currentTimeMillis();
            long timeToWait = Math.min(waitTime, registry.getOptions().getMaxErrorRetryTime());
            synchronized(token){
                //-- code against spurious wake up
                while(!token.isComplete() &&
                        timeToWait > System.currentTimeMillis() - start) {
                    token.wait(timeToWait);
                }
            }

            long time = System.currentTimeMillis() - start;
            if(token.isComplete()){
                response = token.getResponseMessage();

            logger.info("mqtt-sn state [{} <- {}] wait for token {} in {}, confirmation of message -> {}",
                            registry.getOptions().getContextId(), context, token.isError() ? "error" : "ok", MqttsnUtils.getDurationString(time), response == null ? "<null>" : response);

                return Optional.ofNullable(response);
            } else {
                logger.warn("mqtt-sn state [{} <- {}] timed out waiting {}ms for response to {} in {} on thread {}",
                        registry.getOptions().getContextId(), context, waitTime,
                        message, MqttsnUtils.getDurationString(time), Thread.currentThread().getName());
                token.markError("timed out waiting for response");

                //a timeout should unblock the sender UNLESS its a PUBLISH in which case this is the jod of the
                //reaper (should it be? - surely the sender should monitor..)
                try {
                    clearInflight(context);
                } catch(Exception e){
                    logger.error("error cleaning inflight on timeout");
                }
                throw new MqttsnExpectationFailedException("unable to obtain response within timeout ("+waitTime+")");
            }

        } catch(InterruptedException e){
            logger.warn("a thread waiting for a message being sent was interrupted;", e);
            Thread.currentThread().interrupt();
            throw new MqttsnRuntimeException(e);
        }
    }

    @Override
    public IMqttsnMessage notifyMessageReceived(IClientIdentifierContext context, IMqttsnMessage message) throws MqttsnException {

        if(registry.getCodec().isActiveMessage(message) && !message.isErrorMessage()){
            lastActiveMessage.put(context, System.currentTimeMillis());
        }
        lastMessageReceived.put(context, System.currentTimeMillis());

        //if I receive a message that is an ACK it is in response to something sent in the local space
        IMqttsnOriginatingMessageSource source = MqttsnMessageRules.isAck(message, false) ?
                IMqttsnOriginatingMessageSource.LOCAL : IMqttsnOriginatingMessageSource.REMOTE;

        Integer msgId = message.needsId() ? message.getId() : WEAK_ATTACH_ID;
        boolean matchedMessage = inflightExists(context, source, msgId);
        boolean terminalMessage = MqttsnMessageRules.isTerminalMessage(getRegistry().getCodec(), message);

        logger.info("matching message by id {}->{} in {} space, terminalMessage {}, messageIn {}",
                    msgId, matchedMessage, source, terminalMessage, message);

        if (matchedMessage) {
            if (terminalMessage) {
                InflightMessage inflight = removeInflight(context, source, msgId);
                if(inflight == null){
                    logger.warn("inflight message was cleared during notifyReceive for {} -> {}", context, msgId);
                    return null;
                }
                else if (!MqttsnMessageRules.validResponse(getRegistry().getCodec(),
                        inflight.getMessage(), message)) {
                    logger.warn("invalid response message {} for {} -> {}",
                                    message, inflight.getMessage(), context);
                    if(registry.getCodec().isDisconnect(message)){
                        logger.warn("detected distant disconnect, notify application for {} -> {}", inflight.getMessage(), context);
                        MqttsnWaitToken token = inflight.getToken();
                        if (token != null) {
                            synchronized (token) {
                                //-- release any waits
                                token.setResponseMessage(message);
                                token.markError("unexpected disconnect received whilst awaiting response");
                                token.notifyAll();
                            }
                        }
                        registry.getRuntime().handleRemoteDisconnect(context);
                        return null;
                    } else {
                        throw new MqttsnRuntimeException("invalid response received " + message.getMessageName());
                    }
                } else {

                    IMqttsnMessage confirmedMessage = inflight.getMessage();
                    MqttsnWaitToken token = inflight.getToken();

                    if (token != null) {
                        synchronized (token) {
                            //-- release any waits
                            token.setResponseMessage(message);
                            if (message.isErrorMessage()) token.markError("protocol error message received - " + message.getReturnCode());
                            else token.markComplete();
                            token.notifyAll();
                        }
                    }

                    if (message.isErrorMessage()) {

                        logger.warn("error response received {} in response to {} for {}",
                                        message, confirmedMessage, context);

                        //received an error message in response, if its requeuable do so

                        if (inflight instanceof RequeueableInflightMessage) {
                            try {
                                IQueuedPublishMessage m = ((RequeueableInflightMessage) inflight).getQueuedPublishMessage();
                                if(m.getRetryCount() >= registry.getOptions().getMaxErrorRetries()){
                                    logger.warn("publish message {} exceeded max retries {}, discard and notify application", registry.getOptions().getMaxErrorRetries(), m);
                                    PublishData data = registry.getCodec().getData(confirmedMessage);
                                    registry.getRuntime().messageSendFailure(context,
                                            new TopicPath(m.getData().getTopicPath()), data.getQos(), data.isRetained(),
                                            data.getData(), confirmedMessage, m.getRetryCount());
                                } else {
                                    logger.info("message was re-queueable offer to queue {}", context);
                                    ISession session = registry.getSessionRegistry().getSession(context, false);
                                    if(session != null){
                                        registry.getMessageQueue().offer(session, m);
                                    }
                                }
                            } catch(MqttsnQueueAcceptException e){
                                throw new MqttsnException(e);
                            }
                        }

                    } else {

                        //inbound qos 2 commit
                        if (registry.getCodec().isPubRel(message)) {
                            PublishData data = registry.getCodec().getData(confirmedMessage);
                            CommitOperation op = CommitOperation.inbound(context, data, confirmedMessage);
                            op.data.setTopicPath(getTopicPathFromPublish(context, confirmedMessage));
                            confirmPublish(op);
                        }

                        //outbound qos 1
                        if (registry.getCodec().isPuback(message)) {
                            RequeueableInflightMessage rim = (RequeueableInflightMessage) inflight;
                            PublishData data = registry.getCodec().getData(confirmedMessage);
                            CommitOperation op = CommitOperation.outbound(context, rim.getQueuedPublishMessage().getDataRefId(), data, confirmedMessage);
                            op.data.setTopicPath(rim.getQueuedPublishMessage().getData().getTopicPath());
                            confirmPublish(op);
                        }
                    }
                    return confirmedMessage;
                }
            } else {

                InflightMessage inflight = getInflightMessage(context, source, msgId);

                //none terminal matched message.. this is fine (PUBREC or PUBREL)
                //outbound qos 2 commit point
                if(inflight != null && registry.getCodec().isPubRec(message)){
                    PublishData data = registry.getCodec().getData(inflight.getMessage());
                    CommitOperation op = CommitOperation.outbound(context,
                            ((RequeueableInflightMessage) inflight).getQueuedPublishMessage().getDataRefId(),
                            data, inflight.getMessage());
                    op.data.setTopicPath(((RequeueableInflightMessage) inflight).
                            getQueuedPublishMessage().getData().getTopicPath());
                    confirmPublish(op);
                }

                return null;
            }

        } else {

            //-- received NEW message that was not associated with an inflight message
            //-- so we need to pin it into the inflight system (if it needs confirming).
            if (registry.getCodec().isPublish(message)) {
                PublishData data = registry.getCodec().getData(message);
                if (data.getQos() == MqttsnConstants.QoS2) {
                    //-- Qos 2 needs further confirmation before being sent to application
                    markInflight(source, context, message, null);
                }
                else {
                    //-- Qos 0 & 1 are inbound are confirmed on receipt of message
                    CommitOperation op = CommitOperation.inbound(context, data, message);
                    op.data.setTopicPath(getTopicPathFromPublish(context, message));
                    confirmPublish(op);
                }
            }
            return null;
        }
    }

    /**
     * Confirmation delivery to the application takes place on the worker thread group
     */
    protected void confirmPublish(final CommitOperation operation) {

        getRegistry().getRuntime().generalPurposeSubmit(() -> {
            IClientIdentifierContext context = operation.context;
            byte[] payload = operation.data.getData();
            if(registry.getSecurityService().payloadIntegrityEnabled()){
                try {
                    INetworkContext networkContext = registry.getNetworkRegistry().getContext(operation.context);
                    payload = registry.getSecurityService().readVerified(networkContext, payload);
                } catch(MqttsnSecurityException e){
                    logger.warn("dropping received publish message which did not pass integrity checks", e);
                    return;
                }
            }

            if(operation.inbound){
                registry.getRuntime().messageReceived(context,
                        new TopicPath(operation.data.getTopicPath()),
                        operation.data.getQos(),
                        operation.data.isRetained(),
                        payload,
                        operation.message);
            } else {
                registry.getRuntime().messageSent(context,
                        new TopicPath(operation.data.getTopicPath()),
                        operation.data.getQos(),
                        operation.data.isRetained(),
                        payload,
                        operation.message);
            }
        });
    }

    protected MqttsnWaitToken markInflight(IMqttsnOriginatingMessageSource source, IClientIdentifierContext context, IMqttsnMessage message, IQueuedPublishMessage queuedPublishMessage)
            throws MqttsnException {

        //may have old inbound messages kicking around depending on reap settings, to just allow these to come in
        int count = 0;
        if((count = countInflight(context, source)) >=
                registry.getOptions().getMaxMessagesInflight()){

            if(source == IMqttsnOriginatingMessageSource.LOCAL){
                logger.warn("{} max inflight message number reached ({}), fail-fast {} {}", context, count, source, message);
                throw new MqttsnExpectationFailedException("max number of inflight messages reached for " + source);
            }
        }

        InflightMessage inflight = queuedPublishMessage == null ? new InflightMessage(message, source, MqttsnWaitToken.from(message)) :
                new RequeueableInflightMessage(queuedPublishMessage, message);

        LastIdContext idContext = LastIdContext.from(context, source);
        int msgId = WEAK_ATTACH_ID;
        if (message.needsId()) {
            synchronized (context){
                if (message.getId() > 0) {
                    msgId = message.getId();
                } else {
                    msgId = getNextMsgId(idContext);
                    message.setId(msgId);
                }
                //-- SLJ Edit - move this to tighen the synchronisation of assigned packetIdentifier
                //NB: this code used to be below the addInflightMethod below (in case this has a knock on)
                if(msgId != WEAK_ATTACH_ID) lastUsedMsgIds.put(idContext, msgId);
            }

            //-- ensure we update the queued version so if delivery fails we know what to redeliver with
            if(queuedPublishMessage != null) queuedPublishMessage.setPacketId(msgId);
        }

        addInflightMessage(context, msgId, inflight);
        logger.debug("[{} - {}] marking {} message {} inflight id context {}",
                    registry.getOptions().getContextId(), context, source, message, idContext);
        return inflight.getToken();
    }

    /**
     * This requires external synchronisation since it uses session based data structures
     */
    protected Integer getNextMsgId(LastIdContext context) throws MqttsnException {

        Map<Integer, InflightMessage> map = getInflightMessages(context.context, context.source);
        int startAt = Math.max(lastUsedMsgIds.get(context) == null ? 1 : lastUsedMsgIds.get(context) + 1,
                registry.getOptions().getMsgIdStartAt());

        startAt = startAt % MqttsnConstants.UNSIGNED_MAX_16;
        startAt = Math.max(Math.max(1, registry.getOptions().getMsgIdStartAt()), startAt);

        Set<Integer> set = map.keySet();
        while(set.contains(Integer.valueOf(startAt))){
            startAt = ++startAt % MqttsnConstants.UNSIGNED_MAX_16;
            startAt = Math.max(registry.getOptions().getMsgIdStartAt(), startAt);
        }

        if(set.contains(Integer.valueOf(startAt)))
            throw new MqttsnRuntimeException("cannot assign msg id " + startAt);

        logger.debug("next id available for context {} is {}", context, startAt);

        return startAt;
    }

    public void clearInflight(IClientIdentifierContext context) throws MqttsnException {
        clearInflightInternal(context, 0);
    }

    @Override
    public void clear(IClientIdentifierContext context) throws MqttsnException {
        logger.info("clearing down message state for context {}", context);
        unscheduleFlush(context);
        lastActiveMessage.remove(context);
        lastMessageReceived.remove(context);
        lastMessageSent.remove(context);
        lastUsedMsgIds.remove(LastIdContext.from(context, IMqttsnOriginatingMessageSource.REMOTE));
        lastUsedMsgIds.remove(LastIdContext.from(context, IMqttsnOriginatingMessageSource.LOCAL));
    }

    protected void clearInflightInternal(IClientIdentifierContext context, long evictionTime) throws MqttsnException {
        logger.debug("clearing all inflight messages for context {}, forced = {}", context, evictionTime == 0);
        if(registry.getOptions().isReapReceivingMessages()){
            clearInternal(context, getInflightMessages(context, IMqttsnOriginatingMessageSource.REMOTE), evictionTime);
        }
        clearInternal(context, getInflightMessages(context, IMqttsnOriginatingMessageSource.LOCAL), evictionTime);
    }


    //-- deadlock fixed by PCC see
    //-- https://github.com/simon622/mqtt-sn/issues/69
    private void clearInternal(IClientIdentifierContext context, Map<Integer, InflightMessage> messages, long evictionTime) throws MqttsnException {
        if(messages != null && !messages.isEmpty()){
            List<InflightMessage> evicted = new ArrayList<InflightMessage>();
            synchronized (messages){
                Iterator<Integer> messageItr = messages.keySet().iterator();
                while(messageItr.hasNext()){
                    Integer i = messageItr.next();
                    InflightMessage f = messages.get(i);
                    if(f != null){
                        if(evictionTime == 0 ||
                                f.getTime() + registry.getOptions().getMaxTimeInflight() < evictionTime){
                            messageItr.remove();
                            evicted.add(f);
                        }
                    }
                }
            }
            for (InflightMessage inflightMessage : evicted) {
                reapInflight(context, inflightMessage);
            }
        }
    }



    protected void reapInflight(IClientIdentifierContext context, InflightMessage inflight) throws MqttsnException {

        IMqttsnMessage message = inflight.getMessage();
        logger.info("clearing message {} destined for {} aged {} from inflight",
                message, context, MqttsnUtils.getDurationString(System.currentTimeMillis() - inflight.getTime()));

        MqttsnWaitToken token = inflight.getToken();
        if(token != null){
            synchronized (token){
                if(!token.isComplete()){
                    token.markError("timed out waiting for reply");
                }
                token.notifyAll();
            }
        }


        //-- requeue if its a PUBLISH and we have a message queue bound
        if(inflight instanceof RequeueableInflightMessage){
            RequeueableInflightMessage requeueableInflightMessage = (RequeueableInflightMessage) inflight;
            if(registry.getMessageQueue() != null &&
                    registry.getOptions().isRequeueOnInflightTimeout() &&
                    requeueableInflightMessage.getQueuedPublishMessage() != null) {
                IQueuedPublishMessage queuedPublishMessage = requeueableInflightMessage.getQueuedPublishMessage();
                queuedPublishMessage.setToken(null);
                boolean maxRetries = queuedPublishMessage.getRetryCount() >= registry.getOptions().getMaxErrorRetries();
                try {
                    if(maxRetries){
                        logger.info("max delivery attempts hit for context, dlq message {} for {}", context,
                                queuedPublishMessage);
                        getRegistry().getDeadLetterQueue().add(
                                MqttsnDeadLetterQueueBean.REASON.RETRY_COUNT_EXCEEDED,
                                context, queuedPublishMessage);
                    } else {
                        logger.info("re-queuing publish message {} for {}", context,
                                queuedPublishMessage);
                        ISession session = registry.getSessionRegistry().getSession(context, false);
                        if(session != null){
                            registry.getMessageQueue().offer(session, queuedPublishMessage);
                        }
                    }
                } catch(MqttsnQueueAcceptException e){
                    //queue is full cant put it there
                } finally {
                    if(maxRetries){
                        registry.getRuntime().handleConnectionLost(context, null);
                    }
                }
            }
        }
    }

    @Override
    public int countInflight(IClientIdentifierContext context, IMqttsnOriginatingMessageSource source) throws MqttsnException {
        Map<Integer, InflightMessage> map = getInflightMessages(context, source);
        return map.size();
    }

    @Override
    public boolean canSend(IClientIdentifierContext context) throws MqttsnException {
        int inflight = countInflight(context, IMqttsnOriginatingMessageSource.LOCAL);
        boolean canSend = inflight <
                registry.getOptions().getMaxMessagesInflight();
        if(!canSend){
            logger.debug("{} number of inflight messages {} reached the configured max. {}", context, inflight, registry.getOptions().getMaxMessagesInflight());
        }
        return canSend;
    }

    @Override
    public Long getMessageLastSentToContext(IClientIdentifierContext context) {
        return lastMessageSent.get(context);
    }

    @Override
    public Long getMessageLastReceivedFromContext(IClientIdentifierContext context) {
        return lastMessageReceived.get(context);
    }

    public Long getLastActiveMessage(IClientIdentifierContext context){
        return lastActiveMessage.get(context);
    }

    protected String getTopicPathFromPublish(IClientIdentifierContext context, IMqttsnMessage message) throws MqttsnException {

        int topicIdType;
        byte[] topicData;
        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0) {
            MqttsnPublish_V2_0 publish = (MqttsnPublish_V2_0) message;
            topicIdType = publish.getTopicIdType();
            topicData = publish.getTopicData();
        }
        else {
            MqttsnPublish publish = (MqttsnPublish) message;
            topicIdType = publish.getTopicType();
            topicData = publish.getTopicData();
        }

        TopicInfo info = registry.getTopicRegistry().normalize((byte) topicIdType, topicData, false);

        ISession session = getRegistry().getSessionRegistry().getSession(context, false);
        String topicPath = registry.getTopicRegistry().topicPath(session, info, true);
        return topicPath;
    }

    public abstract InflightMessage removeInflight(IClientIdentifierContext context, IMqttsnOriginatingMessageSource source, Integer packetId) throws MqttsnException;

    protected abstract void addInflightMessage(IClientIdentifierContext context, Integer packetId, InflightMessage message) throws MqttsnException ;

    protected abstract InflightMessage getInflightMessage(IClientIdentifierContext context, IMqttsnOriginatingMessageSource source, Integer packetId) throws MqttsnException ;

    protected abstract Map<Integer, InflightMessage>  getInflightMessages(IClientIdentifierContext context, IMqttsnOriginatingMessageSource source) throws MqttsnException;

    protected abstract boolean inflightExists(IClientIdentifierContext context, IMqttsnOriginatingMessageSource source, Integer packetId) throws MqttsnException;

    static class CommitOperation {

        protected PublishData data;
        protected IMqttsnMessage message;
        protected IClientIdentifierContext context;
        protected long timestamp;
        protected IDataRef messageId;
        //-- TODO this should encapsulate the source enum for consistency
        protected boolean inbound;

        public CommitOperation(IClientIdentifierContext context, PublishData data, IMqttsnMessage message, boolean inbound){
            this.context = context;
            this.data = data;
            this.message = message;
            this.inbound = inbound;
            this.timestamp = System.currentTimeMillis();
        }

        public static CommitOperation inbound(IClientIdentifierContext context, PublishData data, IMqttsnMessage message){
            return new CommitOperation(context, data, message, true);
        }

        public static CommitOperation outbound(IClientIdentifierContext context, IDataRef messageId, PublishData data, IMqttsnMessage message){
            CommitOperation c = new CommitOperation(context, data, message, false);
            c.messageId = messageId;
            return c;
        }

        @Override
        public String toString() {
            return "CommitOperation{" +
                    "data=" + data +
                    ", message=" + message +
                    ", context=" + context +
                    ", timestamp=" + timestamp +
                    ", messageId=" + messageId +
                    ", inbound=" + inbound +
                    '}';
        }
    }

    protected static class LastIdContext {

        protected final IClientIdentifierContext context;
        protected final IMqttsnOriginatingMessageSource source;

        public LastIdContext(IClientIdentifierContext context, IMqttsnOriginatingMessageSource source) {
            this.context = context;
            this.source = source;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LastIdContext that = (LastIdContext) o;
            return context.equals(that.context) && source == that.source;
        }

        @Override
        public int hashCode() {
            return Objects.hash(context, source);
        }

        @Override
        public String toString() {
            return "LastIdContext{" +
                    "context=" + context +
                    ", source=" + source +
                    '}';
        }

        public static LastIdContext from(IClientIdentifierContext context, IMqttsnOriginatingMessageSource source){
            return new LastIdContext(context, source);
        }
    }
}
