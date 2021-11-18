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

package org.slj.mqtt.sn.model;

import org.slj.mqtt.sn.utils.TopicPath;

import java.util.Objects;

public class Subscription {

    TopicPath topicPath;
    int QoS;

    public Subscription(){

    }

    public Subscription(TopicPath topicPath){
        this.topicPath = topicPath;
    }

    public Subscription(TopicPath topicPath, int qoS) {
        this.topicPath = topicPath;
        QoS = qoS;
    }

    public TopicPath getTopicPath() {
        return topicPath;
    }

    public void setTopicPath(TopicPath topicPath) {
        this.topicPath = topicPath;
    }

    public int getQoS() {
        return QoS;
    }

    public void setQoS(int qoS) {
        QoS = qoS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscription that = (Subscription) o;
        return Objects.equals(topicPath, that.topicPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicPath);
    }
}
