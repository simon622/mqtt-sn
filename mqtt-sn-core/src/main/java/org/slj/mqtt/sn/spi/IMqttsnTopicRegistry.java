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
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.model.session.ITopicRegistration;

import java.util.Set;

/**
 * The topic registry is responsible for tracking, storing and determining the correct alias
 * to use for a given remote context and topic combination. The topic registry will be cleared
 * according to session lifecycle rules.
 */
@MqttsnService
public interface IMqttsnTopicRegistry extends IMqttsnService  {

    TopicInfo normalize(byte topicIdType, byte[] topicData, boolean normalAsLong) throws MqttsnException;

    TopicInfo register(ISession session, String topicPath) throws MqttsnException;

    void register(ISession session, String topicPath, int alias) throws MqttsnException;

    boolean registered(ISession session, String topicPath) throws MqttsnException;

    TopicInfo lookup(ISession session, String topicPath) throws MqttsnException;

    TopicInfo lookup(ISession session, String topicPath, boolean confirmedOnly) throws MqttsnException;

    String topicPath(ISession session, TopicInfo topicInfo, boolean considerContext) throws MqttsnException ;

    //-- lookup specific parts of the registry
    Integer lookupRegistered(ISession session, String topicPath, boolean confirmedOnly) throws MqttsnException;

    String lookupRegistered(ISession session, int topicAlias) throws MqttsnException;

    Integer lookupPredefined(ISession session, String topicPath) throws MqttsnException;

    String lookupPredefined(ISession session, int topicAlias) throws MqttsnException;

    void clear(ISession session, boolean hardClear) throws MqttsnException;

    void clear(ISession session) throws MqttsnException;

    Set<ITopicRegistration> getRegistrations(ISession session) throws MqttsnException;
}
