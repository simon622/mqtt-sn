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

import org.slj.mqtt.sn.model.TopicInfo;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.IMqttsnTopicRegistration;

import java.util.Set;

/**
 * The topic registry is responsible for tracking, storing and determining the correct alias
 * to use for a given remote context and topic combination. The topic registry will be cleared
 * according to session lifecycle rules.
 */
public interface IMqttsnTopicRegistry extends IMqttsnRegistry {

    TopicInfo normalize(byte topicIdType, byte[] topicData, boolean normalAsLong) throws MqttsnException;

    TopicInfo register(IMqttsnSession session, String topicPath) throws MqttsnException;

    void register(IMqttsnSession session, String topicPath, int alias) throws MqttsnException;

    boolean registered(IMqttsnSession session, String topicPath) throws MqttsnException;

    TopicInfo lookup(IMqttsnSession session, String topicPath) throws MqttsnException;

    TopicInfo lookup(IMqttsnSession session, String topicPath, boolean confirmedOnly) throws MqttsnException;

    String topicPath(IMqttsnSession session, TopicInfo topicInfo, boolean considerContext) throws MqttsnException ;

    //-- lookup specific parts of the registry
    Integer lookupRegistered(IMqttsnSession session, String topicPath, boolean confirmedOnly) throws MqttsnException;

    String lookupRegistered(IMqttsnSession session, int topicAlias) throws MqttsnException;

    Integer lookupPredefined(IMqttsnSession session, String topicPath) throws MqttsnException;

    String lookupPredefined(IMqttsnSession session, int topicAlias) throws MqttsnException;

    void clear(IMqttsnSession session, boolean hardClear) throws MqttsnException;

    void clear(IMqttsnSession session) throws MqttsnException;

    Set<IMqttsnTopicRegistration> getRegistrations(IMqttsnSession session) throws MqttsnException;
}
