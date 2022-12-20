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

import java.io.Serializable;

public class PublishData implements Serializable {

    private static final long serialVersionUID = -1524505593526571492L;
    private boolean retained;
    private String topicPath;
    private int qos;
    private byte[] data;

    public PublishData() {
    }

    public PublishData(int qos, boolean retained, byte[] data) {
        this.qos = qos;
        this.data = data;
        this.retained = retained;
    }

    public PublishData(String topicPath, int qos, boolean retained) {
        this.qos = qos;
        this.topicPath = topicPath;
        this.retained = retained;
    }

    public PublishData(String topicPath, int qos, boolean retained, byte[] data) {
        this.topicPath = topicPath;
        this.qos = qos;
        this.data = data;
        this.retained = retained;
    }

    public String getTopicPath() {
        return topicPath;
    }

    public int getQos() {
        return qos == 3 ? -1 : qos;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isRetained() {
        return retained;
    }

    public void setTopicPath(String topicPath) {
        this.topicPath = topicPath;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "PublishData{" +
                "retained=" + retained +
                ", topicPath='" + topicPath + '\'' +
                ", qos=" + qos +
                ", size=" + (data == null ? "<null>" : data.length) +
                '}';
    }
}
