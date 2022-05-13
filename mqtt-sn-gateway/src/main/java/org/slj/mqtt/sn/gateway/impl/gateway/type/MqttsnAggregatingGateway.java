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

package org.slj.mqtt.sn.gateway.impl.gateway.type;

import com.google.common.util.concurrent.RateLimiter;
import org.slj.mqtt.sn.gateway.impl.backend.AbstractMqttsnBackendConnection;
import org.slj.mqtt.sn.gateway.impl.backend.AbstractMqttsnBackendService;
import org.slj.mqtt.sn.gateway.spi.PublishResult;
import org.slj.mqtt.sn.gateway.spi.Result;
import org.slj.mqtt.sn.gateway.spi.broker.IMqttsnBackendConnection;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBackendException;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBackendOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.Date;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

/**
 * A single broker connection is maintained and used for all connecting gateway side
 * devices
 */
public class MqttsnAggregatingGateway extends AbstractMqttsnBackendService {

    private volatile IMqttsnBackendConnection connection;
    private volatile boolean stopped = false;
    private Thread publishingThread = null;
    private final Object monitor = new Object();
    private final Queue<BrokerPublishOperation> queue = new LinkedBlockingQueue<>();
    private volatile Date lastPublishAttempt = null;
    private volatile RateLimiter rateLimiter = null;
    private static final long PUBLISH_THREAD_MAX_WAIT = 10000;
    private static final long MANAGED_CONNECTION_VALIDATION_TIME = 10000;

    public MqttsnAggregatingGateway(MqttsnBackendOptions options){
        super(options);
    }

    @Override
    public void start(IMqttsnGatewayRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        double limiter = ((MqttsnGatewayOptions)runtime.getOptions()).
                getMaxBrokerPublishesPerSecond();
        rateLimiter = limiter == 0d ? null : RateLimiter.create(limiter);
        connectOnStartup();
        initPublisher();
    }

    protected void connectOnStartup() throws MqttsnException{
        if(options.getConnectOnStartup()){
            logger.log(Level.INFO, "aggregating backend connecting during startup requested..");
            try {
                getBrokerConnection(null);
            } catch(MqttsnBackendException e){
                logger.log(Level.SEVERE, "encountered error attempting broker connect..", e);
                throw new MqttsnException("encountered error attempting broker connect..",e);
            }
            logger.log(Level.INFO, "connection complete, backend service ready.");
        }
    }

    @Override
    public void stop() throws MqttsnException {
        stopped = true;
        super.stop();
        try {
            close(connection);
        } catch(MqttsnBackendException e){
            logger.log(Level.WARNING, "error encountered shutting down broker connection;", e);
        } finally {
            synchronized (monitor){
                monitor.notifyAll();
            }
        }
    }

    @Override
    protected void initThread() {
        //-- only start deamon process if we are managing the connections
        if(options.getManagedConnections()){
            super.initThread();
        }
    }

    @Override
    public boolean isConnected(IMqttsnContext context) throws MqttsnBackendException {
        return !stopped && connection != null && connection.isConnected();
    }

    @Override
    public PublishResult publish(IMqttsnContext context, TopicPath topicPath, int qos, boolean retained, byte[] payload, IMqttsnMessage message) throws MqttsnBackendException {
        try {
            if(isConnected(context)){
                if(!connection.canAccept(context, topicPath, payload, message)){
                    logger.log(Level.WARNING, String.format("unable to accept publish [%s]", topicPath));
                    return new PublishResult(Result.STATUS.ERROR,
                            String.format("publisher unable to accept message on [%s]", topicPath));
                }
            }

            BrokerPublishOperation op = new BrokerPublishOperation();
            op.context = context;
            op.topicPath = topicPath;
            op.initialMessage = message;
            op.payload = payload;
            op.retained = retained;
            op.qos = qos;

            queue.add(op);
            logger.log(Level.FINE, String.format("queuing message for publish [%s], queue contains [%s]", topicPath, queue.size()));
            synchronized (monitor){
                monitor.notifyAll();
            }
            return new PublishResult(Result.STATUS.SUCCESS,"queued for sending on publishing thread");
        } catch(Exception e){
            throw new MqttsnBackendException(e);
        }
    }

    @Override
    protected long doWork() {
        try {
            if(options.getManagedConnections()){
                logger.log(Level.FINE, "checking status of managed connection..");
                if(connection != null){
                    if(!connection.isConnected()){
                        logger.log(Level.WARNING, "detected invalid connection to broker, dropping stale connection.");
                        close(connection);
                    }
                } else {
                    initConnection();
                }
            }
        } catch(Exception e){
            logger.log(Level.SEVERE, "error occurred monitoring connections;", e);
        }
        return MANAGED_CONNECTION_VALIDATION_TIME;
    }

