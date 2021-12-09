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

package org.slj.mqtt.sn.gateway.impl.broker;

import com.google.common.util.concurrent.RateLimiter;
import org.slj.mqtt.sn.gateway.spi.PublishResult;
import org.slj.mqtt.sn.gateway.spi.Result;
import org.slj.mqtt.sn.gateway.spi.broker.IMqttsnBrokerConnection;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBrokerException;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBrokerOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * A single broker connection is maintained and used for all connecting gateway side
 * devices
 */
public class MqttsnAggregatingBrokerService extends AbstractMqttsnBrokerService {

    volatile IMqttsnBrokerConnection connection;
    volatile boolean stopped = false;
    private Thread publishingThread = null;
    private final Object monitor = new Object();
    private final Queue<PublishOp> queue = new LinkedBlockingQueue<>();
    private volatile Date lastPublishAttempt = null;
    private volatile RateLimiter rateLimiter = null;
    private static final long PUBLISH_THREAD_MAX_WAIT = 10000;
    private static final long MANAGED_CONNECTION_VALIDATION_TIME = 10000;

    public MqttsnAggregatingBrokerService(MqttsnBrokerOptions options){
        super(options);
    }

    @Override
    public void start(IMqttsnGatewayRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        rateLimiter = RateLimiter.create(((MqttsnGatewayOptions)runtime.getOptions()).
                getMaxBrokerPublishesPerSecond());
        initPublisher();
    }

    @Override
    public void stop() throws MqttsnException {
        stopped = true;
        super.stop();
        try {
            close(connection);
        } catch(MqttsnBrokerException e){
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
    public boolean isConnected(IMqttsnContext context) throws MqttsnBrokerException {
        return !stopped && connection != null && connection.isConnected();
    }

    @Override
    public PublishResult publish(IMqttsnContext context, String topicPath, int QoS, byte[] payload, boolean retain) throws MqttsnBrokerException {
        try {
            rateLimiter.acquire();
            PublishOp op = new PublishOp();
            op.data = payload;
            op.context = context;
            op.retain = retain;
            op.topicPath = topicPath;
            op.QoS = QoS;
            queue.add(op);
            logger.log(Level.INFO, String.format("queuing message for publish [%s] -> [%s] bytes, queue contains [%s]", topicPath, payload.length, queue.size()));
            synchronized (monitor){
                monitor.notifyAll();
            }
            return new PublishResult(Result.STATUS.SUCCESS,"queued for sending on publishing thread");
        } catch(Exception e){
            throw new MqttsnBrokerException(e);
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
    protected IMqttsnBrokerConnection getBrokerConnectionInternal(IMqttsnContext context) throws MqttsnBrokerException {
        if(stopped) throw new MqttsnBrokerException("broker service is in the process or shutting down");
        initConnection();
        return connection;
    }

    private void initPublisher(){
        publishingThread = new Thread(() -> {
            int errorCount = 0;
            do {
                try {
                    if(connection != null && connection.isConnected()) {
                        PublishOp op = queue.poll();
                        if(op != null){
                            logger.log(Level.INFO, String.format("dequeing message to broker from queue, [%s] remaining", queue.size()));
                            lastPublishAttempt = new Date();
                            PublishResult res = super.publish(op.context, op.topicPath, op.QoS, op.data, op.retain);
                            if(res.isError()){
                                logger.log(Level.WARNING, String.format("error pushing message, dont deque, [%s] remaining", queue.size()));
                                queue.offer(op);
                                errorCount++;
                            } else {
                                errorCount = 0;
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
        }, "mqtt-sn-broker-publisher");
        publishingThread.setDaemon(true);
        publishingThread.setPriority(Thread.MIN_PRIORITY);
        publishingThread.start();
    }

    protected void initConnection() throws MqttsnBrokerException {
        if(connection == null){
            //-- in aggregation mode connect with the gatewayId as the clientId on the broker side
            synchronized (this){
                if(connection == null){
                    connection = registry.getBrokerConnectionFactory().createConnection(options,
                            registry.getOptions().getContextId());
                    if(connection instanceof AbstractMqttsnBrokerConnection){
                        ((AbstractMqttsnBrokerConnection)connection).setBrokerService(this);
                        //-- ensure we subscribe the connection to any existing subscriptions
                        try {
                            Set<TopicPath> paths = getRuntimeRegistry().getSubscriptionRegistry().readAllSubscribedTopicPaths();
                            if(paths!= null){
                                logger.log(Level.INFO, String.format("new aggregated connection subscribing to [%s] existing topics..", paths.size()));
                                paths.forEach(path -> {
                                    try {
                                        connection.subscribe(null, path.toString(), 2);
                                    } catch (MqttsnBrokerException e) {
                                        e.printStackTrace();
                                        logger.log(Level.WARNING, "error subscribing to [%s] existing topics..", e);
                                    }
                                });
                            }
                        } catch (MqttsnException e) {
                            logger.log(Level.WARNING, "error subscribing to [%s] existing topics..", e);
                            throw new MqttsnBrokerException(e);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void close(IMqttsnBrokerConnection connection) throws MqttsnBrokerException {
        if(connection != null && connection.isConnected()){
            connection.close();
        }
        this.connection = null;
    }

    public int getQueuedCount() {
        return queue.size();
    }

    public void reinit() throws MqttsnBrokerException {
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
        return "gateway-broker-managed-connection";
    }

    static class PublishOp {
        public IMqttsnContext context;
        public String topicPath;
        public int QoS;
        public boolean retain;
        public byte[] data;
    }
}
