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

import org.slj.mqtt.sn.MqttsnConstants;

public class TopicInfo {

    // ** when subscribing to a wildcard topic, we return the wildcard alias response **/
    public static TopicInfo WILD = new TopicInfo(MqttsnConstants.TOPIC_TYPE.NORMAL, 0x00);

    MqttsnConstants.TOPIC_TYPE type;
    int topicId;
    String topicPath;

    public TopicInfo(MqttsnConstants.TOPIC_TYPE type, int topicId){
        this.type = type;
        this.topicId = topicId;
    }

    public TopicInfo(MqttsnConstants.TOPIC_TYPE type, String topicPath){
        this.type = type;
        this.topicPath = topicPath;
    }

    public MqttsnConstants.TOPIC_TYPE getType() {
        return type;
    }

    public void setType(MqttsnConstants.TOPIC_TYPE type) {
        this.type = type;
    }

    public int getTopicId() {
       return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public String getTopicPath() {
        return topicPath;
    }

    public void setTopicPath(String topicPath) {
        this.topicPath = topicPath;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TopicInfo{");
        sb.append("type=").append(type);
        sb.append(", topicId=").append(topicId);
        sb.append(", topicPath='").append(topicPath).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
