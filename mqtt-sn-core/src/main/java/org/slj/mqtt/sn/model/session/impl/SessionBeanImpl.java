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

import org.slj.mqtt.sn.model.ClientState;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.session.IQueuedPublishMessage;
import org.slj.mqtt.sn.model.session.ISubscription;
import org.slj.mqtt.sn.model.session.ITopicRegistration;
import org.slj.mqtt.sn.model.session.IWillData;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class SessionBeanImpl extends SessionImpl {

    private static final int INITIAL_CAPACITY = 8;
    private Set<ISubscription> subscriptionSet = new HashSet<>(INITIAL_CAPACITY);
    private Map<String, ITopicRegistration> registrationMap = new HashMap<>(INITIAL_CAPACITY);
    private Queue<IQueuedPublishMessage> messageQueue = new PriorityBlockingQueue<>(INITIAL_CAPACITY);
    private IWillData willData;

    public SessionBeanImpl(IClientIdentifierContext context, ClientState state) {
        super(context, state);
    }

    public boolean addSubscription(ISubscription subscription){
        return subscriptionSet.add(subscription);
    }

    public boolean removeSubscription(ISubscription subscription){
        return subscriptionSet.remove(subscription);
    }

    public boolean addTopicRegistration(ITopicRegistration registration){
        return registrationMap.put(registration.getTopicPath(), registration) == null;
    }

    public boolean removeTopicRegistration(ITopicRegistration registration){
        return registrationMap.remove(registration.getTopicPath()) != null;
    }

    public boolean offer(IQueuedPublishMessage message){
        synchronized (messageQueue){
            return messageQueue.offer(message);
        }
    }

    public int getQueueSize(){
        return messageQueue.size();
    }

    public IQueuedPublishMessage peek(){
        return messageQueue.peek();
    }

    public IQueuedPublishMessage poll(){
        return messageQueue.poll();
    }

    public void clearSubscriptions(){
        subscriptionSet.clear();
    }

    public Set<ISubscription> getSubscriptions(){
        return Collections.unmodifiableSet(new HashSet<>(subscriptionSet));
    }

    public Map<String, ITopicRegistration> getRegistrations(){
        return Collections.unmodifiableMap(registrationMap);
    }

    public IWillData getWillData() {
        return willData;
    }

    public void setWillData(IWillData willData) {
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
