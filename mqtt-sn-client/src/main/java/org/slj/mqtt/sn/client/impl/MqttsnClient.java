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

package org.slj.mqtt.sn.client.impl;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.PublishData;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.client.impl.examples.Example;
import org.slj.mqtt.sn.client.spi.IMqttsnClient;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntime;
import org.slj.mqtt.sn.model.*;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.impl.MqttsnQueuedPublishMessageImpl;
import org.slj.mqtt.sn.model.session.impl.MqttsnWillDataImpl;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnHelo;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Provides a blocking command implementation, with the ability to handle transparent reconnection
 * during unsolicited disconnection events.
 *
 * Publishing occurs asynchronously and is managed by a FIFO queue. The size of the queue is determined
 * by the configuration supplied.
 *
 * Connect, subscribe, unsubscribe and disconnect ( &amp; sleep) are blocking calls which are considered successful on
 * receipt of the correlated acknowledgement message.
 *
 * Management of the sleeping client state can either be supervised by the application, or by the client itself. During
 * the sleep cycle, underlying resources (threads) are intelligently started and stopped. For example during sleep, the
 * queue processing is closed down, and restarted during the Connected state.
 *
 * The client is {@link java.io.Closeable}. On close, a remote DISCONNECT operation is run (if required) and all encapsulated
 * state (timers and threads) are stopped gracefully at best attempt. Once a client has been closed, it should be discarded and a new instance created
 * should a new connection be required.
 *
 * For example use, please refer to {@link Example}.
 */
public class MqttsnClient extends AbstractMqttsnRuntime implements IMqttsnClient {
    private volatile IMqttsnSession session;
    private volatile int keepAlive;
    private volatile boolean cleanSession;
    private int errorRetryCounter = 0;
    private Thread managedConnectionThread = null;
    private final Object sleepMonitor = new Object();
    private final Object connectionMonitor = new Object();
    private final Object functionMutex = new Object();
    private final boolean managedConnection;
    private final boolean autoReconnect;

    /**
     * Construct a new client instance whose connection is NOT automatically managed. It will be up to the application
     * to monitor and manage the connection lifecycle.
     *
     * If you wish to have the client supervise your connection (including active pings and unsolicited disconnect handling) then you
     * should use the constructor which specifies managedConnected = true.
     */
    public MqttsnClient(){
        this(false, false);
    }

    /**
     * Construct a new client instance specifying whether you want to client to automatically handle unsolicited DISCONNECT
     * events.
     *
     * @param managedConnection - You can choose to use managed connections which will actively monitor your connection with the remote gateway,
     *                         and issue PINGS where neccessary to keep your session alive
     */
    public MqttsnClient(boolean managedConnection){
        this(managedConnection, true);
    }

    /**
     * Construct a new client instance specifying whether you want to client to automatically handle unsolicited DISCONNECT
     * events.
     *
     * @param managedConnection - You can choose to use managed connections which will actively monitor your connection with the remote gateway,
     *      *                         and issue PINGS where neccessary to keep your session alive
     *
     * @param autoReconnect - When operating in managedConnection mode, should we attempt to silently reconnect if we detected a dropped
     *                      connection
     */
    public MqttsnClient(boolean managedConnection, boolean autoReconnect){
        this.managedConnection = managedConnection;
        this.autoReconnect = autoReconnect;
        registerConnectionListener(connectionListener);
    }

    protected void resetErrorState(){
        errorRetryCounter = 0;
    }

    @Override
    protected void notifyServicesStarted() {
        try {
            Optional<INetworkContext> optionalContext = registry.getNetworkRegistry().first();
            if(!registry.getOptions().isEnableDiscovery() &&
                    !optionalContext.isPresent()){
                throw new MqttsnRuntimeException("unable to launch non-discoverable client without configured gateway");
            }
        } catch(NetworkRegistryException e){
            throw new MqttsnRuntimeException("error using network registry", e);
        }
    }

    @Override
    public boolean isConnected() {
        try {
            synchronized(functionMutex){
                IMqttsnSession state = checkSession(false);
                if(state == null) return false;
                return state.getClientState() == MqttsnClientState.ACTIVE;
            }
        } catch(MqttsnException e){
            logger.warn("error checking connection state", e);
            return false;
        }
    }

