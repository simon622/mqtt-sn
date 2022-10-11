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
import org.slj.mqtt.sn.utils.RollingList;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * A metric whose sample size is constrained to the number of samples provided by the contructor.
 * When the max. size is reached the oldest sample will be removed on in preference to the newest
 * sample. Operates in FIFO mode.
 */
public class MqttsnContrainedSizeMetric extends AbstractMqttsnMetric {
    final private List<MqttsnMetricSample> samples;
    volatile MqttsnMetricSample minValue;
    volatile MqttsnMetricSample maxValue;
    private AtomicLong total = new AtomicLong(0);

    public MqttsnContrainedSizeMetric(String name, String description,
                                      int maxSamples) {
        super(name, description);
        this.samples = new RollingList<>(maxSamples);
    }

    @Override
    public List<MqttsnMetricSample> getSamples(Date from, Date to, int max) {
        MqttsnMetricSample[] samples = getSamplesInternal(max);
        List<MqttsnMetricSample> l = Arrays.stream(samples).
                filter(s -> s.getTimestamp() > from.getTime()).
                filter(s -> s.getTimestamp() <= to.getTime()).collect(Collectors.toList());
        return l;
    }

    @Override
    public List<MqttsnMetricSample> getSamples(Date from, int max) {
        MqttsnMetricSample[] samples = getSamplesInternal(max);
        List<MqttsnMetricSample> l = Arrays.stream(samples).
                filter(s -> s.getTimestamp() > from.getTime()).collect(Collectors.toList());
        return l;
    }

    public List<MqttsnMetricSample> getSamples(Date from) {
        MqttsnMetricSample[] samples = getSamplesInternal(Integer.MAX_VALUE);
        List<MqttsnMetricSample> l = Arrays.stream(samples).
                filter(s -> s.getTimestamp() > from.getTime()).collect(Collectors.toList());
        return l;
    }

    @Override
    public void putSample(MqttsnMetricSample sample) {

        Long value = sample.getLongValue();
        if(value != null){
            if(maxValue == null){
                maxValue = sample;
            }
            if(minValue == null){
                minValue = sample;
            }

            if(sample.getLongValue() > maxValue.getLongValue()){
                maxValue = sample;
            }
            if(sample.getLongValue() < minValue.getLongValue()){
                minValue = sample;
            }
            total.addAndGet(value);
            samples.add(sample);
        }
    }

    protected MqttsnMetricSample[] getSamplesInternal(int max){
        MqttsnMetricSample[] sortedSamples = samples.toArray(
                new MqttsnMetricSample[samples.size()]);
        Arrays.sort(sortedSamples, MqttsnMetricSample.TIMESTAMP);

        //0 idx is the oldest -> youngest at end of list

        if(sortedSamples.length > max){
            MqttsnMetricSample[] arr = new MqttsnMetricSample[max];
            System.arraycopy(sortedSamples, sortedSamples.length - max, arr, 0, max);
            sortedSamples = arr;
        }
        return sortedSamples;
    }

    public MqttsnMetricSample getLastSample(){
        if(samples.isEmpty()) return null;
        MqttsnMetricSample[] sortedSamples = samples.toArray(
                new MqttsnMetricSample[samples.size()]);
        Arrays.sort(sortedSamples, MqttsnMetricSample.TIMESTAMP);
        return sortedSamples[sortedSamples.length - 1];
    }

    @Override
    public MqttsnMetricSample getMaxSample() {
        return maxValue;
    }

    @Override
    public MqttsnMetricSample getMinSample() {
        return minValue;
    }

    @Override
    public Long getTotalValue() {
        return total.longValue();
    }

    @Override
    public void increment(long by) {
        throw new UnsupportedOperationException("not supported by metric type");
    }


    @Override
    public String toString() {
        return "MqttsnContrainedSizeMetric{" +
                "minValue=" + minValue +
                ", maxValue=" + maxValue +
                ", total=" + total +
                ", samples=" + samples +
                '}';
    }
}
