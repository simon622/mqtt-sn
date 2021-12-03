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

package org.slj.mqtt.sn;

public class PublishData {

    boolean retained;
    String topicPath;
    int qos;
    byte[] data;

    public PublishData(int qos, byte[] data, boolean retained) {
        this.qos = qos;
        this.data = data;
        this.retained = retained;
    }

    public PublishData(String topicPath, int qos, byte[] data, boolean retained) {
        this.topicPath = topicPath;
        this.qos = qos;
        this.data = data;
        this.retained = retained;
    }

    public String getTopicPath() {
        return topicPath;
    }

    public int getQos() {
        return qos;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isRetained() {
        return retained;
    }
}
