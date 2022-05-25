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
import org.slj.mqtt.sn.PublishData;
import org.slj.mqtt.sn.model.*;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.TopicPath;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.version1_2.payload.*;
import org.slj.mqtt.sn.wire.version2_0.payload.MqttsnPublish_V2_0;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public abstract class AbstractMqttsnMessageStateService <T extends IMqttsnRuntimeRegistry>
        extends AbstractMqttsnBackoffThreadService<T> implements IMqttsnMessageStateService<T> {

    protected static final Integer WEAK_ATTACH_ID = new Integer(MqttsnConstants.UNSIGNED_MAX_16 + 1);
    protected boolean clientMode;

    protected Map<IMqttsnContext, Long> lastActiveMessage;
    protected Map<IMqttsnContext, Long> lastMessageSent;
    protected Map<IMqttsnContext, Long> lastMessageReceived;

    protected Map<LastIdContext, Integer> lastUsedMsgIds;
    protected Map<IMqttsnContext, ScheduledFuture<IMqttsnMessageQueueProcessor.RESULT>> flushOperations;
    protected ScheduledExecutorService executorService = null;
    private static final int TIMEOUT_BACKOFF = 15000;

    public AbstractMqttsnMessageStateService(boolean clientMode) {
        this.clientMode = clientMode;
    }

    @Override
    public synchronized void start(T runtime) throws MqttsnException {
        flushOperations = new HashMap();
        executorService = runtime.getRuntime().createManagedScheduledExecutorService("mqtt-sn-scheduled-queue-flush-",
                runtime.getOptions().getQueueProcessorThreadCount());
        lastUsedMsgIds = Collections.synchronizedMap(new HashMap());
        lastMessageReceived = Collections.synchronizedMap(new HashMap());
        lastMessageSent = Collections.synchronizedMap(new HashMap());
        lastActiveMessage = Collections.synchronizedMap(new HashMap());
        super.start(runtime);
    }

    protected void scheduleWork(IMqttsnContext context, int time, TimeUnit unit){
        ScheduledFuture<IMqttsnMessageQueueProcessor.RESULT> future =
                executorService.schedule(() -> {
                    IMqttsnMessageQueueProcessor.RESULT result =
                            IMqttsnMessageQueueProcessor.RESULT.REMOVE_PROCESS;
                    boolean process = !flushOperations.containsKey(context) ||
                            !flushOperations.get(context).isDone();
                    if(logger.isLoggable(Level.FINE)){
                        logger.log(Level.FINE, String.format("processing scheduled work for context [%s] -> [%s]", context, process));
                    }
                    if(process){
                        result = processQueue(context);
                        switch(result){
                            case REMOVE_PROCESS:
                                synchronized (flushOperations){
                                    flushOperations.remove(context);
                                }
                                if(logger.isLoggable(Level.FINE)){
                                    logger.log(Level.FINE, String.format("removed context from work list [%s]", context));
                                }
                                break;
                            case BACKOFF_PROCESS:
                                Long lastReceived = lastMessageReceived.get(context);
                                long delta = lastReceived == null ? 0 : System.currentTimeMillis() - lastReceived;
                                boolean remove = registry.getOptions().getActiveContextTimeout() < delta;
                                if(logger.isLoggable(Level.FINE)){
                                    logger.log(Level.FINE, String.format("backoff requested for [%s], activity delta is [%s], remove work ? [%s]", context, delta, remove));
                                }
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
                                scheduleWork(context, registry.getOptions().getMinFlushTime(), TimeUnit.MILLISECONDS);
                        }
                    }
                    if(logger.isLoggable(Level.FINE)){
                        logger.log(Level.FINE, String.format("context [%s] flush completed with [%s]", context, result));
                    }
                    return result;
        }, time, unit);
        synchronized (flushOperations){
            flushOperations.put(context, future);
        }
    }

    protected IMqttsnMessageQueueProcessor.RESULT processQueue(IMqttsnContext context){
        try {
            return registry.getQueueProcessor().process(context);
        } catch(Exception e){
            logger.log(Level.SEVERE,
                    String.format("error encountered processing queue for [%s]", context), e);
            return IMqttsnMessageQueueProcessor.RESULT.REMOVE_PROCESS;
        }
    }

    @Override
    public void unscheduleFlush(IMqttsnContext context) {
        ScheduledFuture<?> future = null;
        if(!flushOperations.containsKey(context)) {
            synchronized (flushOperations) {
                future = flushOperations.remove(context);
            }
        }
        if(future != null){
            if(!future.isDone()){
                future.cancel(false);
            }
        }
    }

    @Override
    public void scheduleFlush(IMqttsnContext context)  {
        if(!flushOperations.containsKey(context) ||
                flushOperations.get(context).isDone()){
            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE, String.format("scheduling flush for [%s]", context));
            }
            if(executorService != null &&
                    !executorService.isTerminated() && !executorService.isShutdown()){
                scheduleWork(context,
                        ThreadLocalRandom.current().nextInt(1, 250), TimeUnit.MILLISECONDS);
            }

        }
    }

    @Override
    protected long doWork() {

        //-- monitor active context timeouts
        int activeMessageTimeout = registry.getOptions().getActiveContextTimeout();
        if(activeMessageTimeout > 0){
            Set<IMqttsnContext> copy = null;
            synchronized (lastActiveMessage){
                copy = new HashSet<>(lastActiveMessage.keySet());
            }
            Iterator<IMqttsnContext> itr = copy.iterator();
            while(itr.hasNext()){
                IMqttsnContext context = itr.next();
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
            logger.log(Level.SEVERE, "error tidying message registry on state thread;", e);
        }
        return TIMEOUT_BACKOFF;
    }

    protected boolean allowedToSend(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {
        return true;
    }

    @Override
    public MqttsnWaitToken sendMessage(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {
        return sendMessageInternal(context, message, null);
    }

    @Override
    public MqttsnWaitToken sendPublishMessage(IMqttsnContext context, TopicInfo info, QueuedPublishMessage queuedPublishMessage) throws MqttsnException {

        byte[] payload = registry.getMessageRegistry().get(queuedPublishMessage.getMessageId());

        if(registry.getSecurityService().payloadIntegrityEnabled()){
            payload = registry.getSecurityService().writeVerified(payload);
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

        IMqttsnMessage publish = registry.getMessageFactory().createPublish(queuedPublishMessage.getData().getQos(),
                isDUPDelivery(queuedPublishMessage),
                queuedPublishMessage.getData().isRetained(), type, topicId,
                payload);
        if(queuedPublishMessage.getMsgId() != 0){
            //-- ensure if we have sent a message before we use the same ID again
            publish.setId(queuedPublishMessage.getMsgId());
        }
        return sendMessageInternal(context, publish, queuedPublishMessage);
    }

    protected boolean isDUPDelivery(QueuedPublishMessage message){
        return message.getRetryCount() > 1 || message.getMsgId() > 0;
    }

    protected MqttsnWaitToken sendMessageInternal(IMqttsnContext context, IMqttsnMessage message, QueuedPublishMessage queuedPublishMessage) throws MqttsnException {

        if(!allowedToSend(context, message)){
            logger.log(Level.WARNING,
                    String.format("allowed to send [%s] check failed [%s]",
                            message, context));
            throw new MqttsnExpectationFailedException("allowed to send check failed");
        }

        InflightMessage.DIRECTION direction = registry.getMessageHandler().isPartOfOriginatingMessage(message) ?
                InflightMessage.DIRECTION.SENDING : InflightMessage.DIRECTION.RECEIVING;

        int count = countInflight(context, direction);
        if(count > 0){
            logger.log(Level.WARNING,
                    String.format("presently unable to send [%s],[%s] to [%s], max inflight reached for direction [%s] [%s] -> [%s]",
                            message, queuedPublishMessage, context, direction, count,
                            Objects.toString(getInflightMessages(context))));

            Optional<InflightMessage> blockingMessage =
                    getInflightMessages(context).values().stream().filter(i -> i.getDirection() == direction).findFirst();
            if(blockingMessage.isPresent() && clientMode){
                //-- if we are in client mode, attempt to wait for the ongoing outbound message to complete before we issue next message
                MqttsnWaitToken token = blockingMessage.get().getToken();
                if(token != null){
                    waitForCompletion(context, token);
                    if(!token.isError() && token.isComplete()){
                        //-- recurse point
                        return sendMessageInternal(context, message, queuedPublishMessage);
                    }
                    else {
                        logger.log(Level.WARNING, String.format("unable to send, partial send in progress with token [%s]", token));
                        throw new MqttsnExpectationFailedException("unable to send message, partial send in progress");
                    }
                }
            } else {
                throw new MqttsnExpectationFailedException("max number of inflight messages reached");
            }
        }

        try {

            MqttsnWaitToken token = null;
            boolean requiresResponse;
            if((requiresResponse = registry.getMessageHandler().requiresResponse(message))){
                token = markInflight(context, message, queuedPublishMessage);
            }

            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE,
                        String.format("mqtt-sn state [%s -> %s] sending message [%s], marking inflight ? [%s]",
                                registry.getOptions().getContextId(), context, message, requiresResponse));
            }

            Runnable callback = null;
            if(!requiresResponse && registry.getCodec().isPublish(message)){
                PublishData data = registry.getCodec().getData(message);
                CommitOperation op = CommitOperation.outbound(context,
                        queuedPublishMessage.getMessageId(), data, message);
                op.data.setTopicPath(queuedPublishMessage.getData().getTopicPath());

                //-- wait until the transport confirms the send else the confirm could happen before the backpressure is relieved
                callback = () -> confirmPublish(op);
            }

            if(callback == null){
                registry.getTransport().writeToTransport(registry.getNetworkRegistry().getContext(context), message);
            } else {
                registry.getTransport().writeToTransportWithWork(registry.getNetworkRegistry().getContext(context), message, callback);
            }
            long time = System.currentTimeMillis();
            if(registry.getCodec().isActiveMessage(message) &&
                    !message.isErrorMessage()){
                lastActiveMessage.put(context, time);
            }
            lastMessageSent.put(context, time);
            return token;

        } catch(Exception e){
            throw new MqttsnException("error sending message with confirmations", e);
        }
    }

    @Override
    public Optional<IMqttsnMessage> waitForCompletion(IMqttsnContext context, final MqttsnWaitToken token) throws MqttsnExpectationFailedException {
        return waitForCompletion(context, token, registry.getOptions().getMaxWait());
    }

    @Override
    public Optional<IMqttsnMessage> waitForCompletion(IMqttsnContext context, final MqttsnWaitToken token, int waitTime) throws MqttsnExpectationFailedException {
        try {
            if(token == null){
                logger.log(Level.WARNING, "cannot wait for a <null> token");
                return Optional.empty();
            }

            IMqttsnMessage message = token.getMessage();
            if(token.isComplete()){
                return Optional.ofNullable(message);
            }
            IMqttsnMessage response = null;

            long start = System.currentTimeMillis();
            long timeToWait = Math.max(waitTime, registry.getOptions().getMaxErrorRetryTime());
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
                if(logger.isLoggable(Level.INFO)){
                    logger.log(Level.INFO, String.format("mqtt-sn state [%s <- %s] wait for token [%s] in [%s], confirmation of message -> [%s]",
                            registry.getOptions().getContextId(), context, token.isError() ? "error" : "ok", MqttsnUtils.getDurationString(time), response == null ? "<null>" : response));
                }
                return Optional.ofNullable(response);
            } else {
                logger.log(Level.WARNING, String.format("mqtt-sn state [%s <- %s] timed out waiting [%s]ms for response to [%s] in [%s] on thread [%s]",
                        registry.getOptions().getContextId(), context, waitTime,
                        message, MqttsnUtils.getDurationString(time), Thread.currentThread().getName()));
                token.markError();

                //a timeout should unblock the sender UNLESS its a PUBLISH in which case this is the jod of the
                //reaper (should it be? - surely the sender should monitor..)
                try {
                    clearInflight(context);
                } catch(Exception e){
                    logger.log(Level.SEVERE, "error cleaning inflight on timeout");
                }
                throw new MqttsnExpectationFailedException("unable to obtain response within timeout ("+waitTime+")");
            }

        } catch(InterruptedException e){
            logger.log(Level.WARNING, "a thread waiting for a message being sent was interrupted;", e);
            Thread.currentThread().interrupt();
            throw new MqttsnRuntimeException(e);
        }
    }

    @Override
    public IMqttsnMessage notifyMessageReceived(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {

        if(registry.getCodec().isActiveMessage(message) && !message.isErrorMessage()){
            lastActiveMessage.put(context, System.currentTimeMillis());
        }
        lastMessageReceived.put(context, System.currentTimeMillis());

        Integer msgId = message.needsId() ? message.getId() : WEAK_ATTACH_ID;
        boolean matchedMessage = inflightExists(context, msgId);
        boolean terminalMessage = registry.getMessageHandler().isTerminalMessage(message);

        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, String.format("matched message by id [%s]->[%s], terminalMessage [%s], messageIn [%s]",
                    msgId, matchedMessage, terminalMessage, message));
        }

        if (matchedMessage) {
            if (terminalMessage) {
                InflightMessage inflight = removeInflight(context, msgId);
                if(inflight == null){
                    logger.log(Level.WARNING,
                            String.format("inflight message was cleared during notifyReceive for [%s] -> [%s]", context, msgId));
                    return null;
                }
                else if (!registry.getMessageHandler().validResponse(inflight.getMessage(), message)) {
                    logger.log(Level.WARNING,
                            String.format("invalid response message [%s] for [%s] -> [%s]",
                                    message, inflight.getMessage(), context));

                    if(registry.getCodec().isDisconnect(message)){

                        logger.log(Level.WARNING,
                                String.format("detected distant disconnect, notify application for [%s] -> [%s]",
                                        inflight.getMessage(), context));
                        MqttsnWaitToken token = inflight.getToken();
                        if (token != null) {
                            synchronized (token) {
                                //-- release any waits
                                token.setResponseMessage(message);
                                token.markError();
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
                            if (message.isErrorMessage()) token.markError();
                            else token.markComplete();
                            token.notifyAll();
                        }
                    }

                    if (message.isErrorMessage()) {

                        logger.log(Level.WARNING,
                                String.format("error response received [%s] in response to [%s] for [%s]",
                                        message, confirmedMessage, context));

                        //received an error message in response, if its requeuable do so

                        if (inflight instanceof RequeueableInflightMessage) {
                            try {
                                QueuedPublishMessage m = ((RequeueableInflightMessage) inflight).getQueuedPublishMessage();
                                if(m.getRetryCount() >= registry.getOptions().getMaxErrorRetries()){
                                    logger.log(Level.WARNING, String.format("publish message [%s] exceeded max retries [%s], discard and notify application", registry.getOptions().getMaxErrorRetries(), m));
                                    PublishData data = registry.getCodec().getData(confirmedMessage);
                                    registry.getRuntime().messageSendFailure(context, m.getMessageId(),
                                            new TopicPath(m.getData().getTopicPath()), data.getQos(), data.isRetained(),
                                            data.getData(), confirmedMessage, m.getRetryCount());
                                } else {
                                    logger.log(Level.INFO,
                                            String.format("message was re-queueable offer to queue [%s]", context));
                                    registry.getMessageQueue().offer(context, m);
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
                            CommitOperation op = CommitOperation.outbound(context, rim.getQueuedPublishMessage().getMessageId(), data, confirmedMessage);
                            op.data.setTopicPath(rim.getQueuedPublishMessage().getData().getTopicPath());
                            confirmPublish(op);
                        }
                    }
                    return confirmedMessage;
                }
            } else {

                InflightMessage inflight = getInflightMessage(context, msgId);

                //none terminal matched message.. this is fine (PUBREC or PUBREL)
                //outbound qos 2 commit point
                if(inflight != null && registry.getCodec().isPubRec(message)){
                    PublishData data = registry.getCodec().getData(inflight.getMessage());
                    CommitOperation op = CommitOperation.outbound(context,
                            ((RequeueableInflightMessage) inflight).getQueuedPublishMessage().getMessageId(),
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
                if (data.getQos() == 2) {
//                    int count = countInflight(context, InflightMessage.DIRECTION.RECEIVING);
//                    if(count >= registry.getOptions().getMaxMessagesInflight()){
//                        logger.log(Level.WARNING, String.format("have [%s] existing inbound message(s) inflight & new publish QoS2, replacing inflights!", count));
//                       throw new MqttsnException("cannot receive more than maxInflight!");
//                    }
                    //-- Qos 2 needs further confirmation before being sent to application
                    markInflight(context, message, null);
                } else {
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

        getRegistry().getRuntime().async(() -> {
            IMqttsnContext context = operation.context;
            byte[] payload = operation.data.getData();
            if(registry.getSecurityService().payloadIntegrityEnabled()){
                try {
                    payload = registry.getSecurityService().readVerified(payload);
                } catch(MqttsnSecurityException e){
                    logger.log(Level.WARNING, "dropping received publish message which did not pass integrity checks", e);
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
                        operation.messageId,
                        new TopicPath(operation.data.getTopicPath()),
                        operation.data.getQos(),
                        operation.data.isRetained(),
                        payload,
                        operation.message);
            }
        });
    }

    protected MqttsnWaitToken markInflight(IMqttsnContext context, IMqttsnMessage message, QueuedPublishMessage queuedPublishMessage)
            throws MqttsnException {

        InflightMessage.DIRECTION direction =
                message instanceof MqttsnPublish ?
                        queuedPublishMessage == null ?
                                InflightMessage.DIRECTION.RECEIVING : InflightMessage.DIRECTION.SENDING :
                                    registry.getMessageHandler().isPartOfOriginatingMessage(message) ?
                                            InflightMessage.DIRECTION.SENDING : InflightMessage.DIRECTION.RECEIVING;

        //may have old inbound messages kicking around depending on reap settings, to just allow these to come in
        if(countInflight(context, direction) >=
                registry.getOptions().getMaxMessagesInflight()){
            logger.log(Level.WARNING, String.format("[%s] max inflight message number reached, fail-fast for sending, allow for receiving [%s] - [%s]", context, direction, message));
            if(direction == InflightMessage.DIRECTION.SENDING){
                throw new MqttsnExpectationFailedException("max number of inflight messages reached");
            }
        }

        InflightMessage inflight = queuedPublishMessage == null ? new InflightMessage(message, direction, MqttsnWaitToken.from(message)) :
                new RequeueableInflightMessage(queuedPublishMessage, message);

        LastIdContext idContext = LastIdContext.from(context, direction);
        int msgId = WEAK_ATTACH_ID;
        if (message.needsId()) {
            if (message.getId() > 0) {
                msgId = message.getId();
            } else {
                msgId = getNextMsgId(idContext);
                message.setId(msgId);
            }

            //-- ensure we update the queued version so if delivery fails we know what to redeliver with
            if(queuedPublishMessage != null) queuedPublishMessage.setMsgId(msgId);
        }

        addInflightMessage(context, msgId, inflight);

        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, String.format("[%s - %s] marking [%s] message [%s] inflight id context [%s]",
                    registry.getOptions().getContextId(), context, direction, message, idContext));
        }

        if(msgId != WEAK_ATTACH_ID) lastUsedMsgIds.put(idContext, msgId);
        return inflight.getToken();
    }

    protected Integer getNextMsgId(LastIdContext context) throws MqttsnException {

        Map<Integer, InflightMessage> map = getInflightMessages(context.context);
        int startAt = Math.max(lastUsedMsgIds.get(context) == null ? 1 : lastUsedMsgIds.get(context) + 1,
                registry.getOptions().getMsgIdStartAt());

        startAt = startAt % MqttsnConstants.UNSIGNED_MAX_16;
        startAt = Math.max(Math.max(1, registry.getOptions().getMsgIdStartAt()), startAt);

        Set<Integer> set = map.keySet();
        while(set.contains(new Integer(startAt))){
            startAt = ++startAt % MqttsnConstants.UNSIGNED_MAX_16;
            startAt = Math.max(registry.getOptions().getMsgIdStartAt(), startAt);
        }

        if(set.contains(new Integer(startAt)))
            throw new MqttsnRuntimeException("cannot assign msg id " + startAt);

        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, String.format("next id available for context [%s] is [%s]", context, startAt));
        }

        return startAt;
    }

    public void clearInflight(IMqttsnContext context) throws MqttsnException {
        clearInflightInternal(context, 0);
    }

    @Override
    public void clear(IMqttsnContext context) throws MqttsnException {
        logger.log(Level.INFO, String.format("clearing down message state for context [%s]", context));
        unscheduleFlush(context);
        lastActiveMessage.remove(context);
        lastMessageReceived.remove(context);
        lastMessageSent.remove(context);
        lastUsedMsgIds.remove(LastIdContext.from(context, InflightMessage.DIRECTION.SENDING));
        lastUsedMsgIds.remove(LastIdContext.from(context, InflightMessage.DIRECTION.RECEIVING));
    }

    protected void clearInflightInternal(IMqttsnContext context, long evictionTime) throws MqttsnException {
        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, String.format("clearing all inflight messages for context [%s], forced = [%s]", context, evictionTime == 0));
        }
        Map<Integer, InflightMessage> messages = getInflightMessages(context);
        if(messages != null && !messages.isEmpty()){
            synchronized (messages){
                Iterator<Integer> messageItr = messages.keySet().iterator();
                while(messageItr.hasNext()){
                    Integer i = messageItr.next();
                    InflightMessage f = messages.get(i);
                    if(f != null){
                        if(evictionTime == 0 ||
                                f.getTime() + registry.getOptions().getMaxTimeInflight() < evictionTime){
                            if(!registry.getOptions().isReapReceivingMessages() &&
                                    f.getDirection() == InflightMessage.DIRECTION.RECEIVING){
                                continue;
                            }
                            messageItr.remove();
                            reapInflight(context, f);
                        }
                    }
                }
            }
        }
    }

    protected void reapInflight(IMqttsnContext context, InflightMessage inflight) throws MqttsnException {

        IMqttsnMessage message = inflight.getMessage();
        logger.log(Level.WARNING, String.format("clearing message [%s] destined for [%s] aged [%s] from inflight",
                message, context, MqttsnUtils.getDurationString(System.currentTimeMillis() - inflight.getTime())));

        MqttsnWaitToken token = inflight.getToken();
        synchronized (token){
            token.markError();
            token.notifyAll();
        }

        //-- requeue if its a PUBLISH and we have a message queue bound
        if(inflight instanceof RequeueableInflightMessage){
            RequeueableInflightMessage requeueableInflightMessage = (RequeueableInflightMessage) inflight;
            if(registry.getMessageQueue() != null &&
                    registry.getOptions().isRequeueOnInflightTimeout() &&
                    requeueableInflightMessage.getQueuedPublishMessage() != null) {
                logger.log(Level.INFO, String.format("re-queuing publish message [%s] for client [%s]", context,
                        ((RequeueableInflightMessage) inflight).getQueuedPublishMessage()));
                QueuedPublishMessage queuedPublishMessage = requeueableInflightMessage.getQueuedPublishMessage();
                queuedPublishMessage.setToken(null);
                boolean maxRetries = queuedPublishMessage.getRetryCount() >= registry.getOptions().getMaxErrorRetries();
                try {
                    if(maxRetries){
                        //-- we're disconnecting the runtime, so reset counter for next active session and put payload back in
                        //-- registry as we'll need it again on next connection
                        queuedPublishMessage.setRetryCount(0);
                    }
                    registry.getMessageQueue().offer(context, queuedPublishMessage);
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
    public int countInflight(IMqttsnContext context, InflightMessage.DIRECTION direction) throws MqttsnException {
        Map<Integer, InflightMessage> map = getInflightMessages(context);
        synchronized (map){
            Iterator<Integer> itr = map.keySet().iterator();
            int count = 0;
            while(itr.hasNext()){
                Integer i = itr.next();
                InflightMessage msg = map.get(i);
                if(msg != null && msg.getDirection() == direction) count++;
            }
            return count;
        }
    }

    @Override
    public boolean canSend(IMqttsnContext context) throws MqttsnException {
        return countInflight(context, InflightMessage.DIRECTION.SENDING) == 0;
    }

    @Override
    public Long getMessageLastSentToContext(IMqttsnContext context) {
        return lastMessageSent.get(context);
    }

    @Override
    public Long getMessageLastReceivedFromContext(IMqttsnContext context) {
        return lastMessageReceived.get(context);
    }

    protected String getTopicPathFromPublish(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException {

        int topicIdType = 0;
        byte[] topicData = null;
        if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_1_2) {
            MqttsnPublish publish = (MqttsnPublish) message;
            topicIdType = publish.getTopicType();
            topicData = publish.getTopicData();
        } else  if(context.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0) {
            MqttsnPublish_V2_0 publish = (MqttsnPublish_V2_0) message;
            topicIdType = publish.getTopicIdType();
            topicData = publish.getTopicData();
        }
        TopicInfo info = registry.getTopicRegistry().normalize((byte) topicIdType, topicData, false);
        String topicPath = registry.getTopicRegistry().topicPath(context, info, true);
        return topicPath;
    }

    protected abstract void addInflightMessage(IMqttsnContext context, Integer messageId, InflightMessage message) throws MqttsnException ;

    protected abstract InflightMessage getInflightMessage(IMqttsnContext context, Integer messageId) throws MqttsnException ;

    protected abstract Map<Integer, InflightMessage>  getInflightMessages(IMqttsnContext context) throws MqttsnException;

    protected abstract boolean inflightExists(IMqttsnContext context, Integer messageId) throws MqttsnException;

    static class CommitOperation {

        protected PublishData data;
        protected IMqttsnMessage message;
        protected IMqttsnContext context;
        protected long timestamp;
        protected UUID messageId;
        protected boolean inbound;

        public CommitOperation(IMqttsnContext context, PublishData data, IMqttsnMessage message, boolean inbound){
            this.context = context;
            this.data = data;
            this.message = message;
            this.inbound = inbound;
            this.timestamp = System.currentTimeMillis();
        }

        public static CommitOperation inbound(IMqttsnContext context, PublishData data, IMqttsnMessage message){
            return new CommitOperation(context, data, message, true);
        }

        public static CommitOperation outbound(IMqttsnContext context, UUID messageId, PublishData data, IMqttsnMessage message){
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

        protected final IMqttsnContext context;
        protected final InflightMessage.DIRECTION direction;

        public LastIdContext(IMqttsnContext context, InflightMessage.DIRECTION direction) {
            this.context = context;
            this.direction = direction;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LastIdContext that = (LastIdContext) o;
            return context.equals(that.context) && direction == that.direction;
        }

        @Override
        public int hashCode() {
            return Objects.hash(context, direction);
        }

        @Override
        public String toString() {
            return "LastIdContext{" +
                    "context=" + context +
                    ", direction=" + direction +
                    '}';
        }

        public static LastIdContext from(IMqttsnContext context, InflightMessage.DIRECTION direction){
            return new LastIdContext(context, direction);
        }
    }
}