    @Override
    public boolean isAsleep() {
        try {
            IMqttsnSession state = checkSession(false);
            if(state == null) return false;
            return state.getClientState() == MqttsnClientState.ASLEEP;
        } catch(MqttsnException e){
            return false;
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#connect(int, boolean)}
     */
    public void connect(int keepAlive, boolean cleanSession) throws MqttsnException, MqttsnClientConnectException{

        this.keepAlive = keepAlive;
        this.cleanSession = cleanSession;
        IMqttsnSession session = checkSession(false);
        synchronized (functionMutex) {
            //-- its assumed regardless of being already connected or not, if connect is called
            //-- local state should be discarded
            clearState(cleanSession);
            if (session.getClientState() != MqttsnClientState.ACTIVE) {
                startProcessing(false);
                try {
                    IMqttsnMessage message = registry.getMessageFactory().createConnect(
                            registry.getOptions().getContextId(), keepAlive,
                            registry.getWillRegistry().hasWillMessage(session), cleanSession,
                            registry.getOptions().getMaxProtocolMessageSize(),
                            registry.getOptions().getDefaultMaxAwakeMessages(),
                            registry.getOptions().getSessionExpiryInterval());

                    MqttsnWaitToken token = registry.getMessageStateService().sendMessage(session.getContext(), message);
                    Optional<IMqttsnMessage> response =
                            registry.getMessageStateService().waitForCompletion(session.getContext(), token);
                    stateChangeResponseCheck(session, token, response, MqttsnClientState.ACTIVE);

                    getRegistry().getSessionRegistry().modifyKeepAlive(session, keepAlive);
                    startProcessing(true);
                } catch(MqttsnExpectationFailedException e){
                    //-- something was not correct with the CONNECT, shut it down again
                    logger.warn("error issuing CONNECT, disconnect");
                    getRegistry().getSessionRegistry().modifyClientState(session, MqttsnClientState.DISCONNECTED);
                    stopProcessing();
                    throw new MqttsnClientConnectException(e);
                }
            }
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#setWillData(MqttsnWillDataImpl)}
     */
    public void setWillData(MqttsnWillDataImpl willData) throws MqttsnException {

        //if connected, we need to update the existing session
        IMqttsnSession session = checkSession(false);

        registry.getWillRegistry().setWillMessage(session, willData);

        if (session.getClientState() == MqttsnClientState.ACTIVE) {
            try {

                //-- topic update first
                IMqttsnMessage message = registry.getMessageFactory().createWillTopicupd(
                        willData.getQos(),willData.isRetained(), willData.getTopicPath().toString());
                MqttsnWaitToken token = registry.getMessageStateService().sendMessage(session.getContext(), message);
                Optional<IMqttsnMessage> response =
                        registry.getMessageStateService().waitForCompletion(session.getContext(), token);
                MqttsnUtils.responseCheck(token, response);

                //-- then the data udpate
                message = registry.getMessageFactory().createWillMsgupd(willData.getData());
                token = registry.getMessageStateService().sendMessage(session.getContext(), message);
                response =
                        registry.getMessageStateService().waitForCompletion(session.getContext(), token);
                MqttsnUtils.responseCheck(token, response);

            } catch(MqttsnExpectationFailedException e){
                //-- something was not correct with the CONNECT, shut it down again
                logger.warn("error issuing WILL UPDATE", e);
                throw e;
            }
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#setWillData()}
     */
    public void clearWillData() throws MqttsnException {
        IMqttsnSession session = checkSession(false);
        registry.getWillRegistry().clear(session);
    }

    @Override
    /**
     * @see {@link IMqttsnClient#waitForCompletion(MqttsnWaitToken, int)}
     */
    public Optional<IMqttsnMessage> waitForCompletion(MqttsnWaitToken token, int customWaitTime) throws MqttsnExpectationFailedException {
        synchronized (functionMutex){
            Optional<IMqttsnMessage> response = registry.getMessageStateService().waitForCompletion(
                    session.getContext(), token, customWaitTime);
            MqttsnUtils.responseCheck(token, response);
            return response;
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#publish(String, int, boolean, byte[])}
     */
    public MqttsnWaitToken publish(String topicName, int QoS, boolean retained, byte[] data) throws MqttsnException, MqttsnQueueAcceptException{

        MqttsnSpecificationValidator.validateQoS(QoS);
        MqttsnSpecificationValidator.validatePublishPath(topicName);
        MqttsnSpecificationValidator.validatePublishData(data);

        if(QoS == -1){
            if(topicName.length() > 2){
                Integer alias = registry.getTopicRegistry().lookupPredefined(session, topicName);
                if(alias == null)
                    throw new MqttsnExpectationFailedException("can only publish to PREDEFINED topics or SHORT topics at QoS -1");
            }
        }

        IMqttsnSession session = checkSession(QoS >= 0);
        if(MqttsnUtils.in(session.getClientState(),
                MqttsnClientState.ASLEEP, MqttsnClientState.DISCONNECTED)){
            startProcessing(true);
        }
        PublishData publishData = new PublishData(topicName, QoS, retained);
        IMqttsnDataRef dataRef = registry.getMessageRegistry().add(data);
        return registry.getMessageQueue().offerWithToken(session,
                new MqttsnQueuedPublishMessageImpl(
                        dataRef, publishData));
    }

    @Override
    /**
     * @see {@link IMqttsnClient#subscribe(String, int)}
     */
    public void subscribe(String topicName, int QoS) throws MqttsnException{

        MqttsnSpecificationValidator.validateQoS(QoS);
        MqttsnSpecificationValidator.validateSubscribePath(topicName);

        IMqttsnSession session = checkSession(true);

        TopicInfo info = registry.getTopicRegistry().lookup(session, topicName, true);
        IMqttsnMessage message;
        if(info == null || info.getType() == MqttsnConstants.TOPIC_TYPE.SHORT ||
                info.getType() == MqttsnConstants.TOPIC_TYPE.NORMAL){
            //-- the spec is ambiguous here; where a normalId has been obtained, it still requires use of
            //-- topicName string
            message = registry.getMessageFactory().createSubscribe(QoS, topicName);
        }
        else {
            //-- only predefined should use the topicId as an uint16
            message = registry.getMessageFactory().createSubscribe(QoS, info.getType(), info.getTopicId());
        }

        synchronized (this){
            MqttsnWaitToken token = registry.getMessageStateService().sendMessage(session.getContext(), message);
            Optional<IMqttsnMessage> response = registry.getMessageStateService().waitForCompletion(session.getContext(), token);
            MqttsnUtils.responseCheck(token, response);
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#unsubscribe(String)}
     */
    public void unsubscribe(String topicName) throws MqttsnException{

        MqttsnSpecificationValidator.validateSubscribePath(topicName);

        IMqttsnSession session = checkSession(true);

        TopicInfo info = registry.getTopicRegistry().lookup(session, topicName, true);
        IMqttsnMessage message;
        if(info == null || info.getType() == MqttsnConstants.TOPIC_TYPE.SHORT ||
                info.getType() == MqttsnConstants.TOPIC_TYPE.NORMAL){
            //-- the spec is ambiguous here; where a normalId has been obtained, it still requires use of
            //-- topicName string
            message = registry.getMessageFactory().createUnsubscribe(topicName);
        }
        else {
            //-- only predefined should use the topicId as an uint16
            message = registry.getMessageFactory().createUnsubscribe(info.getType(), info.getTopicId());
        }

        synchronized (functionMutex){
            MqttsnWaitToken token = registry.getMessageStateService().sendMessage(session.getContext(), message);
            Optional<IMqttsnMessage> response = registry.getMessageStateService().waitForCompletion(session.getContext(), token);
            MqttsnUtils.responseCheck(token, response);
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#supervisedSleepWithWake(int, int, int, boolean)}
     */
    public void supervisedSleepWithWake(int duration, int wakeAfterIntervalSeconds, int maxWaitTimeMillis, boolean connectOnFinish)
            throws MqttsnException, MqttsnClientConnectException {

        MqttsnSpecificationValidator.validateDuration(duration);

        if(wakeAfterIntervalSeconds > duration)
           throw new MqttsnExpectationFailedException("sleep duration must be greater than the wake after period");

        long now = System.currentTimeMillis();
        long sleepUntil = now + (duration * 1000L);
        sleep(duration);
        while(sleepUntil > (now = System.currentTimeMillis())){
            long timeLeft = sleepUntil - now;
            long period = (int) Math.min(duration, timeLeft / 1000);
            //-- sleep for the wake after period
            try {
                long wake = Math.min(wakeAfterIntervalSeconds, period);
                if(wake > 0){
                    logger.info("will wake after {} seconds", wake);
                    synchronized (sleepMonitor){
                        //TODO protect against spurious wake up here
                        sleepMonitor.wait(wake * 1000);
                    }
                    wake(maxWaitTimeMillis);
                } else {
                    break;
                }
            } catch(InterruptedException e){
                Thread.currentThread().interrupt();
                throw new MqttsnException(e);
            }
        }

        if(connectOnFinish){
            IMqttsnSession state = checkSession(false);
            connect(state.getKeepAlive(), false);
        } else {
            startProcessing(false);
            disconnect();
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#sleep(int)}
     */
    public void sleep(long sessionExpiryInterval)  throws MqttsnException{

        MqttsnSpecificationValidator.validateSessionExpiry(sessionExpiryInterval);

        logger.info("sleeping for {} seconds", sessionExpiryInterval);
        IMqttsnSession state = checkSession(true);
        IMqttsnMessage message = registry.getMessageFactory().createDisconnect(sessionExpiryInterval);
        synchronized (functionMutex){
            MqttsnWaitToken token = registry.getMessageStateService().sendMessage(state.getContext(), message);
            Optional<IMqttsnMessage> response = registry.getMessageStateService().waitForCompletion(state.getContext(), token);
            stateChangeResponseCheck(state, token, response, MqttsnClientState.ASLEEP);
            getRegistry().getSessionRegistry().modifyKeepAlive(state, 0);
            clearState(false);
            stopProcessing();
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#wake()}
     */
    public void wake()  throws MqttsnException{
        wake(registry.getOptions().getMaxWait());
    }

    @Override
    /**
     * @see {@link IMqttsnClient#wake(int)}
     */
    public void wake(int waitTime)  throws MqttsnException{

        IMqttsnSession state = checkSession(false);
        IMqttsnMessage message = registry.getMessageFactory().createPingreq(registry.getOptions().getContextId());
        synchronized (functionMutex){
            if(MqttsnUtils.in(state.getClientState(),
                    MqttsnClientState.ASLEEP)){
                startProcessing(false);
                MqttsnWaitToken token = registry.getMessageStateService().sendMessage(state.getContext(), message);
                getRegistry().getSessionRegistry().modifyClientState(state, MqttsnClientState.AWAKE);
                try {
                    Optional<IMqttsnMessage> response = registry.getMessageStateService().waitForCompletion(state.getContext(), token,
                            waitTime);
                    stateChangeResponseCheck(state, token, response, MqttsnClientState.ASLEEP);
                    stopProcessing();
                } catch(MqttsnExpectationFailedException e){
                    //-- this means NOTHING was received after my sleep - gateway may have gone, so disconnect and
                    //-- force CONNECT to be next operation
                    disconnect(false, false);
                    throw new MqttsnExpectationFailedException("gateway did not respond to AWAKE state; disconnected");
                }
            } else {
                throw new MqttsnExpectationFailedException("client cannot wake from a non-sleep state");
            }
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#ping()}
     */
    public void ping()  throws MqttsnException{
        IMqttsnSession state = checkSession(true);
        IMqttsnMessage message = registry.getMessageFactory().createPingreq(registry.getOptions().getContextId());
        synchronized (functionMutex){
            MqttsnWaitToken token = registry.getMessageStateService().sendMessage(state.getContext(), message);
            registry.getMessageStateService().waitForCompletion(state.getContext(), token);
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#helo()}
     */
    public String helo()  throws MqttsnException{
        IMqttsnSession state = checkSession(true);
        IMqttsnMessage message = registry.getMessageFactory().createHelo(null);
        synchronized (functionMutex){
            MqttsnWaitToken token = registry.getMessageStateService().sendMessage(state.getContext(), message);
            Optional<IMqttsnMessage> response = registry.getMessageStateService().waitForCompletion(state.getContext(), token);
            if(response.isPresent()){
                return ((MqttsnHelo)response.get()).getUserAgent();
            }
        }
        return null;
    }

    @Override
    /**
     * @see {@link IMqttsnClient#disconnect()}
     */
    public void disconnect()  throws MqttsnException {
        disconnect(true, false);
    }

    private void disconnect(boolean sendRemoteDisconnect, boolean deepClean)  throws MqttsnException {
        try {
            IMqttsnSession state = checkSession(false);
            synchronized (functionMutex) {
                if(state != null){
                    try {
                        if (MqttsnUtils.in(state.getClientState(),
                                MqttsnClientState.ACTIVE, MqttsnClientState.ASLEEP, MqttsnClientState.AWAKE)) {
                            logger.info("disconnecting client; deepClean ? [{}], sending remote disconnect ? {}", deepClean, sendRemoteDisconnect);
                            if(sendRemoteDisconnect){
                                IMqttsnMessage message = registry.getMessageFactory().createDisconnect();
                                registry.getMessageStateService().sendMessage(state.getContext(), message);
                            }
                        }
                    } finally {
                        clearState(deepClean);
                        getRegistry().getSessionRegistry().modifyClientState(state, MqttsnClientState.DISCONNECTED);
                    }
                }
            }
        } finally {
            stopProcessing();
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#close()}
     */
    public void close() throws IOException {
        try {
            disconnect();
        } catch(MqttsnException e){
            throw new IOException(e);
        } finally {
            try {
                if(registry != null)
                    stop();
            }
            catch(MqttsnException e){
                throw new IOException (e);
            } finally {
                if(managedConnectionThread != null){
                    synchronized (connectionMonitor){
                        connectionMonitor.notifyAll();
                    }
                }
            }
        }
    }

    /**
     * @see {@link IMqttsnClient#getClientId()}
     */
    public String getClientId(){
        return registry.getOptions().getContextId();
    }

    private void stateChangeResponseCheck(IMqttsnSession session, MqttsnWaitToken token, Optional<IMqttsnMessage> response, MqttsnClientState newState)
            throws MqttsnExpectationFailedException {
        try {
            MqttsnUtils.responseCheck(token, response);
            if(response.isPresent() &&
                    !response.get().isErrorMessage()){
                getRegistry().getSessionRegistry().modifyClientState(session, newState);
            }
        } catch(MqttsnExpectationFailedException e){
            logger.error("operation could not be completed, error in response");
            throw e;
        }
    }

    private IMqttsnSession discoverGatewaySession() throws MqttsnException {
        if(session == null){
            synchronized (functionMutex){
                if(session == null){
                    try {
                        logger.info("discovering gateway...");
                        Optional<INetworkContext> optionalMqttsnContext =
                                registry.getNetworkRegistry().waitForContext(registry.getOptions().getDiscoveryTime(), TimeUnit.SECONDS);
                        if(optionalMqttsnContext.isPresent()){
                            INetworkContext networkContext = optionalMqttsnContext.get();
                            session = registry.getSessionRegistry().createNewSession(registry.getNetworkRegistry().getMqttsnContext(networkContext));
                            logger.info("discovery located a gateway for use {}", networkContext);
                        } else {
                            throw new MqttsnException("unable to discovery gateway within specified timeout");
                        }
                    } catch(NetworkRegistryException | InterruptedException e){
                        throw new MqttsnException("discovery was interrupted and no gateway was found", e);
                    }
                }
            }
        }
        return session;
    }

    public int getPingDelta(){
        return (Math.max(keepAlive, 60) / registry.getOptions().getPingDivisor());
    }

    private void activateManagedConnection(){
        if(managedConnectionThread == null){
            managedConnectionThread = new Thread(() -> {
                while(running){
                    try {
                        synchronized (connectionMonitor){
                            long delta = errorRetryCounter > 0 ? registry.getOptions().getMaxErrorRetryTime() : getPingDelta()  * 1000L;
                            logger.debug("managed connection monitor is running at time delta {}, keepAlive {}...", delta, keepAlive);
                            connectionMonitor.wait(delta);

                            if(running){
                                synchronized (functionMutex){ //-- we could receive a unsolicited disconnect during passive reconnection | ping..
                                    IMqttsnSession state = checkSession(false);
                                    if(state != null){
                                        if(state.getClientState() == MqttsnClientState.DISCONNECTED){
                                            if(autoReconnect){
                                                logger.info("client connection set to auto-reconnect...");
                                                connect(keepAlive, false);
                                                resetErrorState();
                                            }
                                        }
                                        else if(state.getClientState() == MqttsnClientState.ACTIVE){
                                            if(keepAlive > 0){ //-- keepAlive 0 means alive forever, dont bother pinging
                                                Long lastMessageSent = registry.getMessageStateService().
                                                        getMessageLastSentToContext(state.getContext());
                                                if(lastMessageSent == null || System.currentTimeMillis() >
                                                        lastMessageSent + delta ){
                                                    logger.info("managed connection issuing ping...");
                                                    ping();
                                                    resetErrorState();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch(Exception e){
                        try {
                            if(errorRetryCounter++ >= registry.getOptions().getMaxErrorRetries()){
                                logger.error("error on connection manager thread, DISCONNECTING", e);
                                resetErrorState();
                                disconnect(false, true);
                            } else {
                                registry.getMessageStateService().clearInflight(getSessionState().getContext());
                                logger.warn("error on connection manager thread, execute retransmission", e);
                            }
                        } catch(Exception ex){
                            logger.warn("error handling re-tranmission on connection manager thread, execute retransmission", e);
                        }
                    }
                }
                logger.warn("managed-connection closing down");
            }, "mqtt-sn-managed-connection");
            managedConnectionThread.setPriority(Thread.MIN_PRIORITY);
            managedConnectionThread.setDaemon(true);
            managedConnectionThread.start();
        }
    }

    public void resetConnection(IMqttsnContext context, Throwable t, boolean attemptRestart) {
        try {
            logger.warn("connection lost at transport layer", t);
            disconnect(false, true);
            //attempt to restart transport
            callShutdown(registry.getTransport());
            if(attemptRestart){
                callStartup(registry.getTransport());
                if(managedConnectionThread != null){
                    try {
                        synchronized (connectionMonitor){
                            connectionMonitor.notify();
                        }
                    } catch(Exception e){
                        logger.warn("error encountered when trying to recover from unsolicited disconnect", e);
                    }
                }
            }
        } catch(Exception e){
            logger.warn("error encountered resetting connection", e);
        }
    }

    private IMqttsnSession checkSession(boolean validateConnected) throws MqttsnException {
        IMqttsnSession session = discoverGatewaySession();
        if(validateConnected && session.getClientState() != MqttsnClientState.ACTIVE)
            throw new MqttsnRuntimeException("client not connected");
        return session;
    }

    private void stopProcessing() throws MqttsnException {
        //-- ensure we stop message queue sending when we are not connected
        registry.getMessageStateService().unscheduleFlush(session.getContext());
        callShutdown(registry.getMessageHandler());
        callShutdown(registry.getMessageStateService());
    }

    private void startProcessing(boolean processQueue) throws MqttsnException {

        callStartup(registry.getTransport());
        callStartup(registry.getMessageStateService());
        callStartup(registry.getMessageHandler());
        if(processQueue){
            if(managedConnection){
                activateManagedConnection();
            }
        }
    }

    private void clearState(boolean deepClear) throws MqttsnException {
        //-- unsolicited disconnect notify to the application
        IMqttsnSession session = checkSession(false);
        if(session != null){
            logger.info("clearing state, deep clean ? {}", deepClear);
            registry.getMessageStateService().clearInflight(session.getContext());
            registry.getTopicRegistry().clear(session,
                    deepClear || registry.getOptions().isSleepClearsRegistrations());
            if(getSessionState() != null) {
                getRegistry().getSessionRegistry().modifyKeepAlive(session, 0);
            }
            if(deepClear){
                registry.getSubscriptionRegistry().clear(session);
            }
        }
    }

    public long getQueueSize() throws MqttsnException {
        if(session != null){
            return registry.getMessageQueue().queueSize(session);
        }
        return 0;
    }

    public long getIdleTime() throws MqttsnException {
        if(session != null){
            Long l = registry.getMessageStateService().getLastActiveMessage(session.getContext());
            if(l != null){
                return System.currentTimeMillis() - l;
            }
        }
        return 0;
    }

    public IMqttsnSession getSessionState(){
        return session;
    }

    protected IMqttsnConnectionStateListener connectionListener =
            new IMqttsnConnectionStateListener() {

        @Override
        public void notifyConnected(IMqttsnContext context) {
        }

        @Override
        public void notifyRemoteDisconnect(IMqttsnContext context) {
            try {
                disconnect(false, true);
            } catch(Exception e){}
        }

        @Override
        public void notifyActiveTimeout(IMqttsnContext context) {
        }

        @Override
        public void notifyLocalDisconnect(IMqttsnContext context, Throwable t) {
        }

        @Override
        public void notifyConnectionLost(IMqttsnContext context, Throwable t) {
            resetConnection(context, t, autoReconnect);
        }
    };
}
