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

import org.slj.mqtt.sn.model.*;

import java.util.Date;
import java.util.Optional;

/**
 * The state service is responsible for sending messages and processing received messages. It maintains state
 * and tracks messages in and out and their successful acknowledgement (or not).
 *
 * The message handling layer will call into the state service with messages it has received, and the queue processor
 * will use the state service to dispatch new outbound publish messages.
 */
public interface IMqttsnMessageStateService<T extends IMqttsnRuntimeRegistry> extends IMqttsnRegistry<T> {

    /**
     * Dispatch a new message to the transport layer, binding it to the state service en route for tracking.
     * @param context - the recipient of the message
     * @param message - the wire message to send
     * @throws MqttsnException
     */
    MqttsnWaitToken sendMessage(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException;

    /**
     * Dispatch a new message to the transport layer, binding it to the state service en route for tracking.
     * @param context
     * @param queuedPublishMessage - reference to the queues message who originated this send (when its of type Publish);
     * @throws MqttsnException
     */
    MqttsnWaitToken sendMessage(IMqttsnContext context, TopicInfo info, QueuedPublishMessage queuedPublishMessage) throws MqttsnException;


    /**
     * Notify into the state service that a new message has arrived from the transport layer.
     * @param context - the context from which the message was received
     * @param message - the message received from the transport layer
     * @return the messsage (if any) that was confirmed by the receipt of the inbound message
     * @throws MqttsnException
     */
    IMqttsnMessage notifyMessageReceived(IMqttsnContext context, IMqttsnMessage message) throws MqttsnException;

    /**
     * Join the message sent in waiting for the subsequent confirmation if it needs one
     * @param context - The context to whom you are speaking
     * @return An optional which will contain either the confirmation message associated with the
     * message supplied OR optional NULL where the message does not require a reply
     * @throws MqttsnExpectationFailedException - When no confirmation was recieved in the time period
     * specified by the runtime options
     */
    Optional<IMqttsnMessage> waitForCompletion(IMqttsnContext context, MqttsnWaitToken token) throws MqttsnExpectationFailedException;

    /**
     * Join the message sent in waiting for the subsequent confirmation if it needs one
     * @param context - The context to whom you are speaking
     * @param customWaitTime - a custom time to wait for the token to complete
     * @return An optional which will contain either the confirmation message associated with the
     * message supplied OR optional NULL where the message does not require a reply
     * @throws MqttsnExpectationFailedException - When no confirmation was recieved in the time period
     * specified by the runtime options
     */
    Optional<IMqttsnMessage> waitForCompletion(IMqttsnContext context, MqttsnWaitToken token, int customWaitTime) throws MqttsnExpectationFailedException;

    /**
     * If a context has any messages inflight, notify the state service to clear them up, either requeuing or discarding
     * according to configuration
     * @param context - The context to whom you are speaking
     * @throws MqttsnException - an error has occurred
     */
    void clearInflight(IMqttsnContext context) throws MqttsnException ;

    /**
     * Ccount the number of message inflight for a given direction and context
     * @param context - The context to whom you are speaking
     * @param direction - Inflight has 2 channels, inbound and outbound per the spec. Count in which direction.
     * @return The number of messages inflight in a given direction
     * @throws MqttsnException - an error has occurred
     */
    int countInflight(IMqttsnContext context, InflightMessage.DIRECTION direction) throws MqttsnException ;

    /**
     * Remove a specific message from inflight using its messageId, returning the message.
     * If no message exists inflight with the corresponding ID, null will be returned.
     * @param context - The context to whom you are speaking
     * @param msgId - The message id to return
     * @return - the corresponding message or NULL if not found
     * @throws MqttsnException - an error has occurred
     */
    InflightMessage removeInflight(IMqttsnContext context, int msgId) throws MqttsnException ;

    /**
     * According to the state rules, are we in a position to send PUBLISH messages to the given context
     * @param context - The context to whom you are speaking
     * @return true if the state service think we're ok to send PUBLISHES to the device, else false
     * @throws MqttsnException - an error has occurred
     */
    boolean canSend(IMqttsnContext context) throws MqttsnException ;

    /**
     * Mark a context active to have its outbound queue processed
     * @param context - The context whose queue should be processed
     * @throws MqttsnException - an error has occurred
     */
    void scheduleFlush(IMqttsnContext context) throws MqttsnException ;

    /**
     * UnMark a context active to have its outbound queue processed
     * @param context - The context whose queue should be processed
     * @throws MqttsnException - an error has occurred
     */
    void unscheduleFlush(IMqttsnContext context) throws MqttsnException ;

    /**
     * Tracks the point at which the last message was SENT to the context
     * @param context - The context to which the message was sent
     * @return Long representing the point at which the last message was SENT to the context
     * @throws MqttsnException
     */
    Long getMessageLastSentToContext(IMqttsnContext context) throws MqttsnException ;

    /**
     * Tracks the point at which the last message was RECEIVED from the context
     * @param context - The context to which the message was sent
     * @return Long representing the point at which the last message was SENT to the context
     * @throws MqttsnException
     */
    Long getMessageLastReceivedFromContext(IMqttsnContext context) throws MqttsnException ;
}
