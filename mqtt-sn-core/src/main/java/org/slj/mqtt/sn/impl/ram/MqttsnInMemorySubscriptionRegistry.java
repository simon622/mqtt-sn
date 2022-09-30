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

import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.impl.AbstractSubscriptionRegistry;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.IMqttsnSubscription;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnIllegalFormatException;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.TopicPath;
import org.slj.mqtt.sn.utils.TriesTree;

import java.util.*;
import java.util.logging.Level;

public class MqttsnInMemorySubscriptionRegistry
        extends AbstractSubscriptionRegistry {

    private TriesTree<IMqttsnContext> tree;

    @Override
    public synchronized void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        tree = new TriesTree<>("/");
        tree.addWildcard("#");
        tree.addWildpath("+");
    }

    @Override
    public Set<IMqttsnContext> matches(String topicPath) throws MqttsnException, MqttsnIllegalFormatException {

        if (!MqttsnSpecificationValidator.isValidPublishTopic(
                topicPath)) {
            throw new MqttsnIllegalFormatException("invalid topic format detected");
        }

//        long start = System.currentTimeMillis();
//        Set<IMqttsnContext> sessionMatches = matchFromSessions(topicPath);
//        logger.log(Level.INFO, String.format("got [%s] session matches in [%s]", sessionMatches.size(),
//                MqttsnUtils.getDurationString(System.currentTimeMillis() - start)));

        long start = System.currentTimeMillis();
        Set<IMqttsnContext> treeMatches = matchFromTree(topicPath);
        logger.log(Level.INFO, String.format("got [%s] tree matches in [%s]", treeMatches.size(),
                MqttsnUtils.getDurationString(System.currentTimeMillis() - start)));

        return treeMatches;
    }

    protected Set<IMqttsnContext> matchFromTree(String topicPath) throws MqttsnException {
        return tree.search(topicPath);
    }

    protected Set<IMqttsnContext> matchFromSessions(String topicPath) throws MqttsnException {

        Set<IMqttsnContext> matchingClients = new HashSet<>();
        Iterator<IMqttsnSession> sessionItr = getRegistry().getSessionRegistry().iterator();
        while(sessionItr.hasNext()){
            IMqttsnSession session = sessionItr.next();
            Set<IMqttsnSubscription> paths = getRegistry().getSubscriptionRegistry().readSubscriptions(session);
            if(paths != null && !paths.isEmpty()){
                synchronized (paths){
                    Iterator<IMqttsnSubscription> pathItr = paths.iterator();
                    client : while(pathItr.hasNext()) {
                        try {
                            IMqttsnSubscription sub = pathItr.next();
                            TopicPath path = sub.getTopicPath();
                            if(path.matches(topicPath)){
                                matchingClients.add(session.getContext());
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
    public Set<IMqttsnSubscription> readSubscriptions(IMqttsnSession session){
        return getSessionBean(session).getSubscriptions();
    }

    @Override
    protected boolean addSubscription(IMqttsnSession session, IMqttsnSubscription subscription) throws MqttsnIllegalFormatException {
        if(!MqttsnSpecificationValidator.isValidSubscriptionTopic(
                subscription.getTopicPath().toString())){
            throw new MqttsnIllegalFormatException("invalid topic format detected");
        }

        boolean existed = getSessionBean(session).removeSubscription(subscription);
        getSessionBean(session).addSubscription(subscription);
        if(!existed){
            tree.addPath(subscription.getTopicPath().toString(), session.getContext());
        }
        return !existed;
    }

    @Override
    protected boolean removeSubscription(IMqttsnSession session, IMqttsnSubscription subscription){
        boolean removed = getSessionBean(session).removeSubscription(subscription);
        if(removed){
            tree.removeMemberFromPath(subscription.getTopicPath().toString(), session.getContext());
        }
        return removed;
    }

    @Override
    public void clear(IMqttsnSession session) {
        getSessionBean(session).clearSubscriptions();
    }

    @Override
    public Set<TopicPath> readAllSubscribedTopicPaths() {
        //TODO use the trie tree here
        return new HashSet<>();

//        Set<TopicPath> topicPaths = new HashSet<>();
//        synchronized (subscriptionsLookups) {
//            Iterator<IMqttsnContext> clientItr = subscriptionsLookups.keySet().iterator();
//            while(clientItr.hasNext()) {
//                IMqttsnContext client = clientItr.next();
//                Set<MqttsnSubscriptionImpl> paths = subscriptionsLookups.get(client);
//                if(paths != null){
//                    synchronized (paths){
//                        paths.stream().map(MqttsnSubscriptionImpl::getTopicPath).forEach(topicPaths::add);
//                    }
//                }
//            }
//        }
//        return topicPaths;
    }
}
