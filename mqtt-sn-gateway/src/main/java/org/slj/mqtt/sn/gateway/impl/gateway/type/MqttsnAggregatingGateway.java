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

import org.slj.mqtt.sn.cloud.MqttsnConnectorDescriptor;
import org.slj.mqtt.sn.gateway.impl.backend.AbstractMqttsnBackendConnection;
import org.slj.mqtt.sn.gateway.impl.backend.AbstractMqttsnBackendService;
import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.gateway.spi.connector.IMqttsnConnectorConnection;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorException;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.impl.metrics.IMqttsnMetrics;
import org.slj.mqtt.sn.impl.metrics.MqttsnCountingMetric;
import org.slj.mqtt.sn.impl.metrics.MqttsnSnapshotMetric;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnIllegalFormatException;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A single broker connection is maintained and used for all connecting gateway side
 * devices
 */
public class MqttsnAggregatingGateway extends AbstractMqttsnBackendService {

    private volatile IMqttsnConnectorConnection connection;
    private volatile boolean stopped = false;
    private Thread publishingThread = null;
    private final Object monitor = new Object();
    private final Queue<BrokerPublishOperation> queue = new LinkedBlockingQueue<>();
//    private volatile RateLimiter rateLimiter = null;
    private static final long PUBLISH_THREAD_MAX_WAIT = 10000;
    private static final long MANAGED_CONNECTION_VALIDATION_TIME = 10000;
    private static final long MAX_ERROR_RETRIES = 5;
    private volatile boolean metricsLoaded = false;

