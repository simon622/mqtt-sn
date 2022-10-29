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
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.IMqttsnSubscription;

import java.util.Set;

/**
 * The subscription registry maintains a list of subscriptions against the remote context. On the gateway this
 * is used to determine which clients are subscribed to which topics to enable outbound delivery. In client
 * mode it tracks the subscriptions a client presently holds.
 */
public interface IMqttsnSubscriptionRegistry extends IMqttsnRegistry {

    /**
     * Which QoS is a subscription held at
     * @param session - the remote context who owns the subscription
     * @param topicPath - the full clear text topicPath for the subscription e.g. foo/bar
     * @return the QoS at which the subscription is held (0,1,2)
     * @throws MqttsnException - an error occurred
     */
    int getQos(IMqttsnSession session, String topicPath) throws MqttsnException;

    /**
     * Create a new subscription for the context, or update the subscription if it already
     * existed
     * @param session - the remote context who owns the subscription
     * @param topicPath - the full clear text topicPath for the subscription e.g. foo/bar
     * @return true if a NEW subscription or was created, false if one already existed (and was updated)
     * @throws MqttsnException - an error occurred
     */
    boolean subscribe(IMqttsnSession session, String topicPath, int QoS) throws MqttsnException, MqttsnIllegalFormatException;

    /**
     * Remove and existing subscription for the context
     * @param session - the remote context who owns the subscription
     * @param topicPath - the full clear text topicPath for the subscription e.g. foo/bar
     * @return true if a subscription was removed, false if one didnt exist
     * @throws MqttsnException - an error occurred
     */
    boolean unsubscribe(IMqttsnSession session, String topicPath) throws MqttsnException;

    /**
     * This is called upon receipt of a message being received by a BROKER which necessitated expansion
     * onto mutliple client queues. (Message expansion).
     *
     * @param topicPath - the full clear text topicPath for the subscription e.g. foo/bar
     * @return a list of context which hold valid subscriptions for the supplied topic (including wildcard matching)
     * @throws MqttsnException
     */
    Set<IMqttsnContext> matches(String topicPath) throws MqttsnException, MqttsnIllegalFormatException ;


    /**
     * A set of all the tracked subscriptions for the context
     *
     * @param session - the remote context who owns the subscriptions
     * @return a set of subscriptions to which the context is subscribed
     * @throws MqttsnException
     */
    Set<IMqttsnSubscription> readSubscriptions(IMqttsnSession session) throws MqttsnException ;


    /**
     * A set of distinct subscriptions held by this registry
     * @return a set of subscriptions to which the context is subscribed
     * @throws MqttsnException
     */
    Set<String> readAllSubscribedTopicPaths() throws MqttsnException;

    void clear(IMqttsnSession session) throws MqttsnException;

    /**
     * This is used by an aggregating gateway to track if a subscription exists anywhere
     * in the system for this topic, if it does, it should not be resubscribed
     */
    boolean hasSubscription(String topicPath) throws MqttsnException, MqttsnIllegalFormatException ;
}
