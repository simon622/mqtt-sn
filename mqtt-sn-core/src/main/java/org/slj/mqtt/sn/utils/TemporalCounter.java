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

package org.slj.mqtt.sn.utils;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class TemporalCounter {

    private final String name;
    private Date runningSince;
    private Timer timer;
    private final AtomicLong currentCount;
    private long maxValue;
    private long lastValue;
    private final long frequencyMillis;
    private volatile boolean running = false;

    public TemporalCounter(String name, long frequencyMillis) {
        this.name = name;
        this.timer = new Timer();
        this.currentCount = new AtomicLong();
        this.frequencyMillis = frequencyMillis;
    }

    public void increment(long by){
        currentCount.addAndGet(by);
    }

    public String getName() {
        return name;
    }

    public long getMaxValue() {
        return maxValue;
    }

    public long getLastValue() {
        return lastValue;
    }

    public Date getRunningSince() {
        return runningSince;
    }

    public void start(){
        if(!running){
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if(runningSince == null) runningSince = new Date();
                    long value = currentCount.get();
                    currentCount.set(0);
                    lastValue = value;
                    maxValue = Math.max(maxValue, lastValue);
                }
            }, 0, 1000);
            running = true;
        }
         else {
            throw new IllegalStateException("timer is already active");
        }
    }

    public void stop(){
        try {
            running = false;
            timer.cancel();
        } finally {
            runningSince = null;
            timer = null;
        }
    }

    @Override
    public String toString() {
        return "TemporalCounter{" +
                "name='" + name + '\'' +
                ", maxValue=" + maxValue +
                ", lastValue=" + lastValue +
                '}';
    }
}