    public MqttsnAggregatingGateway(){
    }

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        if(!running){
            super.start(runtime);
            double limiter = ((MqttsnGatewayOptions)runtime.getOptions()).
                    getMaxBrokerPublishesPerSecond();
//            rateLimiter = limiter == 0d ? null : RateLimiter.create(limiter);
            stopped = false;
            connectOnStartup();
            initPublisher();
        }
    }

    @Override
    public synchronized boolean initializeConnector(MqttsnConnectorDescriptor descriptor, MqttsnConnectorOptions options) throws MqttsnException {
        int qps = descriptor.getRateLimit();
        if(qps > 0){
//            rateLimiter = RateLimiter.create(qps);
            logger.warn("re-initialising connector rate-limiter with {} permits", qps);
        } else {
//            rateLimiter = null;
        }
        return super.initializeConnector(descriptor, options);
    }

    @Override
    public void stop() throws MqttsnException {
        if(!stopped){
            stopped = true;
            super.stop();
            try {
                close(connection);
            } catch(MqttsnConnectorException e){
                logger.warn("error encountered shutting down broker connector;", e);
            } finally {
                synchronized (monitor){
                    monitor.notifyAll();
                }
            }
        }
    }

    protected void connectOnStartup() throws MqttsnException{

        logger.info("aggregating backend connecting during startup requested..");
        try {
            getConnection(null);
        } catch(MqttsnConnectorException e){
            logger.error("encountered error attempting connect..", e);
            throw new MqttsnException("encountered error attempting connect..",e);
        }

        if(registry.getMetrics() != null && !metricsLoaded){

            registry.getMetrics().registerMetric(new MqttsnCountingMetric(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH,
                    "The number of mqtt application messages published through the backend connector.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SAMPLES_TIME_MILLIS));
            registry.getMetrics().registerMetric(new MqttsnCountingMetric(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH_ERROR,
                    "The number of errors received during mqtt application messages published through the backend connector.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SAMPLES_TIME_MILLIS));
            registry.getMetrics().registerMetric(new MqttsnCountingMetric(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH_RECEIVE,
                    "The number of mqtt application messages received through the backend connector.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SAMPLES_TIME_MILLIS));
            registry.getMetrics().registerMetric(new MqttsnCountingMetric(GatewayMetrics.BACKEND_CONNECTOR_EXPANSION,
                    "The number of mqtt application messages received into sessions.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SAMPLES_TIME_MILLIS));
            registry.getMetrics().registerMetric(new MqttsnSnapshotMetric(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH_QUEUE_SIZE,
                    "The number of mqtt application messages waiting to be published to the backend.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SAMPLES_TIME_MILLIS, () -> queue.size()));
            metricsLoaded = true;
        }
    }

    @Override
    protected void initThread() {
        //-- only start deamon process if we are managing the connections
        super.initThread();
    }

    @Override
    public boolean isConnected(IClientIdentifierContext context) throws MqttsnConnectorException {
        return !stopped && connection != null && connection.isConnected();
    }

    @Override
    public PublishResult publish(IClientIdentifierContext context, TopicPath topicPath, int qos, boolean retained, byte[] payload, IMqttsnMessage message) throws MqttsnConnectorException {
        try {
            if(isConnected(context)){
                if(!connection.canAccept(context, topicPath, payload, message)){
                    logger.warn("unable to accept publish {}", topicPath);
                    return new PublishResult(Result.STATUS.ERROR,
                            String.format("publisher unable to accept message on {}", topicPath));
                }
            }

            if(queue.size() >= ((MqttsnGatewayOptions) registry.getOptions()).getMaxBackendQueueSize()){
                logger.warn("queuing message for publish {} failed, backend queue at capacity {}", topicPath, queue.size());
                return new PublishResult(Result.STATUS.ERROR,"backend queue is full.");
            } else {
                BrokerPublishOperation op = new BrokerPublishOperation();
                op.context = context;
                op.topicPath = topicPath;
                op.initialMessage = message;
                op.payload = payload;
                op.retained = retained;
                op.qos = qos;
                queue.add(op);

                logger.debug("queuing message for publish {}, queue contains {}", topicPath, queue.size());

                synchronized (monitor){
                    monitor.notifyAll();
                }
                return new PublishResult(Result.STATUS.SUCCESS,"queued for sending on publishing thread");
            }

        } catch(Exception e){
            throw new MqttsnConnectorException(e);
        }
    }

    @Override
    protected long doWork() {
        try {
            if(running){
                logger.debug("checking status of managed connection..");
                if(connection != null){
                    if(!connection.isConnected()){
                        logger.warn("detected invalid connection to broker, dropping stale connection.");
                        close(connection);
                    }
                } else {
                    initConnection();
                }
            }
        } catch(Exception e){
            logger.error("error occurred monitoring connections;", e);
        }
        return MANAGED_CONNECTION_VALIDATION_TIME;
    }

    @Override
    protected IMqttsnConnectorConnection getConnectionInternal(IClientIdentifierContext context) throws MqttsnConnectorException {
        if(stopped) throw new MqttsnConnectorException("broker service is in the process or shutting down");
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
                            if(connection.canAccept(op.context, op.topicPath, op.payload, op.initialMessage)){
//                                if(rateLimiter != null) rateLimiter.acquire();
                                logger.debug("de-queuing message to broker from queue, {} remaining", queue.size());
                                PublishResult res = super.publish(op.context, op.topicPath, op.qos, op.retained, op.payload, op.initialMessage);
                                if(res.isError()){
                                    if(++errorCount < MAX_ERROR_RETRIES){
                                        logger.warn("error sending message to backend, {} - requeue", queue.size());
                                        queue.offer(op);
                                    }
                                    else {
                                        logger.warn("error sending message to backend, retries exhausted - discard");
                                        getRegistry().getMetrics().getMetric(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH_ERROR).increment(1);
                                        errorCount = 0;
                                    }
                                } else {
                                    errorCount = 0;
                                    getRegistry().getMetrics().getMetric(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH).increment(1);
                                }
                            } else {
                                logger.warn("unable to accept publish operation from queue - discard");
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
                            while(running && queue.peek() == null){
                                monitor.wait(PUBLISH_THREAD_MAX_WAIT);
                            }
                        }
                    }
                }
                catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    logger.warn("backend publishing thread interrupted;");
                }
                catch(Exception e){
                    logger.error("error publishing via queue publisher;", e);
                }
            } while(running && !stopped);
        }, "mqtt-sn-backend-publisher");
        publishingThread.setDaemon(true);
        publishingThread.setPriority(Thread.MIN_PRIORITY);
        publishingThread.start();
    }

    protected void initConnection() throws MqttsnConnectorException {
        if(connection == null){
            //-- in aggregation mode connect with the gatewayId as the clientId on the broker side
            synchronized (this){
                if(connection == null){
                    //-- no need for custom options per connection in aggregating mode
                    connection = getRegistry().getConnector().createConnection(registry.getOptions().getContextId());
                    if(connection instanceof AbstractMqttsnBackendConnection){
                        ((AbstractMqttsnBackendConnection)connection).setBrokerService(this);
                        //-- ensure we subscribe the connection to any existing subscriptions
                        try {
                            Set<String> paths = getRegistry().getSubscriptionRegistry().readAllSubscribedTopicPaths();
                            if(paths!= null){
                                logger.info("new aggregated connection subscribing to {} existing topics..", paths.size());
                                paths.forEach(path -> {
                                    try {
                                        connection.subscribe(null, new TopicPath(path), null);
                                    } catch (MqttsnConnectorException e) {
                                        e.printStackTrace();
                                        logger.warn("error subscribing to {} existing topics..", e);
                                    }
                                });
                            }
                        } catch (MqttsnException e) {
                            logger.warn("error subscribing to {} existing topics..", e);
                            throw new MqttsnConnectorException(e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public SubscribeResult subscribe(IClientIdentifierContext context, TopicPath topic, IMqttsnMessage message) throws MqttsnConnectorException {

        try {
            if(!getRegistry().getSubscriptionRegistry().
                    hasSubscription(topic.toString())) {
                return super.subscribe(context, topic, message);
            } else {
                logger.info("subscription already existed, not need to subscribe again");
                return new SubscribeResult(Result.STATUS.NOOP);
            }
        } catch(MqttsnIllegalFormatException | MqttsnException e){
            throw new MqttsnConnectorException(e);
        }
    }

    @Override
    public UnsubscribeResult unsubscribe(IClientIdentifierContext context, TopicPath topic, IMqttsnMessage message) throws MqttsnConnectorException {
        try {
            //-- only need to unsubscribe if we will NO LONGER have any subscriptions
            if(getRegistry().getSubscriptionRegistry().
                    hasSubscription(topic.toString())) {
                return super.unsubscribe(context, topic, message);
            } else {
                logger.info("more subscriptions existed, do not unsubscribe");
                return new UnsubscribeResult(Result.STATUS.NOOP);
            }
        } catch(MqttsnIllegalFormatException | MqttsnException e){
            throw new MqttsnConnectorException(e);
        }

    }

    @Override
    protected void close(IMqttsnConnectorConnection connection) throws MqttsnConnectorException {
        if(connection != null && connection.isConnected()){
            connection.close();
        }
        this.connection = null;
    }

    public int getQueuedCount() {
        return queue.size();
    }

    public void reinit() throws MqttsnConnectorException {
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

    @Override
    protected String getDaemonName() {
        return "gateway-backend-managed-connector";
    }

    static class BrokerPublishOperation {
        public IClientIdentifierContext context;
        public TopicPath topicPath;
        public byte[] payload;
        public boolean retained;
        public int qos;
        public IMqttsnMessage initialMessage;
    }
}
