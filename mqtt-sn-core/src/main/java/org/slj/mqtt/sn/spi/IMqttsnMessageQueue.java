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

import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.model.MqttsnWaitToken;
import org.slj.mqtt.sn.model.session.IMqttsnQueuedPublishMessage;
import org.slj.mqtt.sn.model.session.IMqttsnSession;

/**
 * Queue implementation to store messages destined to and from gateways and clients. Queues will be flushed acccording
 * to the session semantics defined during CONNECT.
 *
 * Ideally the queue should be implemented to support FIFO where possible.
 */
public interface IMqttsnMessageQueue<T extends IMqttsnRuntimeRegistry> extends IMqttsnRegistry<T> {

    /**
     * Return the size of the queue for a given context
     * @param session  - the session whose queue youd like to query
     * @return - the size of the queue for a given context
     * @throws MqttsnException - an error occurred
     */
    int size(IMqttsnSession session) throws MqttsnException;

    /**
     * Offer the queue or a context a new message to add to the tail.
     * @param session  - the session whose queue youd like to append
     * @param message - the message metadata to queue
     * @throws MqttsnException - an error occurred, most likely the queue was full
     */
    void offer(IMqttsnSession session, IMqttsnQueuedPublishMessage message)
            throws MqttsnException, MqttsnQueueAcceptException;

    /**
     * Offer the queue or a context a new message to add to the tail.
     * @param session  - the session whose queue youd like to append
     * @param message - the message metadata to queue
     * @return - token if the message was added
     * @throws MqttsnException - an error occurred, most likely the queue was full
     */
    MqttsnWaitToken offerWithToken(IMqttsnSession session, IMqttsnQueuedPublishMessage message)
            throws MqttsnException, MqttsnQueueAcceptException;

    /**
     * Pop a message from the head of the queue. This removes and returns the message at the Head.
     * @param session  - the session whose queue youd like to query
     * @return the message from the head of the queue or NULL if the queue is empty
     * @throws MqttsnException - an error occurred
     */
    IMqttsnQueuedPublishMessage poll(IMqttsnSession session) throws MqttsnException;

    /**
     * Peek at the message on the head of the queue without removing it
     * @param session  - the context whose queue youd like to query
     * @return the message from the head of the queue or NULL if the queue is empty
     * @throws MqttsnException - an error occurred
     */
    IMqttsnQueuedPublishMessage peek(IMqttsnSession session) throws MqttsnException;

    void clear(IMqttsnSession session) throws MqttsnException;

}