    @Override
    protected IMqttsnBackendConnection getBrokerConnectionInternal(IMqttsnContext context) throws MqttsnBackendException {
        if(stopped) throw new MqttsnBackendException("broker service is in the process or shutting down");
        initConnection();
        return connection;
    }

    private void initPublisher(){
        publishingThread = new Thread(() -> {
            int errorCount = 0;
            do {
                try {
                    if(connection != null && connection.isConnected()) {
                        BrokerPublishOperation op = queue.poll();
                        if(op != null){
                            lastPublishAttempt = new Date();
                            if(connection.canAccept(op.context, op.topicPath, op.payload, op.initialMessage)){
                                if(rateLimiter != null) rateLimiter.acquire();
                                logger.log(Level.FINE, String.format("de-queuing message to broker from queue, [%s] remaining", queue.size()));
                                PublishResult res = super.publish(op.context, op.topicPath, op.qos, op.retained, op.payload, op.initialMessage);
                                if(res.isError()){
                                    logger.log(Level.WARNING, String.format("error pushing message, dont deque, [%s] remaining", queue.size()));
                                    queue.offer(op);
                                    errorCount++;
                                } else {
                                    errorCount = 0;
                                }
                            } else {
                                logger.log(Level.WARNING, "unable to accept publish operation from queue - discard");
                            }
                        }
                    } else {
                        errorCount++;
                    }

                    if(errorCount > 0){
                        //exponential back off to allow the connection to reestablish
                        Thread.sleep(
                                MqttsnUtils.getExponentialBackoff(errorCount, true));
                    }

                    if(running && !stopped) {
                        synchronized (monitor){
                            while(queue.peek() == null){
                                monitor.wait(PUBLISH_THREAD_MAX_WAIT);
                            }
                        }
                    }
                } catch(Exception e){
                    logger.log(Level.SEVERE, String.format("error publishing via queue publisher;"), e);
                }
            } while(running && !stopped);
        }, "mqtt-sn-backend-publisher");
        publishingThread.setDaemon(true);
        publishingThread.setPriority(Thread.MIN_PRIORITY);
        publishingThread.start();
    }

    protected void initConnection() throws MqttsnBackendException {
        if(connection == null){
            //-- in aggregation mode connect with the gatewayId as the clientId on the broker side
            synchronized (this){
                if(connection == null){
                    connection = registry.getBackendConnectionFactory().createConnection(options,
                            registry.getOptions().getContextId());
                    if(connection instanceof AbstractMqttsnBackendConnection){
                        ((AbstractMqttsnBackendConnection)connection).setBrokerService(this);
                        //-- ensure we subscribe the connection to any existing subscriptions
                        try {
                            Set<TopicPath> paths = getRuntimeRegistry().getSubscriptionRegistry().readAllSubscribedTopicPaths();
                            if(paths!= null){
                                logger.log(Level.INFO, String.format("new aggregated connection subscribing to [%s] existing topics..", paths.size()));
                                paths.forEach(path -> {
                                    try {
                                        connection.subscribe(null, path, null);
                                    } catch (MqttsnBackendException e) {
                                        e.printStackTrace();
                                        logger.log(Level.WARNING, "error subscribing to [%s] existing topics..", e);
                                    }
                                });
                            }
                        } catch (MqttsnException e) {
                            logger.log(Level.WARNING, "error subscribing to [%s] existing topics..", e);
                            throw new MqttsnBackendException(e);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void close(IMqttsnBackendConnection connection) throws MqttsnBackendException {
        if(connection != null && connection.isConnected()){
            connection.close();
        }
        this.connection = null;
    }

    public int getQueuedCount() {
        return queue.size();
    }

    public void reinit() throws MqttsnBackendException {
        if(connection != null){
            close(connection);
        }
        initConnection();
    }

    public void pokeQueue() {
        synchronized (monitor){
            monitor.notifyAll();
        }
    }

    public Date getLastPublishAttempt(){
        return lastPublishAttempt;
    }

    @Override
    protected String getDaemonName() {
        return "gateway-backend-managed-connection";
    }

    static class BrokerPublishOperation {
        public IMqttsnContext context;
        public TopicPath topicPath;
        public byte[] payload;
        public boolean retained;
        public int qos;
        public IMqttsnMessage initialMessage;
    }
}
