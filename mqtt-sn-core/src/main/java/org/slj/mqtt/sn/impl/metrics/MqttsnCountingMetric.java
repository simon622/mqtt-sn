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

package org.slj.mqtt.sn.impl.metrics;

import org.slj.mqtt.sn.model.MqttsnMetricSample;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Will track the values passed in over a period of time and sample the NUMBER
 */
public class MqttsnCountingMetric
        extends MqttsnTemporalMetric {

    private final AtomicLong currentCount;

    public MqttsnCountingMetric(String name, String description, int maxSamples, long frequency) {
        super(name, description, maxSamples, frequency);
        this.currentCount = new AtomicLong();
    }

    @Override
    public void putSample(MqttsnMetricSample sample) {
        throw new UnsupportedOperationException("sampled metrics cannot register samples created externally, please use the increment method");
    }

    @Override
    protected final synchronized void sample(){
        long value = currentCount.get();
        currentCount.set(0);
        super.putSample(new MqttsnMetricSample(System.currentTimeMillis(), value));
    }

    public synchronized void stop(){
        try {
            super.stop();
        } finally {
            currentCount.set(0);
        }
    }

    public void increment(long by){
        currentCount.addAndGet(by);
    }
}
