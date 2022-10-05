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

import java.util.Comparator;

public class MqttsnMetricSample {

    private final long timestamp;
    private long longValue;
    public MqttsnMetricSample(long timestamp) {
        this.timestamp = timestamp;
    }

    public MqttsnMetricSample(long timestamp, long value) {
        this(timestamp);
        this.longValue = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    @Override
    public String toString() {
        return "MqttsnMetricSample{" +
                "timestamp=" + timestamp +
                ", longValue=" + longValue +
                '}';
    }

    public static final Comparator<MqttsnMetricSample> TIMESTAMP = (o1, o2) -> (int) (o1.timestamp - o2.timestamp);
}
