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

import org.slj.mqtt.sn.model.IMqttsnMetric;
import org.slj.mqtt.sn.model.IMqttsnMetricAlarm;
import org.slj.mqtt.sn.model.MqttsnMetricSample;
import org.slj.mqtt.sn.spi.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MqttsnMetricsService extends AbstractMqttsnService implements IMqttsnMetricsService {

    private Map<String, IMqttsnMetric> metrics = new ConcurrentHashMap<>();
    private List<IMqttsnMetricAlarm> alarms =
            Collections.synchronizedList(new ArrayList<>());

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
    }

    @Override
    public void stop() throws MqttsnException {
        try {
            metrics.values().stream().filter(w -> w instanceof MqttsnTemporalMetric).
                    forEach(w -> ((MqttsnTemporalMetric)w).stop());
        } finally {
            super.stop();
        }
    }

    @Override
    public IMqttsnMetric getMetric(String metricName) {
        if(metricName == null) throw new MqttsnRuntimeException("cannot get a <null> metric");
        IMqttsnMetric m = metrics.get(metricName);
        if(m == null)
            throw new MqttsnRuntimeException("cannot find a metric by name " + metricName);
        return m;
    }

    @Override
    public void registerMetric(IMqttsnMetric metric) {
        if(metric == null) throw new MqttsnRuntimeException("cannot register a <null> metric");
        if(metrics.containsKey(metric.getName()))  throw new MqttsnRuntimeException("unable to re-register metric");
        metrics.put(metric.getName(), metric);
        if(metric instanceof MqttsnTemporalMetric){
            ((MqttsnTemporalMetric)metric).start();
        }
    }

    @Override
    public void registerAlarm(IMqttsnMetricAlarm alarm) {
        if(alarm == null) throw new MqttsnRuntimeException("cannot register a <null> alarm");
        if(!metrics.containsKey(alarm.getMetricName()))  throw new MqttsnRuntimeException("unable to register alarm against non-existent metric");
        alarms.add(alarm);
    }

    @Override
    public Long getLatestValue(String metricName) {
        IMqttsnMetric m = getMetric(metricName);
        MqttsnMetricSample sample = m.getLastSample();
        return sample == null ? null : sample.getLongValue();
    }

    @Override
    public Long getMaxValue(String metricName) {
        IMqttsnMetric m = getMetric(metricName);
        MqttsnMetricSample sample = m.getMaxSample();
        return sample == null ? null : sample.getLongValue();
    }

    @Override
    public Long getMinValue(String metricName) {
        IMqttsnMetric m = getMetric(metricName);
        MqttsnMetricSample sample = m.getMinSample();
        return sample == null ? null : sample.getLongValue();
    }

    @Override
    public Long getTotalValue(String metricName) {
        IMqttsnMetric m = getMetric(metricName);
        return m.getTotalValue();
    }
}
