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

package org.slj.mqtt.sn.load.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.load.ExecutionInput;
import org.slj.mqtt.sn.load.ExecutionProfile;
import org.slj.mqtt.sn.load.LoadTestException;
import org.slj.mqtt.sn.utils.Numbers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractLoadTestRunner extends AbstractLoadTest {

    private final int rampSeconds;
    protected final Logger logger
            = LoggerFactory.getLogger(getClass().getName());
    private Thread watchdog;
    private Long start;
    private CountDownLatch latch;
    final List<Thread> activeThreads = new ArrayList<>();
    final AtomicBoolean interrupt = new AtomicBoolean(false);
    final AtomicInteger threadCount = new AtomicInteger();
    final AtomicInteger completeCount = new AtomicInteger();
    final AtomicInteger runningCount = new AtomicInteger();
    final AtomicInteger errorCount = new AtomicInteger();

    protected ThreadFactory factory = new ThreadFactory (){
        final ThreadGroup threadGroup = new ThreadGroup("mqtt-sn-load-group");
        final Thread.UncaughtExceptionHandler handler = (t, e) -> logger.error("thread failed with; ", e);
        public Thread newThread(Runnable r) {
            Thread t = new Thread(threadGroup, r,
                    "mqtt-sn-load-thread-" + threadCount.incrementAndGet());
            t.setUncaughtExceptionHandler(handler);
            activeThreads.add(t);
            return t;
        }
    };

    public AbstractLoadTestRunner(Class<? extends ExecutionProfile> profile,
                                  int numInstances, int rampSeconds) {
        super(profile, numInstances);
        this.rampSeconds = rampSeconds;
    }

    public void start(ExecutionInput input) throws LoadTestException {

        logger.info("creating {} load test profiles with ramp up of {} seconds", numInstances, rampSeconds);
        start = System.currentTimeMillis();
        latch = new CountDownLatch(numInstances);
        List<ExecutionProfile> executingProfiles = new ArrayList<>();
        for (int i = 0; i < numInstances; i++){
            ExecutionProfile profile = createProfile(input);
            executingProfiles.add(profile);
        }
        startWatchdog();
        int perProfilePause = (rampSeconds / numInstances) * 1000;
        Iterator<ExecutionProfile> itr = executingProfiles.iterator();
        while(itr.hasNext()){
            try {
                //start all the profile and bind the progress to error handler adhering to the ramp period
                ExecutionProfile profile = itr.next();
                run(createRunner(profile));
                Thread.sleep((ThreadLocalRandom.current().nextInt(1, Math.max(perProfilePause, 10))));
            } catch(Exception e){
                throw new LoadTestException(e);
            }
        }

        try {
            if(latch.await(input.getMaxWait(), input.getMaxWaitUnit())){
                logger.info("simulation finished in {} seconds, final status ({} finished, of {} ({} errors))",
                                        (System.currentTimeMillis() - start) / 1000L, completeCount.get(), numInstances, errorCount.get());
            } else {
                logger.warn("simulations still running after cooldown - interrupting tests {} seconds",
                                input.getMaxWaitUnit().toSeconds(input.getMaxWait()));
            }
        } catch(InterruptedException e){
            throw new LoadTestException(e);
        }
    }

    protected synchronized void startWatchdog() {
        if(watchdog == null) {
            String watchdogName = "watchdog";
            final Logger watchdogLog = LoggerFactory.getLogger(watchdogName);
            watchdog = new Thread(() -> {
                while(!interrupt.get()) {
                    try {
                       synchronized (interrupt){
                           interrupt.wait(5000);
                           watchdogLog.info("running for {} seconds, current status {} of {} ({}%) profiles alive - ({} finished, of {} ({} errors))",
                                           (System.currentTimeMillis() - start) / 1000L, runningCount.get(), latch.getCount(),
                                           Numbers.round2_display(Numbers.percent(runningCount.get(), latch.getCount())), completeCount.get(), numInstances, errorCount.get());
                       }
                    } catch(InterruptedException e) {
                        watchdogLog.info("watchdog was interrupted");
                    }
                }
                watchdogLog.info("watchdog has switched off");
            });
            watchdog.setName(watchdogName);
            watchdog.setDaemon(true);
            watchdog.setPriority(Thread.MIN_PRIORITY);
            watchdog.start();
        }
    }

    abstract void run(Runnable runnable);

    protected Runnable createRunner(final ExecutionProfile profile) {
        return new ProfileRunner(profile);
    }

    private class ProfileRunner implements Runnable {

        private final ExecutionProfile profile;

        public ProfileRunner(final ExecutionProfile profile) {
            this.profile = profile;
        }

        public void run() {
            if(!interrupt.get()) {
                long start = System.currentTimeMillis();
                String oldName = Thread.currentThread().getName();
                try {
                    Thread.currentThread().setName(oldName + " " + profile.getProfileName());
                    runningCount.incrementAndGet();
                    profile.executeProfile();
                    profile.getProgress().waitForCompletion();
                } catch(Exception e){
                    logger.error("error executing test profile;", e);
                } finally {
                    try {
                        profile.shutdownProfile();
                        logger.info("finished load test profile {} in {} - success ? {}",
                                    profile.getProfileName(), System.currentTimeMillis() - start, !profile.getProgress().isError());
                    } catch (Exception e) {
                        logger.warn("error finishing load test profile", e);
                    } finally {
                        runningCount.decrementAndGet();
                        completeCount.incrementAndGet();
                        if(profile.getProgress().isError()){
                            errorCount.incrementAndGet();
                        }
                        latch.countDown();
                        Thread.currentThread().setName(oldName);
                    }
                }
            }
        }
    }
}
