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

package org.slj.mqtt.sn.impl.ram;

import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.model.MqttsnWaitToken;
import org.slj.mqtt.sn.model.QueuedPublishMessage;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;

import java.util.*;
import java.util.logging.Level;

public class MqttsnInMemoryMessageQueue<T extends IMqttsnRuntimeRegistry>
        extends MqttsnService<T> implements IMqttsnMessageQueue<T> {

    protected Map<IMqttsnContext, Queue<QueuedPublishMessage>> queues;

    @Override
    public synchronized void start(T runtime) throws MqttsnException {
        queues = Collections.synchronizedMap(new HashMap<>());
        super.start(runtime);
    }

    @Override
    public int size(IMqttsnContext context) throws MqttsnException {
        if (queues.containsKey(context)) {
            Queue<QueuedPublishMessage> queue = getQueue(context);
            return queue.size();
        }
        return 0;
    }

    @Override
    public MqttsnWaitToken offer(IMqttsnContext context, QueuedPublishMessage message)
            throws MqttsnException, MqttsnQueueAcceptException {
        Queue<QueuedPublishMessage> queue = getQueue(context);
        if(size(context) >= getMaxQueueSize()){
            logger.log(Level.WARNING, String.format("max queue size reached for client [%s] >= [%s]", context, queue.size()));
            throw new MqttsnQueueAcceptException("max queue size reached for client");
        }
        boolean b;
        synchronized (queue){
            b = queue.offer(message);
        }

        int size = size(context);

        logger.log(MqttsnUtils.percentOf(size, getMaxQueueSize()) > 80 ? Level.WARNING : Level.FINE, String.format("offered message to queue [%s] for [%s], queue size is [%s]", b, context, queue.size()));
        MqttsnWaitToken token = MqttsnWaitToken.from(message);
        message.setToken(token);

        if(registry.getMessageStateService() != null)
            registry.getMessageStateService().scheduleFlush(context);

        return token;
    }

    @Override
    public void clear(IMqttsnContext context) throws MqttsnException {
        if (queues.containsKey(context)) {
            Queue<QueuedPublishMessage> queue = getQueue(context);
            synchronized (queue){
                queue.clear();
            }
            queues.remove(context);
            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE, String.format("clearing queue for [%s]", context));
            }
        }
    }

    @Override
    public void clearAll() throws MqttsnException {
        queues.clear();
    }

    @Override
    public Iterator<IMqttsnContext> listContexts() throws MqttsnException {
        synchronized (queues){
            return new ArrayList<>(queues.keySet()).iterator();
        }
    }

    @Override
    public QueuedPublishMessage pop(IMqttsnContext context) throws MqttsnException {
        Queue<QueuedPublishMessage> queue = getQueue(context);
        synchronized (queue){
            QueuedPublishMessage p = queue.poll();
            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE, String.format("poll form queue for [%s], queue size is [%s]", context, queue.size()));
            }
            return p;
        }
    }

    @Override
    public QueuedPublishMessage peek(IMqttsnContext context) throws MqttsnException {
        Queue<QueuedPublishMessage> queue = getQueue(context);
        synchronized (queue){
            return queue.peek();
        }
    }

    protected Queue<QueuedPublishMessage> getQueue(IMqttsnContext context){
        Queue<QueuedPublishMessage> queue = queues.get(context);
        if(queue == null){
            synchronized (this){
                if((queue = queues.get(context)) == null){
                    //-- queued message uses date for natural sort
                    queue = new PriorityQueue<>();
                    queues.put(context, queue);
                }
            }
        }
        return queue;
    }

    protected int getMaxQueueSize() {
        return registry.getOptions().getMaxMessagesInQueue();
    }
}
