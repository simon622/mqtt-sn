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
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.client.impl.examples.Example;
import org.slj.mqtt.sn.client.spi.IMqttsnClient;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntime;
import org.slj.mqtt.sn.model.*;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

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

    private volatile MqttsnSessionState state;
    private volatile int keepAlive;
    private volatile boolean cleanSession;

    private volatile int errorRetryCounter = 0;
    private Thread managedConnectionThread = null;

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
    protected void startupServices(IMqttsnRuntimeRegistry registry) throws MqttsnException {

        try {
            Optional<INetworkContext> optionalContext = registry.getNetworkRegistry().first();
            if(!registry.getOptions().isEnableDiscovery() &&
                    !optionalContext.isPresent()){
                throw new MqttsnRuntimeException("unable to launch non-discoverable client without configured gateway");
            }
        } catch(NetworkRegistryException e){
            throw new MqttsnException("error using network registry", e);
        }

        callStartup(registry.getMessageStateService());
        callStartup(registry.getMessageHandler());
        callStartup(registry.getMessageQueue());
        callStartup(registry.getMessageRegistry());
        callStartup(registry.getContextFactory());
        callStartup(registry.getSubscriptionRegistry());
        callStartup(registry.getTopicRegistry());
        callStartup(registry.getQueueProcessor());
        callStartup(registry.getTransport());
    }

    @Override
    protected void stopServices(IMqttsnRuntimeRegistry registry) throws MqttsnException {
        callShutdown(registry.getTransport());
        callShutdown(registry.getQueueProcessor());
        callShutdown(registry.getMessageStateService());
        callShutdown(registry.getMessageHandler());
        callShutdown(registry.getMessageQueue());
        callShutdown(registry.getMessageRegistry());
        callShutdown(registry.getContextFactory());
        callShutdown(registry.getSubscriptionRegistry());
        callShutdown(registry.getTopicRegistry());
    }

    @Override
    public boolean isConnected() {
        try {
            IMqttsnSessionState state = checkSession(false);
            return state.getClientState() == MqttsnClientState.CONNECTED;
        } catch(MqttsnException e){
            return false;
        }
    }

    @Override
    public boolean isAsleep() {
        try {
            IMqttsnSessionState state = checkSession(false);
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
        if(!MqttsnUtils.validUInt16(keepAlive)){
            throw new MqttsnExpectationFailedException("invalid keepAlive supplied");
        }
        this.keepAlive = keepAlive;
        this.cleanSession = cleanSession;
        IMqttsnSessionState state = checkSession(false);
        synchronized (this) {
            //-- its assumed regardless of being already connected or not, if connect is called
            //-- local state should be discarded
            clearState(state.getContext(), cleanSession);
            if (state.getClientState() != MqttsnClientState.CONNECTED) {
                startProcessing(false);
                try {
                    IMqttsnMessage message = registry.getMessageFactory().createConnect(
                            registry.getOptions().getContextId(), keepAlive, false, cleanSession);
                    MqttsnWaitToken token = registry.getMessageStateService().sendMessage(state.getContext(), message);
                    Optional<IMqttsnMessage> response =
                            registry.getMessageStateService().waitForCompletion(state.getContext(), token);
                    stateChangeResponseCheck(state, token, response, MqttsnClientState.CONNECTED);
                    state.setKeepAlive(keepAlive);
                    startProcessing(true);
                } catch(MqttsnExpectationFailedException e){
                    //-- something was not correct with the CONNECT, shut it down again
                    logger.log(Level.WARNING, "error issuing CONNECT, disconnect");
                    state.setClientState(MqttsnClientState.DISCONNECTED);
                    stopProcessing();
                    throw new MqttsnClientConnectException(e);
                }
            }
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#waitForCompletion(MqttsnWaitToken, int)}
     */
    public Optional<IMqttsnMessage> waitForCompletion(MqttsnWaitToken token, int customWaitTime) throws MqttsnExpectationFailedException {
        synchronized (this){
            Optional<IMqttsnMessage> response = registry.getMessageStateService().waitForCompletion(
                    state.getContext(), token, customWaitTime);
            MqttsnUtils.responseCheck(token, response);
            return response;
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#publish(String, int, byte[])}
     */
    public MqttsnWaitToken publish(String topicName, int QoS, byte[] data) throws MqttsnException, MqttsnQueueAcceptException{
        if(!MqttsnUtils.validQos(QoS)){
            throw new MqttsnExpectationFailedException("invalid QoS supplied");
        }
        if(!MqttsnUtils.validTopicName(topicName)){
            throw new MqttsnExpectationFailedException("invalid topicName supplied");
        }

        if(QoS == -1){
            Integer alias = registry.getTopicRegistry().lookupPredefined(state.getContext(), topicName);
            if(alias == null)
                throw new MqttsnExpectationFailedException("can only publish to PREDEFINED topics at QoS -1");
        }

        IMqttsnSessionState state = checkSession(QoS >= 0);
        UUID messageId = registry.getMessageRegistry().add(data, getMessageExpiry());
        MqttsnWaitToken token = registry.getMessageQueue().offer(state.getContext(),
                new QueuedPublishMessage(
                        messageId, topicName, QoS));
        return token;
    }

    @Override
    /**
     * @see {@link IMqttsnClient#subscribe(String, int)}
     */
    public void subscribe(String topicName, int QoS) throws MqttsnException{
        if(!MqttsnUtils.validTopicName(topicName)){
            throw new MqttsnExpectationFailedException("invalid topicName supplied");
        }
        if(!MqttsnUtils.validQos(QoS)){
            throw new MqttsnExpectationFailedException("invalid QoS supplied");
        }
        IMqttsnSessionState state = checkSession(true);

        TopicInfo info = registry.getTopicRegistry().lookup(state.getContext(), topicName, true);
        IMqttsnMessage message = null;
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
            MqttsnWaitToken token = registry.getMessageStateService().sendMessage(state.getContext(), message);
            Optional<IMqttsnMessage> response = registry.getMessageStateService().waitForCompletion(state.getContext(), token);
            MqttsnUtils.responseCheck(token, response);
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#unsubscribe(String)}
     */
    public void unsubscribe(String topicName) throws MqttsnException{
        if(!MqttsnUtils.validTopicName(topicName)){
            throw new MqttsnExpectationFailedException("invalid topicName supplied");
        }
        IMqttsnSessionState state = checkSession(true);

        TopicInfo info = registry.getTopicRegistry().lookup(state.getContext(), topicName, true);
        IMqttsnMessage message = null;
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

        synchronized (this){
            MqttsnWaitToken token = registry.getMessageStateService().sendMessage(state.getContext(), message);
            Optional<IMqttsnMessage> response = registry.getMessageStateService().waitForCompletion(state.getContext(), token);
            MqttsnUtils.responseCheck(token, response);
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#supervisedSleepWithWake(int, int, int, boolean)}
     */
    public void supervisedSleepWithWake(int duration, int wakeAfterIntervalSeconds, int maxWaitTimeMillis, boolean connectOnFinish)
            throws MqttsnException, MqttsnClientConnectException {

        if(!MqttsnUtils.validUInt16(duration)){
            throw new MqttsnExpectationFailedException("invalid duration supplied");
        }

        if(!MqttsnUtils.validUInt16(wakeAfterIntervalSeconds)){
            throw new MqttsnExpectationFailedException("invalid wakeAfterInterval supplied");
        }

        if(wakeAfterIntervalSeconds > duration)
           throw new MqttsnExpectationFailedException("sleep duration must be greater than the wake after period");

        long now = System.currentTimeMillis();
        long sleepUntil = now + (duration * 1000);
        sleep(duration);
        while(sleepUntil > (now = System.currentTimeMillis())){
            long timeLeft = sleepUntil - now;
            long period = (int) Math.min(duration, timeLeft / 1000);
            //-- sleep for the wake after period
            try {
                long wake = Math.min(wakeAfterIntervalSeconds, period);
                if(wake > 0){
                    logger.log(Level.INFO, String.format("waking after [%s] seconds", wake));
                    Thread.sleep(wake * 1000);
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
            IMqttsnSessionState state = checkSession(false);
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
    public void sleep(int duration)  throws MqttsnException{
        if(!MqttsnUtils.validUInt16(duration)){
            throw new MqttsnExpectationFailedException("invalid duration supplied");
        }
        logger.log(Level.INFO, String.format("sleeping for [%s] seconds", duration));
        IMqttsnSessionState state = checkSession(true);
        IMqttsnMessage message = registry.getMessageFactory().createDisconnect(duration);
        synchronized (this){
            MqttsnWaitToken token = registry.getMessageStateService().sendMessage(state.getContext(), message);
            Optional<IMqttsnMessage> response = registry.getMessageStateService().waitForCompletion(state.getContext(), token);
            stateChangeResponseCheck(state, token, response, MqttsnClientState.ASLEEP);
            state.setKeepAlive(0);
            clearState(state.getContext(),false);
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
        IMqttsnSessionState state = checkSession(false);
        IMqttsnMessage message = registry.getMessageFactory().createPingreq(registry.getOptions().getContextId());
        synchronized (this){
            if(MqttsnUtils.in(state.getClientState(),
                    MqttsnClientState.ASLEEP)){
                startProcessing(false);
                MqttsnWaitToken token = registry.getMessageStateService().sendMessage(state.getContext(), message);
                state.setClientState(MqttsnClientState.AWAKE);
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
        IMqttsnSessionState state = checkSession(true);
        IMqttsnMessage message = registry.getMessageFactory().createPingreq(registry.getOptions().getContextId());
        synchronized (this){
            MqttsnWaitToken token = registry.getMessageStateService().sendMessage(state.getContext(), message);
            registry.getMessageStateService().waitForCompletion(state.getContext(), token);
        }
    }

    @Override
    /**
     * @see {@link IMqttsnClient#disconnect()}
     */
    public void disconnect()  throws MqttsnException {
        disconnect(true, true);
    }

    private void disconnect(boolean sendRemoteDisconnect, boolean deepClean)  throws MqttsnException {
        try {
            IMqttsnSessionState state = checkSession(false);
            synchronized (this) {
                if(state != null){
                    try {
                        if (MqttsnUtils.in(state.getClientState(),
                                MqttsnClientState.CONNECTED, MqttsnClientState.ASLEEP, MqttsnClientState.AWAKE)) {
                            logger.log(Level.INFO, String.format("disconnecting client; deepClean ? [%s], sending remote disconnect ? [%s]", deepClean, sendRemoteDisconnect));
                            clearState(state.getContext(), deepClean);
                            if(sendRemoteDisconnect){
                                IMqttsnMessage message = registry.getMessageFactory().createDisconnect();
                                MqttsnWaitToken token = registry.getMessageStateService().sendMessage(state.getContext(), message);
                            }
                        }
                    } finally {
                        state.setClientState(MqttsnClientState.DISCONNECTED);
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
                    synchronized (managedConnectionThread){
                        managedConnectionThread.notifyAll();
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

    private void stateChangeResponseCheck(IMqttsnSessionState sessionState, MqttsnWaitToken token, Optional<IMqttsnMessage> response, MqttsnClientState newState)
            throws MqttsnExpectationFailedException {
        try {
            MqttsnUtils.responseCheck(token, response);
            if(response.isPresent() &&
                    !response.get().isErrorMessage()){
                sessionState.setClientState(newState);
            }
        } catch(MqttsnExpectationFailedException e){
            logger.log(Level.SEVERE, "operation could not be completed, error in response");
            throw e;
        }
    }

    private MqttsnSessionState discoverGatewaySession() throws MqttsnException {
        if(state == null){
            synchronized (this){
                if(state == null){
                    try {
                        logger.log(Level.INFO, "discovering gateway...");
                        Optional<INetworkContext> optionalMqttsnContext =
                                registry.getNetworkRegistry().waitForContext(registry.getOptions().getDiscoveryTime(), TimeUnit.SECONDS);
                        if(optionalMqttsnContext.isPresent()){
                            INetworkContext gatewayContext = optionalMqttsnContext.get();
                            state = new MqttsnSessionState(registry.getNetworkRegistry().getSessionContext(gatewayContext), MqttsnClientState.PENDING);
                            logger.log(Level.INFO, String.format("discovery located a gateway for use [%s]", gatewayContext));
                        } else {
                            throw new MqttsnException("unable to discovery gateway within specified timeout");
                        }
                    } catch(NetworkRegistryException | InterruptedException e){
                        throw new MqttsnException("discovery was interrupted and no gateway was found", e);
                    }
                }
            }
        }
        return state;
    }

    public int getPingDelta(){
        return (Math.max(keepAlive, 60) / registry.getOptions().getPingDivisor());
    }

    private void activateManagedConnection(){
        if(managedConnectionThread == null){
            managedConnectionThread = new Thread(() -> {
                while(running){
                    try {
                        synchronized (managedConnectionThread){
                            long delta = errorRetryCounter > 0 ? registry.getOptions().getMaxErrorRetryTime() : getPingDelta()  * 1000;
                            logger.log(Level.FINE,
                                    String.format("managed connection monitor is running at time delta [%s], keepAlive [%s]...", delta, keepAlive));
                            managedConnectionThread.wait(delta);

                            if(running){
                                synchronized (this){ //-- we could receive a unsolicited disconnect during passive reconnection | ping..
                                    IMqttsnSessionState state = checkSession(false);
                                    if(state != null){
                                        if(state.getClientState() == MqttsnClientState.DISCONNECTED){
                                            if(autoReconnect){
                                                logger.log(Level.INFO, "client connection set to auto-reconnect...");
                                                connect(keepAlive, false);
                                                resetErrorState();
                                            }
                                        }
                                        else if(state.getClientState() == MqttsnClientState.CONNECTED){
                                            if(keepAlive > 0){ //-- keepAlive 0 means alive forever, dont bother pinging
                                                Long lastMessageSent = registry.getMessageStateService().
                                                        getMessageLastSentToContext(state.getContext());
                                                if(lastMessageSent == null || System.currentTimeMillis() >
                                                        lastMessageSent + delta ){
                                                    logger.log(Level.INFO, "managed connection issuing ping...");
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
                                logger.log(Level.SEVERE, String.format("error [%s] on connection manager thread, DISCONNECTING", errorRetryCounter), e);
                                resetErrorState();
                                disconnect(false, true);
                            } else {
                                registry.getMessageStateService().clearInflight(getSessionState().getContext());
                                logger.log(Level.WARNING, String.format("error [%s] on connection manager thread, execute retransmission", errorRetryCounter), e);
                            }
                        } catch(Exception ex){
                            logger.log(Level.WARNING, String.format("error handling retranmission [%s] on connection manager thread, execute retransmission", errorRetryCounter), e);
                        }
                    }
                }
                logger.log(Level.INFO, String.format("managed-connection closing down"));
            }, "mqtt-sn-managed-connection");
            managedConnectionThread.setPriority(Thread.MIN_PRIORITY);
            managedConnectionThread.setDaemon(true);
            managedConnectionThread.start();
        }
    }

    public void resetConnection(IMqttsnContext context, Throwable t, boolean attemptRestart) {
        try {
            logger.log(Level.WARNING, String.format("connection lost at transport layer [%s]", context), t);
            disconnect(false, false);
            //attempt to restart transport
            callShutdown(registry.getTransport());
            if(attemptRestart){
                callStartup(registry.getTransport());
                if(managedConnectionThread != null){
                    try {
                        synchronized (managedConnectionThread){
                            managedConnectionThread.notify();
                        }
                    } catch(Exception e){
                        logger.log(Level.WARNING, String.format("error encountered when trying to recover from unsolicited disconnect [%s]", context, e));
                    }
                }
            }
        } catch(Exception e){
            logger.log(Level.WARNING, String.format("error encountered resetting connection [%s], current client state is [%s]", context, getSessionState()), e);
        }
    }

    private MqttsnSessionState checkSession(boolean validateConnected) throws MqttsnException {
        MqttsnSessionState state = discoverGatewaySession();
        if(validateConnected && state.getClientState() != MqttsnClientState.CONNECTED)
            throw new MqttsnRuntimeException("client not connected");
        return state;
    }

    private final void stopProcessing() throws MqttsnException {
        //-- ensure we stop message queue sending when we are not connected
        registry.getMessageStateService().unscheduleFlush(state.getContext());
        callShutdown(registry.getMessageHandler());
        callShutdown(registry.getMessageStateService());
    }

    private final void startProcessing(boolean processQueue) throws MqttsnException {

        callStartup(registry.getTransport());
        callStartup(registry.getMessageStateService());
        callStartup(registry.getMessageHandler());
        if(processQueue){
            if(managedConnection){
                activateManagedConnection();
            }
        }
    }

    private void clearState(IMqttsnContext context, boolean deepClear) throws MqttsnException {
        //-- unsolicited disconnect notify to the application
        logger.log(Level.INFO, String.format("clearing state, deep clean ? [%s]", deepClear));
        registry.getMessageStateService().clearInflight(context);
        registry.getTopicRegistry().clear(context,
                registry.getOptions().isSleepClearsRegistrations());
        if(getSessionState() != null) state.setKeepAlive(0);
        if(deepClear){
            registry.getSubscriptionRegistry().clear(context);
            registry.getTopicRegistry().clear(context, true);
        }
    }

    private Date getMessageExpiry(){
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.HOUR, +1);
        return c.getTime();
    }

    public IMqttsnSessionState getSessionState(){
        return state;
    }

    protected IMqttsnConnectionStateListener connectionListener =
            new IMqttsnConnectionStateListener() {

        @Override
        public void notifyConnected(IMqttsnContext context) {
        }

        @Override
        public void notifyRemoteDisconnect(IMqttsnContext context) {
            try {
                disconnect(false, false);
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
            resetConnection(context, t, true);
        }
    };
}