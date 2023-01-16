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

import org.slj.mqtt.sn.model.session.ISubscription;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.Objects;

public class SubscriptionImpl implements ISubscription {

    private final TopicPath topicPath;
    private int grantedQoS;

    public SubscriptionImpl(final TopicPath topicPath){
        this.topicPath = topicPath;
    }

    public SubscriptionImpl(final TopicPath topicPath, int qoS) {
        this.topicPath = topicPath;
        grantedQoS = qoS;
    }

    public TopicPath getTopicPath() {
        return topicPath;
    }

    public int getGrantedQoS() {
        return grantedQoS;
    }

    public void setGrantedQoS(int qoS) {
        grantedQoS = qoS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionImpl that = (SubscriptionImpl) o;
        return Objects.equals(topicPath, that.topicPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicPath);
    }
}
