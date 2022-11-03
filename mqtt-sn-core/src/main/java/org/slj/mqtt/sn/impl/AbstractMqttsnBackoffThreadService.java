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

package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnService;

public abstract class AbstractMqttsnBackoffThreadService
        extends MqttsnService implements Runnable {

    private Thread t;
    private Object monitor = new Object();

    @Override
    public synchronized void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        initThread();
    }

    protected void initThread(){
        if(t == null){
            String name = getDaemonName();
            name = name == null ? getClass().getSimpleName().toLowerCase() : name;
            String threadName = String.format("mqtt-sn-deamon-%s-%s", name, System.identityHashCode(registry.getRuntime()));
            t = new Thread(registry.getRuntime().getThreadGroup(), this, threadName);
            t.setPriority(Thread.MIN_PRIORITY);
            t.setDaemon(true);
            t.start();
            t.setUncaughtExceptionHandler((t, e) ->
                    logger.error("uncaught error on deamon process;", e));
        }
    }

    @Override
    public void stop() throws MqttsnException {
        super.stop();
        t = null;
        synchronized (monitor){
            monitor.notifyAll();
        }
    }

    @Override
    public final void run() {
        try {
            registry.getRuntime().joinStartup();
        } catch(Exception e){
            Thread.currentThread().interrupt();
            throw new RuntimeException("error joining startup", e);
        }
        logger.info("starting thread {} processing", Thread.currentThread().getName());
        while(running &&
                !Thread.currentThread().isInterrupted()){
            long maxBackoff = doWork();
            long waitStart = System.currentTimeMillis();
            synchronized (monitor){
                try {
                    monitor.wait(Math.max(1, maxBackoff));
                } catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
            logger.debug("worker {} waited for {} in the end", Thread.currentThread().getName(), System.currentTimeMillis() - waitStart);
        }

        logger.info("stopped {} thread", Thread.currentThread().getName());
    }

    /**
     * Complete your tasks in this method.
     * WARNING, throwing an unchecked exception from this method will cause the service to shutdown
     */
    protected abstract long doWork();

    /**
     * The name of the deamon will be used in instrumentation and logging
     * @return The name of your deamon process
     */
    protected abstract String getDaemonName();
}
