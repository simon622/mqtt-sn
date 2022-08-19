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

import org.slj.mqtt.sn.impl.AbstractSubscriptionRegistry;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.model.Subscription;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.*;

public class MqttsnInMemorySubscriptionRegistry<T extends IMqttsnRuntimeRegistry>
        extends AbstractSubscriptionRegistry<T> {

    protected Map<IMqttsnContext, Set<Subscription>> subscriptionsLookups;

    @Override
    public synchronized void start(T runtime) throws MqttsnException {
        subscriptionsLookups = Collections.synchronizedMap(new HashMap());
        super.start(runtime);
    }

    @Override
    public List<IMqttsnContext> matches(String topicPath) throws MqttsnException {
        List<IMqttsnContext> matchingClients = new ArrayList<>();
        Iterator<IMqttsnContext> clientItr = null;
        synchronized (subscriptionsLookups) {
            clientItr = new HashSet(subscriptionsLookups.keySet()).iterator();
        }
        while(clientItr.hasNext()){
            IMqttsnContext client = clientItr.next();
            Set<Subscription> paths = subscriptionsLookups.get(client);
            if(paths != null && !paths.isEmpty()){
                synchronized (paths){
                    Iterator<Subscription> pathItr = paths.iterator();
                    client : while(pathItr.hasNext()) {
                        try {
                            Subscription sub = pathItr.next();
                            TopicPath path = sub.getTopicPath();
                            if(path.matches(topicPath)){
                                matchingClients.add(client);
                                break client;
                            }
                        } catch(Exception e){
                            throw new MqttsnException(e);
                        }
                    }
                }
            }
        }

        return matchingClients;
    }

    @Override
    public Set<Subscription> readSubscriptions(IMqttsnContext context){
        Set<Subscription> set = subscriptionsLookups.get(context);
        if(set == null){
            synchronized (subscriptionsLookups){
                if((set = subscriptionsLookups.get(context)) == null){
                    set = new HashSet<>();
                    subscriptionsLookups.put(context, set);
                }
            }
        }
        return set;
    }

    @Override
    protected boolean addSubscription(IMqttsnContext context, Subscription subscription) throws MqttsnException {
        Set<Subscription> set = readSubscriptions(context);
        synchronized (set){
            return set.add(subscription);
        }
    }

    @Override
    protected boolean removeSubscription(IMqttsnContext context, Subscription subscription) throws MqttsnException {
        Set<Subscription> set = readSubscriptions(context);
        synchronized (set){
            return set.remove(subscription);
        }
    }

    @Override
    public void clear(IMqttsnContext context) throws MqttsnException {
        subscriptionsLookups.remove(context);
    }

    @Override
    public void clearAll() {
        subscriptionsLookups.clear();
    }

    @Override
    public Set<TopicPath> readAllSubscribedTopicPaths() {
        Set<TopicPath> topicPaths = new HashSet<>();
        synchronized (subscriptionsLookups) {
            Iterator<IMqttsnContext> clientItr = subscriptionsLookups.keySet().iterator();
            while(clientItr.hasNext()) {
                IMqttsnContext client = clientItr.next();
                Set<Subscription> paths = subscriptionsLookups.get(client);
                if(paths != null){
                    synchronized (paths){
                        paths.stream().map(Subscription::getTopicPath).forEach(topicPaths::add);
                    }
                }
            }
        }
        return topicPaths;
    }
}
