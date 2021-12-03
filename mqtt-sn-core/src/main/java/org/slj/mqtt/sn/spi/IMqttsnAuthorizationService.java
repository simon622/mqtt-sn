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

/**
 * Optional - when installed it will be consulted to determine whether a remote context can perform certain
 * operations;
 *
 * SUBSCRIBE to a given topicPath
 * Granted Maximum Subscription Levels
 * Eligibility to publish to a given path & size
 */
public interface IMqttsnAuthorizationService {

    /**
     * Is a client allowed to SUBSCRIBE to a given topic path
     * @param context - the context who would like to SUBSCRIBE
     * @param topicPath - the topic path to subscribe to
     * @return true if the client is allowed to SUBSCRIBE (yielding SUBACK ok) or false if not allowed (yielding SUBACK error)
     * @throws MqttsnException an error occurred
     */
    boolean allowedToSubscribe(IMqttsnContext context, String topicPath) throws MqttsnException;

    /**
     * What is the maximum granted QoS allowed on a given topicPath.
     * @param context - the context who would like to SUBSCRIBE
     * @param topicPath - the topic path to subscribe to
     * @return one of 0,1,2 matching the maximum QoS that can be granted for this subscription
     * @throws MqttsnException an error occurred
     */
    int allowedMaximumQoS(IMqttsnContext context, String topicPath) throws MqttsnException;

    /**
     * Is the context allowed to publish to the given topicPath.
     * @param context - the context who would like to PUBLISH
     * @param topicPath - the topic path to publish to
     * @param size - size of the data to be published
     * @return true if the client is allowed to PUBLISH (yielding PUBLISH normal lifecycle) or false if not allowed (yielding PUBACK error)
     * @throws MqttsnException an error occurred
     */
    boolean allowedToPublish(IMqttsnContext context, String topicPath, int size, int QoS) throws MqttsnException;
}
