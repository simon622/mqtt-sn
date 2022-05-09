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

import java.io.Serializable;
import java.util.Arrays;

public class MqttsnWillData implements Serializable {

    private static final long serialVersionUID = 2284318994034723218L;

    private TopicPath topicPath;
    private int qos;
    private boolean retain;
    private byte[] data;

    public MqttsnWillData() {

    }

    public MqttsnWillData(TopicPath topicPath, byte[] data, int qos, boolean retain) {
        this.topicPath = topicPath;
        this.data = data;
        this.qos = qos;
        this.retain = retain;
    }

    public TopicPath getTopicPath() {
        return topicPath;
    }

    public void setTopicPath(TopicPath topicPath) {
        this.topicPath = topicPath;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean isRetain() {
        return retain;
    }

    public void setRetain(boolean retain) {
        this.retain = retain;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    @Override
    public String toString() {
        return "MqttsnWillData{" +
                "topicPath=" + topicPath +
                ", qos=" + qos +
                ", retain=" + retain +
                ", data=" + (data == null ? "<null>" : data.length) +
                '}';
    }
}
