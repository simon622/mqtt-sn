/*
 * Copyright (c) 2020 Simon Johnson <simon622 AT gmail DOT com>
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
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnContext;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnExpectationFailedException;

import java.util.*;
import java.util.logging.Level;

public class MqttsnInMemoryTopicRegistry<T extends IMqttsnRuntimeRegistry>
        extends AbstractTopicRegistry<T> {

    protected Map<IMqttsnContext, Set<ConfirmableTopicRegistration>> topicLookups;

    @Override
    public synchronized void start(T runtime) throws MqttsnException {
        topicLookups = Collections.synchronizedMap(new HashMap<>());
        super.start(runtime);
    }

    public Set<ConfirmableTopicRegistration> getAll(IMqttsnContext context){
        Set<ConfirmableTopicRegistration> set = topicLookups.get(context);
        if(set == null){
            synchronized (this){
                if((set = topicLookups.get(context)) == null){
                    set = Collections.synchronizedSet(new HashSet<>());
                    topicLookups.put(context, set);
                }
            }
        }
        return set;
    }

    protected Map<String, Integer> getRegistrationsInternal(IMqttsnContext context, boolean confirmedOnly){
        Set<ConfirmableTopicRegistration> set = getAll(context);
        Map<String, Integer> map = new HashMap<>();
        synchronized (set){
            Iterator<ConfirmableTopicRegistration> itr  = set.iterator();
            while(itr.hasNext()){
                ConfirmableTopicRegistration reg = itr.next();
                if(!confirmedOnly || reg.confirmed){
                    map.put(reg.topicPath, reg.aliasId);
                }
            }
        }
        return map;
    }

    @Override
    protected boolean addOrUpdateRegistration(IMqttsnContext context, String topicPath, int alias) throws MqttsnException {

        if(topicPath == null || topicPath.trim().length() == 0)
            throw new MqttsnExpectationFailedException("null or empty topic path not allowed");
        Set<ConfirmableTopicRegistration> set = getAll(context);
        boolean updated = false;
        synchronized (set){
            Iterator<ConfirmableTopicRegistration> itr  = set.iterator();
            if(itr.hasNext()){
                ConfirmableTopicRegistration reg = itr.next();
                if(reg.topicPath.equals(topicPath)){
                    reg.confirmed = true;
                    reg.setAliasId(alias);
                    updated = true;
                }
            }
            if(!updated){
                set.add(new ConfirmableTopicRegistration(topicPath, alias, true));
            }
        }

        return !updated;
    }

    @Override
    protected Map<String, Integer> getPredefinedTopicsForString(IMqttsnContext context) {
        Map<String, Integer> m = registry.getOptions().getPredefinedTopics();
        return m == null ? Collections.emptyMap() : m;
    }

    @Override
    protected Map<String, Integer> getPredefinedTopicsForInteger(IMqttsnContext context) {
        return getPredefinedTopicsForString(context);
    }

    @Override
    public void clearAll() throws MqttsnException {
        topicLookups.clear();
    }

    @Override
    public void clear(IMqttsnContext context, boolean hardClear) throws MqttsnException {
        if(hardClear){
            topicLookups.remove(context);
        } else{
            Set<ConfirmableTopicRegistration> set = topicLookups.get(context);
            if(set != null){
                synchronized (set){
                    Iterator<ConfirmableTopicRegistration> itr  = set.iterator();
                    if(itr.hasNext()){
                        ConfirmableTopicRegistration reg = itr.next();
                        reg.confirmed = false;
                    }
                }
            }
        }
    }

    @Override
    public void clear(IMqttsnContext context) throws MqttsnException {
        topicLookups.remove(context);
    }

    public static class ConfirmableTopicRegistration {

        boolean confirmed;
        String topicPath;
        int aliasId;

        public ConfirmableTopicRegistration(String topicPath, int aliasId, boolean confirmed){
            this.topicPath = topicPath;
            this.aliasId = aliasId;
            this.confirmed = confirmed;
        }

        public boolean isConfirmed() {
            return confirmed;
        }

        public void setConfirmed(boolean confirmed) {
            this.confirmed = confirmed;
        }

        public String getTopicPath() {
            return topicPath;
        }

        public void setTopicPath(String topicPath) {
            this.topicPath = topicPath;
        }

        public int getAliasId() {
            return aliasId;
        }

        public void setAliasId(int aliasId) {
            this.aliasId = aliasId;
        }

        @Override
        public String toString() {
            return "ConfirmableTopicRegistration{" +
                    "confirmed=" + confirmed +
                    ", topicPath='" + topicPath + '\'' +
                    ", aliasId=" + aliasId +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConfirmableTopicRegistration that = (ConfirmableTopicRegistration) o;
            return topicPath.equals(that.topicPath);
        }

        @Override
        public int hashCode() {
            int result = topicPath.hashCode();
            return result;
        }
    }
}
