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
import org.slj.mqtt.sn.model.Subscription;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.Iterator;
import java.util.Set;

public abstract class AbstractSubscriptionRegistry <T extends IMqttsnRuntimeRegistry>
        extends AbstractRationalTopicService<T>
        implements IMqttsnSubscriptionRegistry<T> {

    @Override
    public boolean subscribe(IMqttsnContext context, String topicPath, int QoS) throws MqttsnException, MqttsnIllegalFormatException {
        TopicPath path = new TopicPath(rationalizeTopic(context, topicPath));
        return addSubscription(context, new Subscription(path, QoS));
    }

    @Override
    public boolean unsubscribe(IMqttsnContext context, String topicPath) throws MqttsnException {
        Set<Subscription> paths = readSubscriptions(context);
        TopicPath path = new TopicPath(rationalizeTopic(context, topicPath));
        Subscription sub = new Subscription(path);
        if(paths.contains(sub)){
            return removeSubscription(context, sub);
        }
        return false;
    }

    @Override
    public int getQos(IMqttsnContext context, String topicPath) throws MqttsnException {
        Set<Subscription> paths = readSubscriptions(context);
        if(paths != null && !paths.isEmpty()) {
            Iterator<Subscription> pathItr = paths.iterator();
            client:
            while (pathItr.hasNext()) {
                try {
                    Subscription sub = pathItr.next();
                    TopicPath path = sub.getTopicPath();
                    if (path.matches(rationalizeTopic(context, topicPath))) {
                        return sub.getQoS();
                    }
                } catch (Exception e) {
                    throw new MqttsnException(e);
                }
            }
        }
        throw new MqttsnException("no matching subscription found for client");
    }

    public abstract Set<Subscription> readSubscriptions(IMqttsnContext context) throws MqttsnException ;

    public abstract Set<TopicPath> readAllSubscribedTopicPaths() throws MqttsnException ;

    protected abstract boolean addSubscription(IMqttsnContext context, Subscription subscription) throws MqttsnException, MqttsnIllegalFormatException;

    protected abstract boolean removeSubscription(IMqttsnContext context, Subscription subscription) throws MqttsnException ;
}
