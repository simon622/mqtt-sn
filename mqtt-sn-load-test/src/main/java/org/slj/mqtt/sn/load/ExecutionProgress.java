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

package org.slj.mqtt.sn.load;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutionProgress {

    private final Object monitor = new Object();
    private final Long created;
    private Long started;
    private final AtomicInteger work = new AtomicInteger();
    private final AtomicInteger complete = new AtomicInteger();
    private Throwable error;
    private AtomicBoolean finished = new AtomicBoolean(false);
    private long maxExecutionTime;
    private TimeUnit timeUnit;
    private ExecutionInput input;

    public ExecutionProgress(ExecutionInput input){
        this.maxExecutionTime = input.getMaxWait();
        this.timeUnit = input.getMaxWaitUnit();
        this.created = System.currentTimeMillis();
        this.input = input;
    }

    public void markStarted(){
        started = System.currentTimeMillis();
    }

    public void setTotalWork(int totalWork){
        work.set(totalWork);
    }

    public void incrementProgress(int completed){
        if(started == null) markStarted();
        int val = complete.addAndGet(completed);
//System.err.println(val +  " / " + work.get());
        if(val >= work.get()){
            markFinished();
        }
    }

    public void markFinished(){
        finished.set(true);
        synchronized (monitor){
            monitor.notifyAll();
        }
    }

    public boolean isStarted(){
        return started != null;
    }

    public boolean isComplete(){
        return finished.get();
    }

    public boolean isError(){ return error != null;}

    public int getProgress(){
        return (int) (complete.get() / work.get() * 100D);
    }

    public void setError(Throwable error){
        this.error = error;
        markFinished();
    }

    public ExecutionInput getInput() {
        return input;
    }

    public Date getCreated() {
        return new Date(created);
    }

    public Date getStarted() {
        return started == null ? null : new Date(started);
    }

    public void waitForCompletion(){
        while(!isComplete()){
            synchronized (monitor) {
                try {
                    monitor.wait(timeUnit.toMillis(maxExecutionTime));
                } catch(InterruptedException e){
                    Thread.interrupted();
                }
            }
        }
    }
}
