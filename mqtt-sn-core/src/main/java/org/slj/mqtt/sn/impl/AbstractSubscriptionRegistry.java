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

package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.IMqttsnSubscription;
import org.slj.mqtt.sn.model.session.impl.MqttsnSubscriptionImpl;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.Iterator;
import java.util.Set;

public abstract class AbstractSubscriptionRegistry
        extends AbstractMqttsnSessionBeanRegistry
        implements IMqttsnSubscriptionRegistry {

    @Override
    public boolean subscribe(IMqttsnSession session, String topicPath, int QoS) throws MqttsnException, MqttsnIllegalFormatException {
        TopicPath path = new TopicPath(
                getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath));
        return addSubscription(session, new MqttsnSubscriptionImpl(path, QoS));
    }

    @Override
    public boolean unsubscribe(IMqttsnSession session, String topicPath) throws MqttsnException {
        Set<IMqttsnSubscription> paths = readSubscriptions(session);
        TopicPath path = new TopicPath(
                getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath));
        MqttsnSubscriptionImpl sub = new MqttsnSubscriptionImpl(path);
        if(paths.contains(sub)){
            return removeSubscription(session, sub);
        }
        return false;
    }

    @Override
    public int getQos(IMqttsnSession session, String topicPath) throws MqttsnException {
        Set<IMqttsnSubscription> paths = readSubscriptions(session);
        if(paths != null && !paths.isEmpty()) {
            Iterator<IMqttsnSubscription> pathItr = paths.iterator();
            client:
            while (pathItr.hasNext()) {
                try {
                    IMqttsnSubscription sub = pathItr.next();
                    TopicPath path = sub.getTopicPath();
                    if (path.matches(
                            getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath))) {
                        return sub.getGrantedQoS();
                    }
                } catch (Exception e) {
                    throw new MqttsnException(e);
                }
            }
        }
        throw new MqttsnException("no matching subscription found for client");
    }

    public abstract Set<IMqttsnSubscription> readSubscriptions(IMqttsnSession session) throws MqttsnException ;

    public abstract void clear(IMqttsnSession session) ;

    public abstract Set<TopicPath> readAllSubscribedTopicPaths() throws MqttsnException ;

    protected abstract boolean addSubscription(IMqttsnSession session, IMqttsnSubscription subscription) throws MqttsnException, MqttsnIllegalFormatException;

    protected abstract boolean removeSubscription(IMqttsnSession session, IMqttsnSubscription subscription) throws MqttsnException ;
}
