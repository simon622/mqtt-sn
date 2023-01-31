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

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.impl.AbstractSubscriptionRegistry;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.model.session.ISubscription;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnIllegalFormatException;
import org.slj.mqtt.tree.MqttTree;
import org.slj.mqtt.tree.MqttTreeException;
import org.slj.mqtt.tree.MqttTreeLimitExceededException;

import java.util.Set;

public class MqttsnInMemorySubscriptionRegistry
        extends AbstractSubscriptionRegistry {

    private MqttTree<IClientIdentifierContext> tree;

    @Override
    public synchronized void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        tree = new MqttTree<>(MqttsnConstants.PATH_SEP, true);
        tree.withMaxMembersAtLevel(1024 * 1024);
        tree.withWildcard(MqttsnConstants.MULTI_LEVEL_WILDCARD);
        tree.withWildpath(MqttsnConstants.SINGLE_LEVEL_WILDCARD);
    }

    @Override
    public Set<IClientIdentifierContext> matches(String topicPath) throws MqttsnException, MqttsnIllegalFormatException {

        if (!MqttsnSpecificationValidator.isValidPublishTopic(
                topicPath)) {
            throw new MqttsnIllegalFormatException("invalid topic format detected");
        }
        Set<IClientIdentifierContext> treeMatches = matchFromTree(topicPath);
        return treeMatches;
    }

    protected Set<IClientIdentifierContext> matchFromTree(String topicPath) throws MqttsnException {
        try {
            return tree.search(topicPath);
        } catch (MqttTreeException e) {
            throw new MqttsnException(e);
        }
    }

    @Override
    public Set<ISubscription> readSubscriptions(ISession session){
        return getSessionBean(session).getSubscriptions();
    }

    @Override
    protected boolean addSubscription(ISession session, ISubscription subscription)
            throws MqttsnIllegalFormatException, MqttsnException {

        if(!MqttsnSpecificationValidator.isValidSubscriptionTopic(
                subscription.getTopicPath().toString())){
            throw new MqttsnIllegalFormatException("invalid topic format detected");
        }

        boolean existed = getSessionBean(session).removeSubscription(subscription);
        getSessionBean(session).addSubscription(subscription);
        if(!existed){
            try {
                tree.addSubscription(subscription.getTopicPath().toString(), session.getContext());
            } catch (MqttTreeLimitExceededException | MqttTreeException e) {
                throw new MqttsnException(e);
            }
        }
        return !existed;
    }

    @Override
    protected boolean removeSubscription(ISession session, ISubscription subscription) throws MqttsnException{
        boolean removed = getSessionBean(session).removeSubscription(subscription);
        if(removed){
            try {
                tree.removeSubscriptionFromPath(subscription.getTopicPath().toString(), session.getContext());
            } catch (MqttTreeException e) {
                throw new MqttsnException(e);
            }
        }
        return removed;
    }

    @Override
    public boolean hasSubscription(String topicPath) {
        boolean exists = tree.hasMembers(topicPath);
        logger.info("subscription exists ? {} -> {}", topicPath, exists);
        return exists;
    }

    @Override
    public void clear(ISession session) {
        //need tp clear from index and lists
        Set<ISubscription> all = readSubscriptions(session);
        for(ISubscription s : all){
            try {
                removeSubscription(session, s);
            } catch (MqttsnException e) {
                logger.warn("error clearing subscription from session", e);
            }
        }
    }

    @Override
    public Set<String> readAllSubscribedTopicPaths() {
        return tree.getDistinctPaths(true);
    }
}
