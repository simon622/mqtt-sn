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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.impl.metrics.IMqttsnMetrics;
import org.slj.mqtt.sn.impl.metrics.MqttsnCountingMetric;
import org.slj.mqtt.sn.impl.metrics.MqttsnSnapshotMetric;
import org.slj.mqtt.sn.model.ClientState;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.TopicPath;
import org.slj.mqtt.sn.utils.Environment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

public abstract class AbstractMqttsnRuntime implements Thread.UncaughtExceptionHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());
    protected IMqttsnRuntimeRegistry registry;

    protected final List<IMqttsnPublishReceivedListener> receivedListeners
            = new ArrayList<>();
    protected final List<IMqttsnPublishSentListener> sentListeners
            = new ArrayList<>();
    protected final List<IMqttsnPublishFailureListener> sendFailureListeners
            = new ArrayList<>();
    protected final List<IMqttsnConnectionStateListener> connectionListeners
            = new ArrayList<>();
    protected final List<IMqttsnTrafficListener> trafficListeners
            = new ArrayList<>();
    protected final List<IMqttsnService> activeServices
            = Collections.synchronizedList(new ArrayList<>());
    private volatile ThreadGroup threadGroup;
    private ExecutorService generalUseExecutorService;
    private List<ExecutorService> managedExecutorServices = new ArrayList<>();
    private CountDownLatch startupLatch;
    private long startedAt;
    private final Object monitor = new Object();
    protected volatile boolean running = false;

    public final void start(IMqttsnRuntimeRegistry reg) throws MqttsnException {
        start(reg, false);
    }

    public final void start(IMqttsnRuntimeRegistry reg, boolean join) throws MqttsnException {
        if(!running){
            startedAt = java.lang.System.currentTimeMillis();
            registry = reg;
            startupLatch = new CountDownLatch(1);
            registry.setRuntime(this);
            registry.init();

            try {
                setupEnvironment(reg.getOptions());
            } catch(IOException e){
                throw new MqttsnException("error on environment setup;", e);
            }

            logger.info("starting mqttsn-environment {}, initializing options from storage using {}",
                    java.lang.System.identityHashCode(this),
                    getRegistry().getStorageService().getClass().getSimpleName());

            registry.getStorageService().updateRuntimeOptionsFromStorage(registry.getOptions());
            registry.getOptions().processFromSystemPropertyOverrides();

            running = true;

            generalUseExecutorService =
                    createManagedExecutorService("mqtt-sn-general-purpose-thread-",
                            reg.getOptions().getGeneralPurposeThreadCount());
            bindShutdownHook();
            startupServices();
            notifyServicesStarted();
            if(registry.getMetrics() != null){
                installRuntimeMetrics();
            }
            startupLatch.countDown();
            logger.info("mqttsn-environment started successfully in {}", java.lang.System.currentTimeMillis() - startedAt);
            if(join){
                while(running){
                    synchronized (monitor){
                        try {
                            monitor.wait();
                        } catch(InterruptedException e){
                            Thread.currentThread().interrupt();
                            throw new MqttsnException(e);
                        }
                    }
                }
            }
        }
    }

    public final void stop() throws MqttsnException {
        if(running){
            logger.info("stopping mqttsn-environment {}", java.lang.System.identityHashCode(this));
            try {
                stopServices();
                notifyServicesStopped();
            } finally {
                running = false;
                try {
                    managedExecutorServices.stream().forEach(e -> closeManagedExecutorService(e));
                    if(generalUseExecutorService != null)
                        closeManagedExecutorService(generalUseExecutorService);
                } finally {
                    receivedListeners.clear();
                    sentListeners.clear();
                    sendFailureListeners.clear();
                    managedExecutorServices.clear();
                    trafficListeners.clear();
                    synchronized (monitor){
                        monitor.notifyAll();
                    }
                }
            }
        }
    }

    public void closeManagedExecutorService(ExecutorService executorService){
        try {
            if(!executorService.isShutdown()){
                executorService.shutdown();
            }
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch(InterruptedException e){
            Thread.currentThread().interrupt();
        } finally {
            if (!executorService.isTerminated()) {
                executorService.shutdownNow();
            }
        }
    }

    private void bindShutdownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread(getThreadGroup(), () -> {
            try {
                close();
            } catch(Exception e){
                logger.error("encountered error executing shutdown hook", e);
            }
        }, "mqtt-sn-finalizer"));
    }

    protected final void callStartup(IMqttsnService service) throws MqttsnException {
        if(service == null) throw new MqttsnRuntimeException("unable to start <null> service");
        if(!service.running()){
            logger.info("starting {} for runtime {}", service.getClass().getName(), java.lang.System.identityHashCode(this));
            service.start(registry);
            activeServices.add(service);
        }
    }

    protected final void callShutdown(IMqttsnService service) throws MqttsnException {
        if(service == null) throw new MqttsnRuntimeException("unable to start <null> service");
        if(service.running()){
            logger.info("stopping {} for runtime {}", service.getClass().getName(), java.lang.System.identityHashCode(this));
            service.stop();
            activeServices.remove(service);
        }
    }

    /**
     * Allow services to join the startup thread until startup is complete
     */
    public final void joinStartup() throws InterruptedException {
        startupLatch.await(60, TimeUnit.SECONDS);
    }

    public static void setupEnvironment(MqttsnOptions options) throws IOException {
        initializeLogging(options);
    }

    public static void initializeLogging(MqttsnOptions options) throws IOException {

//        if (java.lang.System.getProperty("java.util.logging.config.file") == null) {
//            LogManager.getLogManager().reset();
//            boolean applied = false;
//            try (InputStream stream = AbstractMqttsnRuntime.class.getResourceAsStream("/logging.properties")) {
//                if (null != stream) {
//                    Logger.getAnonymousLogger().log(Level.INFO, "applying logging from config found on classpath");
//                    LogManager.getLogManager().readConfiguration(stream);
//                    applied = true;
//                } else {
//                    throw new IOException("unable to read logging properties");
//                }
//            }
//            if(!applied){
//                Logger.getAnonymousLogger().log(Level.SEVERE, "unable to initialise logging, applying fallback");
//                String pattern = options.getLogPattern();
//                pattern = pattern == null ? "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %2$s %5$s%6$s%n" : pattern;
//                java.lang.System.setProperty("java.util.logging.SimpleFormatter.format", pattern);
//            }
//        }
    }

    protected final void messageReceived(IClientIdentifierContext context, TopicPath topicPath, int qos, boolean retained, byte[] data, IMqttsnMessage message){
        logger.debug("publish received by application {}, notifying {} listeners", topicPath, receivedListeners.size());
        receivedListeners.forEach(p -> p.receive(context, topicPath, qos, retained, data, message));
    }

    protected final void messageSent(IClientIdentifierContext context, TopicPath topicPath, int qos, boolean retained, byte[] data, IMqttsnMessage message){
        logger.debug("sent confirmed by application {}, notifying {} listeners", topicPath, sentListeners.size());
        sentListeners.forEach(p -> p.sent(context, topicPath, qos, retained, data, message));
    }

    protected final void messageSendFailure(IClientIdentifierContext context, TopicPath topicPath, int qos, boolean retained, byte[] data, IMqttsnMessage message, int retryCount){
        logger.debug("message failed sending {}, notifying {} listeners", topicPath, sendFailureListeners.size());
        sendFailureListeners.forEach(p -> p.sendFailure(context, topicPath, qos, retained, data, message, retryCount));
    }

    public void registerPublishReceivedListener(IMqttsnPublishReceivedListener listener) {
        if(listener == null) throw new IllegalArgumentException("cannot register <null> listener");
        if(!receivedListeners.contains(listener))
            receivedListeners.add(listener);
    }

    public void registerPublishSentListener(IMqttsnPublishSentListener listener) {
        if(listener == null) throw new IllegalArgumentException("cannot register <null> listener");
        if(!sentListeners.contains(listener))
            sentListeners.add(listener);
    }

    public boolean unregisterReceivedListener(IMqttsnPublishReceivedListener listener) {
        if(listener == null) throw new IllegalArgumentException("cannot unregister <null> listener");
        return receivedListeners.remove(listener);
    }
    public boolean unregisterSentListener(IMqttsnPublishSentListener listener) {
        if(listener == null) throw new IllegalArgumentException("cannot unregister <null> listener");
        return sentListeners.remove(listener);
    }
    public void clearSentReceiveListeners(){
        sentListeners.clear();
        receivedListeners.clear();
    }
    public void registerConnectionListener(IMqttsnConnectionStateListener listener) {
        if(listener == null) throw new IllegalArgumentException("cannot register <null> listener");
        if(!connectionListeners.contains(listener))
            connectionListeners.add(listener);
    }
    public void registerPublishFailedListener(IMqttsnPublishFailureListener listener) {
        if(listener == null) throw new IllegalArgumentException("cannot register <null> listener");
        if(!sendFailureListeners.contains(listener))
            sendFailureListeners.add(listener);
    }
    public void registerTrafficListener(IMqttsnTrafficListener listener) {
        if(listener == null) throw new IllegalArgumentException("cannot register <null> listener");
        if(!trafficListeners.contains(listener))
            trafficListeners.add(listener);
    }

    public List<IMqttsnTrafficListener> getTrafficListeners(){
        return Collections.unmodifiableList(trafficListeners);
    }


    /**
     * A Disconnect was received from the remote context
     * @param context - The context who sent the DISCONNECT
     * @return should the local runtime send a DISCONNECT in reponse
     */
    public boolean handleRemoteDisconnect(IClientIdentifierContext context){
        logger.debug("notified of remote disconnect [{} <- {}]", registry.getOptions().getContextId(), context);
        connectionListeners.forEach(p -> p.notifyRemoteDisconnect(context));
        return true;
    }

    /**
     * When the runtime reaches a condition from which it cannot recover for the context,
     * it will generate a DISCONNECT to send to the context, the exception and context are then
     * passed to this method so the application has visibility of them
     * @param context - The context whose state encountered the problem thag caused the DISCONNECT
     * @param t - the exception that was encountered
     * @return was the exception handled, if so, the trace is not thrown up to the transport layer,
     * if not, the exception is reported into the transport layer
     */
    public boolean handleLocalDisconnect(IClientIdentifierContext context, Throwable t){
        logger.debug("notified of local disconnect [{} !- {}] - {}", registry.getOptions().getContextId(), context, t.getMessage());
        connectionListeners.forEach(p -> p.notifyLocalDisconnect(context, t));
        return true;
    }

    /**
     * Reported by the transport layer when its (stateful) connection is lost. Invariably
     * this will be Socket connections over TCP IP
     * @param context - The context whose state encountered the problem thag caused the DISCONNECT
     * @param t - the exception that was encountered
     */
    public void handleConnectionLost(IClientIdentifierContext context, Throwable t){
        logger.debug("notified of connection lost [{} !- {}] - {}", registry.getOptions().getContextId(), context, t == null ? null : t.getMessage());
        connectionListeners.forEach(p -> p.notifyConnectionLost(context, t));
    }

    /**
     * Reported the when a CONNECTION is successfully established
     * @param context
     */
    public void handleConnected(IClientIdentifierContext context){
        logger.debug("notified of new connection [{} <- {}]", registry.getOptions().getContextId(), context);
        connectionListeners.forEach(p -> p.notifyConnected(context));
    }

    /**
     * Any context the gateway (or client) is communicating to will report into active timeout when
     * the last received message FROM this context exceeds to the active timeout threshold. (NOTE: this is NOT keep alive,
     * is merely to give an indication to the application that a context is not actively communicating)
     *
     * @param context - the context who hasnt been heard of since the timeout
     */
    public void handleActiveTimeout(IClientIdentifierContext context){
        logger.debug("notified of active timeout [{} <- {}]", registry.getOptions().getContextId(), context);
        connectionListeners.forEach(p -> p.notifyActiveTimeout(context));
    }

    protected ThreadFactory createManagedThreadFactory(String name, int threadPriority){
        ThreadFactory tf = new ThreadFactory() {
            volatile int count = 0;
            @Override
            public synchronized Thread newThread(Runnable r) {
                Thread t = new Thread(getThreadGroup(), r, name + ++count);
                t.setPriority(Math.max(1, Math.min(threadPriority, Thread.MAX_PRIORITY)));
                t.setUncaughtExceptionHandler(AbstractMqttsnRuntime.this);
                return t;
            }
        };
        return tf;
    }

    /**
     * Create a centrally managed executor service with managed groups and backpressure
     */
    public synchronized ExecutorService createManagedExecutorService(String name, int threadCount){

        int max = Math.max(1, threadCount);
        int core = max / 2;
        BlockingQueue<Runnable> linkedBlockingDeque
                = new LinkedBlockingDeque<>(registry.getOptions().getQueueBackPressure());
        ExecutorService executorService = new ThreadPoolExecutor(core, max, 30,
                TimeUnit.SECONDS, linkedBlockingDeque,
                createManagedThreadFactory(name, Thread.MIN_PRIORITY + 1),
                new ThreadPoolExecutor.CallerRunsPolicy());

        managedExecutorServices.add(executorService);
        return executorService;
    }

    /**
     * Create a centrally managed scheduled executor service with managed group
     */
    public synchronized ScheduledExecutorService createManagedScheduledExecutorService(String name, int threadCount){

        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(Math.max(1, threadCount),
                createManagedThreadFactory(name, Thread.NORM_PRIORITY),
                new ThreadPoolExecutor.CallerRunsPolicy());//DiscardPolicy());
        managedExecutorServices.add(executorService);
        return executorService;
    }

    public <T> Future<T> submit(ExecutorService executorService, Runnable task, T result){
        if(running) {
            return executorService.submit(task, result);
        }
        else {
            throw new MqttsnRuntimeException("runtime is not active");
        }
    }

    public void submit(ExecutorService executorService, Runnable task){
        if(running){
            executorService.submit(task);
        } else {
            throw new MqttsnRuntimeException("runtime is not active");
        }
    }

    public void generalPurposeSubmit(Runnable r){
        submit(generalUseExecutorService, r);
    }

    /**
     * @return - The thread group for this runtime
     */
    public ThreadGroup getThreadGroup(){
        if(threadGroup == null){
            synchronized (this){
                if(threadGroup == null)
                    threadGroup = new ThreadGroup("mqtt-sn");
            }
        }
        return threadGroup;
    }

    public String getUserAgent() {
        //todo get the build to compile in constant VERSION from build system
        return getClass().getCanonicalName();
    }

    public IMqttsnRuntimeRegistry getRegistry(){
        if(registry == null)
            throw new NullPointerException("registry is <null> on runtime");
        return registry;
    }

    protected void installRuntimeMetrics(){

        //-- snapshot metrics are self-managed
        registry.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SYSTEM_VM_MEMORY_USED, "The amount of memory available to the virtual machine (kb).",
                IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> Environment.getUsedMemoryKb()));
        registry.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SYSTEM_VM_THREADS_USED, "The number of threads in the virtual machine.",
                IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> Environment.getThreadCount()));
        registry.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.NETWORK_REGISTRY_COUNT, "The number of entries in the network registry.",
                IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> registry.getNetworkRegistry().size()));
        registry.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.MESSAGE_REGISTRY_COUNT, "The number of messages residing in the application message data store.",
                IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SAMPLES_TIME_MILLIS, () -> registry.getMessageRegistry().size()));
        registry.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.DLQ_REGISTRY_COUNT, "The number of messages residing in the dead letter queue.",
                IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SAMPLES_TIME_MILLIS, () -> registry.getDeadLetterQueue().size()));

        //-- these require managing externally
        registry.getMetrics().registerMetric(new MqttsnCountingMetric(IMqttsnMetrics.PUBLISH_MESSAGE_IN, "The number of mqtt-sn publish messages received (ingress) in the time period.",
                IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SAMPLES_TIME_MILLIS));
        registry.getMetrics().registerMetric(new MqttsnCountingMetric(IMqttsnMetrics.PUBLISH_MESSAGE_OUT, "The number of mqtt-sn publish messages sent (egress) in the time period.",
                IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SAMPLES_TIME_MILLIS));

        registry.getMetrics().registerMetric(new MqttsnCountingMetric(IMqttsnMetrics.NETWORK_BYTES_IN, "The number of network bytes received (ingress) in the time period.",
                IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SAMPLES_TIME_MILLIS));
        registry.getMetrics().registerMetric(new MqttsnCountingMetric(IMqttsnMetrics.NETWORK_BYTES_OUT, "The number of network bytes sent (egress) in the time period.",
                IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SAMPLES_TIME_MILLIS));

        registerPublishReceivedListener((context, topicPath, qos, retained, data, message) ->
                registry.getMetrics().getMetric(IMqttsnMetrics.PUBLISH_MESSAGE_IN).increment(1));

        registerPublishSentListener((context, topicPath, qos, retained, data, message) ->
                registry.getMetrics().getMetric(IMqttsnMetrics.PUBLISH_MESSAGE_OUT).increment(1));
        registerTrafficListener(new IMqttsnTrafficListener() {
            @Override
            public void trafficSent(INetworkContext context, byte[] data, IMqttsnMessage message) {
                registry.getMetrics().getMetric(IMqttsnMetrics.NETWORK_BYTES_OUT).increment(data.length);
            }

            @Override
            public void trafficReceived(INetworkContext context, byte[] data, IMqttsnMessage message) {
                registry.getMetrics().getMetric(IMqttsnMetrics.NETWORK_BYTES_IN).increment(data.length);
            }
        });
    }

    private final synchronized void startupServices()
            throws MqttsnException {

        Iterator<IMqttsnService> services =
                getRegistry().getServices().iterator();
        while(services.hasNext()){
            try {
                IMqttsnService s = services.next();
                callStartup(s);
            } catch(MqttsnException e){
                logger.error("error starting service;", e);
                throw new MqttsnException(e);
            }
        }
    }

    private final synchronized void stopServices()
            throws MqttsnException {

        Iterator<IMqttsnService> services =
                getRegistry().getServices().iterator();
        while(services.hasNext()){
            try {
                IMqttsnService s = services.next();
                callShutdown(s);
            } catch(MqttsnException e){
                logger.error("error starting service;", e);
                throw new MqttsnException(e);
            }
        }
    }


    /**
     * Optionally override this hook method to bootstrap logic into the managed lifecycle;
     * called AFTER all services have been started
     */
    protected void notifyServicesStarted(){

    }

    /**
     * Optionally override this hook method to bootstrap logic into the managed lifecycle;
     * called AFTER all services have been stopped
     */
    protected void notifyServicesStopped(){

    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        logger.error("uncaught error in thread-pool", t);
    }

    private void notifySessions(){
        try {
            if(getRegistry().getSessionRegistry().countTotalSessions() > 0){
                Iterator<ISession> sessionIterator = getRegistry().getSessionRegistry().iterator();
                while(sessionIterator.hasNext()){
                    ISession session = sessionIterator.next();
                    if(session.getClientState() != ClientState.LOST){
                        //notify all sessions of going away exception LOST sessions
                        try {
                            logger.warn("notifying {} is going away", session);
                            IMqttsnMessage disconnect = getRegistry().getMessageFactory().createDisconnect(
                                    MqttsnConstants.RETURN_CODE_SERVER_UNAVAILABLE, "Gateway going away");
                            getRegistry().getTransportLocator().writeToTransport(
                                    getRegistry().getNetworkRegistry().getContext(session.getContext()), disconnect);
                        } catch(Exception e){
                            logger.warn("unable to send disconnect {}", e.getMessage());
                        }
                    }
                }
            }
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            notifySessions();
        } catch(Exception e){
            throw new RuntimeException(e);
        } finally {
            try {
                stop();
            } catch (MqttsnException e) {
            }
        }
    }
}
