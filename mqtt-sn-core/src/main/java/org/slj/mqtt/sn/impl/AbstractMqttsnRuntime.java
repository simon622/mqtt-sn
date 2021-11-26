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

import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.utils.StringTable;
import org.slj.mqtt.sn.utils.StringTableWriters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractMqttsnRuntime {

    protected Logger logger = Logger.getLogger(getClass().getName());
    protected IMqttsnRuntimeRegistry registry;

    protected List<IMqttsnPublishReceivedListener> receivedListeners
            = new ArrayList<>();
    protected List<IMqttsnPublishSentListener> sentListeners
            = new ArrayList<>();
    protected List<IMqttsnPublishFailureListener> sendFailureListeners
            = new ArrayList<>();
    protected List<IMqttsnConnectionStateListener> connectionListeners
            = new ArrayList<>();

    protected List<IMqttsnService> activeServices
            = Collections.synchronizedList(new ArrayList<>());

    private volatile ThreadGroup threadGroup;
    protected ExecutorService executorService;
    protected CountDownLatch startupLatch;
    protected volatile boolean running = false;
    protected long startedAt;
    private final Object monitor = new Object();

    public final void start(IMqttsnRuntimeRegistry reg) throws MqttsnException {
        start(reg, false);
    }

    public final void start(IMqttsnRuntimeRegistry reg, boolean join) throws MqttsnException {
        if(!running){
            executorService = createExecutorService(reg.getOptions());
            startedAt = System.currentTimeMillis();
            setupEnvironment(reg.getOptions());
            registry = reg;
            startupLatch = new CountDownLatch(1);
            running = true;
            registry.setRuntime(this);
            registry.init();
            bindShutdownHook();
            logger.log(Level.INFO, "starting mqttsn-environment..");
            startupServices(registry);
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
            logger.log(Level.INFO, "stopping mqttsn-environment..");
            stopServices(registry);
            running = false;
            receivedListeners.clear();
            sentListeners.clear();
            sendFailureListeners.clear();
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
            synchronized (monitor){
                monitor.notifyAll();
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
                logger.log(Level.INFO, String.format("starting [%s]", service.getClass().getName()));
                snService.start(registry);
                activeServices.add(snService);
            }
        }
    }

    protected final void callShutdown(Object service) throws MqttsnException {
        if(service instanceof IMqttsnService){
            IMqttsnService snService =  (IMqttsnService) service;
            if(snService.running()){
                logger.log(Level.INFO, String.format("stopping [%s]", service.getClass().getName()));
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
        if(options.getLogPattern() != null){
            System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tc] %4$s %2$s - %5$s %6$s%n");
        }
    }

    protected final void messageReceived(IMqttsnContext context, String topicName, int QoS, byte[] payload){
        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, String.format("publish received by application [%s], notifying [%s] listeners", topicName, receivedListeners.size()));
        }
        receivedListeners.forEach(p -> p.receive(context, topicName, QoS, payload));
    }

    protected final void messageSent(IMqttsnContext context, UUID messageId, String topicName, int QoS, byte[] payload){
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, String.format("sent confirmed by application [%s], notifying [%s] listeners", topicName, sentListeners.size()));
        }
        sentListeners.forEach(p -> p.sent(context, messageId, topicName, QoS, payload));
    }

    protected final void messageSendFailure(IMqttsnContext context, UUID messageId, String topicName, int QoS, byte[] payload, int retryCount){
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, String.format("message failed sending [%s], notifying [%s] listeners", topicName, sendFailureListeners.size()));
        }
        sendFailureListeners.forEach(p -> p.sendFailure(context, messageId, topicName, QoS, payload, retryCount));
    }

    public void registerReceivedListener(IMqttsnPublishReceivedListener listener) {
        if(listener == null) throw new IllegalArgumentException("cannot register <null> listener");
        if(!receivedListeners.contains(listener))
            receivedListeners.add(listener);
    }

    public void registerSentListener(IMqttsnPublishSentListener listener) {
        if(listener == null) throw new IllegalArgumentException("cannot register <null> listener");
        if(!sentListeners.contains(listener))
            sentListeners.add(listener);
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


    /**
     * A Disconnect was received from the remote context
     * @param context - The context who sent the DISCONNECT
     * @return should the local runtime send a DISCONNECT in reponse
     */
    public boolean handleRemoteDisconnect(IMqttsnContext context){
        logger.log(Level.INFO, String.format("notified of remote disconnect [%s]", context));
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
        logger.log(Level.INFO, String.format("notified of local disconnect [%s]", context), t);
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
        logger.log(Level.INFO, String.format("notified of connection lost [%s]", context), t);
        connectionListeners.forEach(p -> p.notifyConnectionLost(context, t));
    }

    /**
     * Reported the when a CONNECTION is successfully established
     * @param context
     */
    public void handleConnected(IMqttsnContext context){
        logger.log(Level.INFO, String.format("notified of new connection [%s]", context));
        connectionListeners.forEach(p -> p.notifyConnected(context));
    }

    /**
     * Reported the when a CONNECTION is successfully established
     * @param context
     */
    public void handleActiveTimeout(IMqttsnContext context){
        logger.log(Level.INFO, String.format("notified of active timeout [%s]", context));
        connectionListeners.forEach(p -> p.notifyActiveTimeout(context));
    }

    /**
     * Hook method, create the intial async executor service which will be used for handoff from the
     * transport layer into the application
     */
    protected ExecutorService createExecutorService(MqttsnOptions options){
        int threadCount = options.getHandoffThreadCount();
        return Executors.newFixedThreadPool(threadCount, new ThreadFactory() {
            int count = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(getThreadGroup(), r, "mqtt-sn-worker-thread-" + ++count);
                t.setPriority(Thread.MIN_PRIORITY + 1);
                t.setDaemon(true);
                return t;
            }
        });
    }

    /**
     * Submit work for the main worker thread group, this could be
     * transport operations or confirmations etc.
     */
    public Future<?> async(Runnable r){
        return executorService.submit(r);
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

    public abstract void close() throws IOException ;

    protected abstract void startupServices(IMqttsnRuntimeRegistry runtime) throws MqttsnException;

    protected abstract void stopServices(IMqttsnRuntimeRegistry runtime) throws MqttsnException;
}