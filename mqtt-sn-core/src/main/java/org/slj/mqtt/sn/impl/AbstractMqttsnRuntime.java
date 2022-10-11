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

import org.slj.mqtt.sn.impl.metrics.IMqttsnMetrics;
import org.slj.mqtt.sn.impl.metrics.MqttsnCountingMetric;
import org.slj.mqtt.sn.impl.metrics.MqttsnSnapshotMetric;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.SystemUtils;
import org.slj.mqtt.sn.utils.TopicPath;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public abstract class AbstractMqttsnRuntime {

    protected final Logger logger = Logger.getLogger(getClass().getName());
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
            setupEnvironment(reg.getOptions());
            startedAt = System.currentTimeMillis();
            registry = reg;
            startupLatch = new CountDownLatch(1);
            running = true;
            registry.setRuntime(this);
            registry.init();
            if(reg.getTrafficListeners() != null && !reg.getTrafficListeners().isEmpty()){
                trafficListeners.addAll(reg.getTrafficListeners());
            }
            generalUseExecutorService = createManagedExecutorService("mqtt-sn-general-purpose-thread-", reg.getOptions().getGeneralPurposeThreadCount());
            bindShutdownHook();
            logger.log(Level.INFO, String.format("starting mqttsn-environment [%s]", System.identityHashCode(this)));
            startupServices(registry);
            postStartupTasks();
            startupLatch.countDown();
            logger.log(Level.INFO, String.format("mqttsn-environment started successfully in [%s]", System.currentTimeMillis() - startedAt));
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
            logger.log(Level.INFO, String.format("stopping mqttsn-environment [%s]", System.identityHashCode(this)));
            try {
                stopServices(registry);
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
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        } catch(InterruptedException e){
            Thread.currentThread().interrupt();
        } finally {
            if (!executorService.isTerminated()) {
                executorService.shutdownNow();
            }
        }
    }

    protected void bindShutdownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread(getThreadGroup(), () -> {
            try {
                AbstractMqttsnRuntime.this.stop();
            } catch(Exception e){
                logger.log(Level.SEVERE, "encountered error executing shutdown hook", e);
            }
        }, "mqtt-sn-finalizer"));
    }

    protected final void callStartup(Object service) throws MqttsnException {
        if(service instanceof IMqttsnService){
            IMqttsnService snService =  (IMqttsnService) service;
            if(!snService.running()){
                if(logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, String.format("starting [%s] for runtime (%s)", service.getClass().getName(), System.identityHashCode(this)));
                }
                snService.start(registry);
                activeServices.add(snService);
            }
        }
    }

    protected final void callShutdown(Object service) throws MqttsnException {
        if(service instanceof IMqttsnService){
            IMqttsnService snService =  (IMqttsnService) service;
            if(snService.running()){
                if(logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, String.format("stopping [%s] for runtime (%s)", service.getClass().getName(), System.identityHashCode(this)));
                }
                snService.stop();
                activeServices.remove(snService);
            }
        }
    }

    /**
     * Allow services to join the startup thread until startup is complete
     */
    public final void joinStartup() throws InterruptedException {
        startupLatch.await(60, TimeUnit.SECONDS);
    }

    public static void setupEnvironment(MqttsnOptions options){

        initializeLogging(options);
    }

    public static void initializeLogging(MqttsnOptions options) {
        if (System.getProperty("java.util.logging.config.file") == null) {
            try (InputStream stream = AbstractMqttsnRuntime.class.getResourceAsStream("/logging.properties")) {
                if (null != stream) {
                    LogManager.getLogManager().reset();
                    LogManager.getLogManager().readConfiguration(stream);
                    Logger.getAnonymousLogger().log(Level.INFO, "applying logging config from resource");
                } else {
                    Logger.getAnonymousLogger().log(Level.SEVERE, "unable to initialise logging, applying fallback");
                    String pattern = options.getLogPattern();
                    pattern = pattern == null ? "[%1$tc] %4$s %2$s - %5$s %6$s%n" : pattern;
                    System.setProperty("java.util.logging.SimpleFormatter.format", pattern);
                }
            } catch (IOException e) {
                // ignored for now
                e.printStackTrace();
            }
        }
    }

    protected final void messageReceived(IMqttsnContext context, TopicPath topicPath, int qos, boolean retained, byte[] data, IMqttsnMessage message){
        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, String.format("publish received by application [%s], notifying [%s] listeners", topicPath, receivedListeners.size()));
        }
        receivedListeners.forEach(p -> p.receive(context, topicPath, qos, retained, data, message));
    }

    protected final void messageSent(IMqttsnContext context, UUID messageId, TopicPath topicPath, int qos, boolean retained, byte[] data, IMqttsnMessage message){
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, String.format("sent confirmed by application [%s], notifying [%s] listeners", topicPath, sentListeners.size()));
        }
        sentListeners.forEach(p -> p.sent(context, messageId, topicPath, qos, retained, data, message));
    }

    protected final void messageSendFailure(IMqttsnContext context, UUID messageId, TopicPath topicPath, int qos, boolean retained, byte[] data, IMqttsnMessage message, int retryCount){
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, String.format("message failed sending [%s], notifying [%s] listeners", topicPath, sendFailureListeners.size()));
        }
        sendFailureListeners.forEach(p -> p.sendFailure(context, messageId, topicPath, qos, retained, data, message, retryCount));
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
        return trafficListeners;
    }


    /**
     * A Disconnect was received from the remote context
     * @param context - The context who sent the DISCONNECT
     * @return should the local runtime send a DISCONNECT in reponse
     */
    public boolean handleRemoteDisconnect(IMqttsnContext context){
        logger.log(Level.FINE, String.format("notified of remote disconnect [%s <- %s]", registry.getOptions().getContextId(), context));
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
    public boolean handleLocalDisconnect(IMqttsnContext context, Throwable t){
        logger.log(Level.FINE, String.format("notified of local disconnect [%s !- %s]", registry.getOptions().getContextId(), context), t);
        connectionListeners.forEach(p -> p.notifyLocalDisconnect(context, t));
        return true;
    }

    /**
     * Reported by the transport layer when its (stateful) connection is lost. Invariably
     * this will be Socket connections over TCP IP
     * @param context - The context whose state encountered the problem thag caused the DISCONNECT
     * @param t - the exception that was encountered
     */
    public void handleConnectionLost(IMqttsnContext context, Throwable t){
        logger.log(Level.FINE, String.format("notified of connection lost [%s !- %s]", registry.getOptions().getContextId(), context), t);
        connectionListeners.forEach(p -> p.notifyConnectionLost(context, t));
    }

    /**
     * Reported the when a CONNECTION is successfully established
     * @param context
     */
    public void handleConnected(IMqttsnContext context){
        logger.log(Level.FINE, String.format("notified of new connection [%s <- %s]", registry.getOptions().getContextId(), context));
        connectionListeners.forEach(p -> p.notifyConnected(context));
    }

    /**
     * Any context the gateway (or client) is communicating to will report into active timeout when
     * the last received message FROM this context exceeds to the active timeout threshold. (NOTE: this is NOT keep alive,
     * is merely to give an indication to the application that a context is not actively communicating)
     *
     * @param context - the context who hasnt been heard of since the timeout
     */
    public void handleActiveTimeout(IMqttsnContext context){
        logger.log(Level.FINE, String.format("notified of active timeout [%s <- %s]", registry.getOptions().getContextId(), context));
        connectionListeners.forEach(p -> p.notifyActiveTimeout(context));
    }

    protected ThreadFactory createManagedThreadFactory(String name, int threadPriority){
        return new ThreadFactory() {
            volatile int count = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(getThreadGroup(), r, name + ++count);
                t.setPriority(Math.max(1, Math.min(threadPriority, Thread.MAX_PRIORITY)));
                return t;
            }
        };
    }

    /**
     * Create a centrally managed executor service with managed groups and backpressure
     */
    public synchronized ExecutorService createManagedExecutorService(String name, int threadCount){

        BlockingQueue<Runnable> linkedBlockingDeque
                = new LinkedBlockingDeque<>(registry.getOptions().getQueueBackPressure());
        ExecutorService executorService = new ThreadPoolExecutor(1, Math.max(1, threadCount), 30,
                TimeUnit.SECONDS, linkedBlockingDeque, createManagedThreadFactory(name, Thread.MIN_PRIORITY + 1),
                new ThreadPoolExecutor.CallerRunsPolicy());
        managedExecutorServices.add(executorService);
        return executorService;
    }

    /**
     * Create a centrally managed scheduled executor service with managed group
     */
    public synchronized ScheduledExecutorService createManagedScheduledExecutorService(String name, int threadCount){

        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(Math.max(1, threadCount),
                createManagedThreadFactory(name, Thread.MIN_PRIORITY + 4),
                new ThreadPoolExecutor.CallerRunsPolicy());//DiscardPolicy());
        managedExecutorServices.add(executorService);
        return executorService;
    }

    public <T> Future<T> async(ExecutorService executorService, Runnable r, T result){
        return running ? executorService.submit(r, result) : null;
    }

    public void async(ExecutorService executorService, Runnable r){
        if(running) executorService.submit(r);
    }

    public void asyncWithCallback(ExecutorService executorService, Runnable r, Runnable callback){
        executorService.submit(() -> {
            try {
                r.run();
            } finally {
                callback.run();
            }
        });
    }

    /**
     * Submit work for the main worker thread group, this could be
     * transport operations or confirmations etc.
     */
    public <T> Future<T> async(Runnable r, T result){
        return async(generalUseExecutorService, r, result);
    }

    public void async(Runnable r){
        async(generalUseExecutorService, r);
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

    protected void postStartupTasks(){
        if(registry.getMetrics() != null){

            //-- snapshot metrics are self-managed
            registry.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SYSTEM_VM_MEMORY_USED, "The amount of memory available to the virtual machine (kb).",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> SystemUtils.getUsedMemoryKb()));
            registry.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SYSTEM_VM_THREADS_USED, "The number of threads in the virtual machine.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> SystemUtils.getThreadCount()));
            registry.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.NETWORK_REGISTRY_COUNT, "The number of entries in the network registry.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> registry.getNetworkRegistry().size()));

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

            registerPublishSentListener((context, messageId, topicPath, qos, retained, data, message) ->
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
    }

    public abstract void close() throws IOException ;

    protected abstract void startupServices(IMqttsnRuntimeRegistry runtime) throws MqttsnException;

    protected abstract void stopServices(IMqttsnRuntimeRegistry runtime) throws MqttsnException;
}