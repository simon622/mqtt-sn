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

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public abstract class MqttsnTemporalMetric
        extends MqttsnContrainedSizeMetric {

    //-- keep this static as it can be shared across all samplers, dont want thread bloat
    protected static Timer timer =
            new Timer("mqtt-sn-metrics-sampler", true);

    private volatile boolean running = false;
    private TimerTask task;
    private Date runningSince;
    final private long frequency;
    private final Object lock = new Object();

    public MqttsnTemporalMetric(String name, String description, int maxSamples, long frequency) {
        super(name, description, maxSamples);
        this.frequency = frequency;
    }

    public void start(){
        if(!running){
            task = new TimerTask() {
                public void run() {
                    if(running){
                        synchronized (lock){ //in theory slow running operations could overlap
                            if(runningSince == null) runningSince = new Date();
                            sample();
                        }
                    }
                }
            };
            timer.scheduleAtFixedRate(task, 0, frequency);
            running = true;
        }
        else {
            throw new IllegalStateException("timer is already active");
        }
    }

    public synchronized void stop(){
        try {
            running = false;
            if(task != null) task.cancel();
        } finally {
            runningSince = null;
            task = null;
        }
    }

    protected abstract void sample();
}
