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

import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.model.session.ISubscription;
import org.slj.mqtt.sn.model.session.impl.SubscriptionImpl;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.Iterator;
import java.util.Set;

public abstract class AbstractSubscriptionRegistry
        extends AbstractMqttsnSessionBeanRegistry
        implements IMqttsnSubscriptionRegistry {

    @Override
    public boolean subscribe(ISession session, String topicPath, int QoS) throws MqttsnException, MqttsnIllegalFormatException {
        TopicPath path = new TopicPath(
                getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath));
        return addSubscription(session, new SubscriptionImpl(path, QoS));
    }

    @Override
    public boolean unsubscribe(ISession session, String topicPath) throws MqttsnException {
        Set<ISubscription> paths = readSubscriptions(session);
        TopicPath path = new TopicPath(
                getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath));
        SubscriptionImpl sub = new SubscriptionImpl(path);
        if(paths.contains(sub)){
            return removeSubscription(session, sub);
        }
        return false;
    }

    @Override
    public int getQos(ISession session, String topicPath) throws MqttsnException {
        Set<ISubscription> paths = readSubscriptions(session);
        if(paths != null && !paths.isEmpty()) {
            Iterator<ISubscription> pathItr = paths.iterator();
            client:
            while (pathItr.hasNext()) {
                try {
                    ISubscription sub = pathItr.next();
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

    public abstract Set<ISubscription> readSubscriptions(ISession session) throws MqttsnException ;

    public abstract void clear(ISession session) ;

    protected abstract boolean addSubscription(ISession session, ISubscription subscription) throws MqttsnException, MqttsnIllegalFormatException;

    protected abstract boolean removeSubscription(ISession session, ISubscription subscription) throws MqttsnException ;
}
