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

package org.slj.mqtt.sn.model.session.impl;

import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnClientState;
import org.slj.mqtt.sn.model.session.IMqttsnQueuedPublishMessage;
import org.slj.mqtt.sn.model.session.IMqttsnSubscription;
import org.slj.mqtt.sn.model.session.IMqttsnTopicRegistration;
import org.slj.mqtt.sn.model.session.IMqttsnWillData;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class MqttsnSessionBeanImpl extends MqttsnSessionImpl {

    private static final int INITIAL_CAPACITY = 8;
    private Set<IMqttsnSubscription> subscriptionSet = new HashSet<>(INITIAL_CAPACITY);
    private Map<String, IMqttsnTopicRegistration> registrationMap = new HashMap<>(INITIAL_CAPACITY);
    private Queue<IMqttsnQueuedPublishMessage> messageQueue = new PriorityBlockingQueue<>(INITIAL_CAPACITY);
    private IMqttsnWillData willData;

    public MqttsnSessionBeanImpl(IMqttsnContext context, MqttsnClientState state) {
        super(context, state);
    }

    public boolean addSubscription(IMqttsnSubscription subscription){
        return subscriptionSet.add(subscription);
    }

    public boolean removeSubscription(IMqttsnSubscription subscription){
        return subscriptionSet.remove(subscription);
    }

    public boolean addTopicRegistration(IMqttsnTopicRegistration registration){
        return registrationMap.put(registration.getTopicPath(), registration) == null;
    }

    public boolean removeTopicRegistration(IMqttsnTopicRegistration registration){
        return registrationMap.remove(registration.getTopicPath()) != null;
    }

    public boolean offer(IMqttsnQueuedPublishMessage message){
        synchronized (messageQueue){
            return messageQueue.offer(message);
        }
    }

    public int getQueueSize(){
        return messageQueue.size();
    }

    public IMqttsnQueuedPublishMessage peek(){
        return messageQueue.peek();
    }

    public IMqttsnQueuedPublishMessage poll(){
        return messageQueue.poll();
    }

    public void clearSubscriptions(){
        subscriptionSet.clear();
    }

    public Set<IMqttsnSubscription> getSubscriptions(){
        return Collections.unmodifiableSet(subscriptionSet);
    }

    public Map<String, IMqttsnTopicRegistration> getRegistrations(){
        return Collections.unmodifiableMap(registrationMap);
    }

    public IMqttsnWillData getWillData() {
        return willData;
    }

    public void setWillData(IMqttsnWillData willData) {
        this.willData = willData;
    }

    public void clearMessageQueue(){
        messageQueue.clear();
    }

    public void clearRegistrations(){
        registrationMap.clear();
    }

    @Override
    public void clearAll() {
        super.clearAll();
        clearSubscriptions();
        clearMessageQueue();
        clearRegistrations();
    }
}
