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

package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.model.MqttsnWaitToken;
import org.slj.mqtt.sn.model.QueuedPublishMessage;

import java.util.Iterator;
import java.util.List;

/**
 * Queue implementation to store messages destined to and from gateways and clients. Queues will be flushed acccording
 * to the session semantics defined during CONNECT.
 *
 * Ideally the queue should be implemented to support FIFO where possible.
 */
public interface IMqttsnMessageQueue<T extends IMqttsnRuntimeRegistry> extends IMqttsnRegistry<T> {

    /**
     * List the contexts which are present within this queue. Each context represents a client or gateway Queue of which
     * there will be many when run in gateway mode.
     *
     * @return - An iterator over the contexts for which a queue is presently held
     * @throws MqttsnException - an error occurred
     */
    Iterator<IMqttsnContext> listContexts() throws MqttsnException;

    /**
     * Return the size of the queue for a given context
     * @param context  - the context whose queue youd like to query
     * @return - the size of the queue for a given context
     * @throws MqttsnException - an error occurred
     */
    int size(IMqttsnContext context) throws MqttsnException;

    /**
     * Offer the queue or a context a new message to add to the tail.
     * @param context  - the context whose queue youd like to append
     * @param message - the message metadata to queue
     * @return - token if the message was added
     * @throws MqttsnException - an error occurred, most likely the queue was full
     */
    MqttsnWaitToken offer(IMqttsnContext context, QueuedPublishMessage message) throws MqttsnException, MqttsnQueueAcceptException;

    /**
     * Pop a message from the head of the queue. This removes and returns the message at the Head.
     * @param context  - the context whose queue youd like to query
     * @return the message from the head of the queue or NULL if the queue is empty
     * @throws MqttsnException - an error occurred
     */
    QueuedPublishMessage pop(IMqttsnContext context) throws MqttsnException;

    /**
     * Peek at the message on the head of the queue without removing it
     * @param context  - the context whose queue youd like to query
     * @return the message from the head of the queue or NULL if the queue is empty
     * @throws MqttsnException - an error occurred
     */
    QueuedPublishMessage peek(IMqttsnContext context) throws MqttsnException;

}
