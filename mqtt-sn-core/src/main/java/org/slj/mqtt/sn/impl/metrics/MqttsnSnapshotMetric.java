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

/**
 * Will take a snapshot using the supplied snapshot interface at the frequency imposed
 */
public class MqttsnSnapshotMetric
        extends MqttsnTemporalMetric {

    private final Snapshot snapshot;

    public MqttsnSnapshotMetric(String name, String description, int maxSamples, long frequency, Snapshot snapshot) {
        super(name, description, maxSamples, frequency);
        this.snapshot = snapshot;
    }

    @Override
    public void putSample(MqttsnMetricSample sample) {
        throw new UnsupportedOperationException("sampled metrics cannot register samples created externally, please use the increment method");
    }

    @Override
    protected void sample() {
        long value = snapshot.snapshot();
        super.putSample(new MqttsnMetricSample(System.currentTimeMillis(), value));
    }

    public interface Snapshot {
        long snapshot();
    }
}
