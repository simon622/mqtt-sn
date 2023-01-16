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

import org.slj.mqtt.sn.impl.AbstractTopicRegistry;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.model.session.ITopicRegistration;
import org.slj.mqtt.sn.model.session.impl.TopicRegistrationImpl;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnExpectationFailedException;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;

import java.util.*;
import java.util.stream.Collectors;

public class MqttsnInMemoryTopicRegistry
        extends AbstractTopicRegistry {

    @Override
    public Set<ITopicRegistration> getRegistrations(ISession session) throws MqttsnException {
        Map<String, ITopicRegistration> registrations = getSessionBean(session).getRegistrations();
        Set<ITopicRegistration> set;
        synchronized (registrations){
            set = registrations.values().stream().collect(
                    Collectors.toSet());
        }
        return Collections.unmodifiableSet(set);
    }



    @Override
    protected boolean addOrUpdateRegistration(ISession session, String topicPath, int alias) throws MqttsnException {

        if(topicPath == null || topicPath.trim().length() == 0)
            throw new MqttsnExpectationFailedException("null or empty topic path not allowed");
        return getSessionBean(session).addTopicRegistration(new TopicRegistrationImpl(topicPath, alias, true));
    }

    @Override
    protected Map<String, Integer> getPredefinedTopicsForString(ISession session) {
        Map<String, Integer> m = registry.getOptions().getPredefinedTopics();
        return m == null ? Collections.emptyMap() : m;
    }

    @Override
    protected Map<String, Integer> getPredefinedTopicsForInteger(ISession session) {
        return getPredefinedTopicsForString(session);
    }

    @Override
    public void clear(ISession session, boolean hardClear) throws MqttsnException {
        if(hardClear){
            getSessionBean(session).clearRegistrations();
        } else{
            Map<String, ITopicRegistration> map =
                    getSessionBean(session).getRegistrations();
            synchronized (map){
                map.values().stream().forEach(t -> t.setConfirmed(false));
            }
        }
    }

    @Override
    public void clear(ISession session) {
        try {
            clear(session, true);
        } catch(MqttsnException e){
            throw new MqttsnRuntimeException(e);
        }
    }
}
