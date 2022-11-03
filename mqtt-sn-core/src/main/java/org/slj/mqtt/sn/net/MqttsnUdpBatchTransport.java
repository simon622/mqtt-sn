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

package org.slj.mqtt.sn.net;

import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.LinkedBlockingQueue;

public class MqttsnUdpBatchTransport extends MqttsnUdpTransport {

    protected LinkedBlockingQueue<DatagramPacket> queue;
    private Thread senderThread;

    public MqttsnUdpBatchTransport(MqttsnUdpOptions udpOptions, int queueSize) {
        super(udpOptions);
        queue = new LinkedBlockingQueue(queueSize);
    }

    @Override
    public synchronized void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        initSender();
    }

    private void initSender(){
        if(senderThread == null){
            logger.info("starting udp datagram batching sender..");
            senderThread = new Thread(() -> {
                while(running){
                    try {
                        DatagramPacket packet = queue.take();
                        if(packet != null) {
                            MqttsnUdpBatchTransport.super.send(packet);
                        }
                    }
                    catch(InterruptedException e){
                        Thread.currentThread().interrupt();
                        logger.warn("batched sending interrupted");
                    }
                    catch(Exception e){
                        logger.error("error on sending thread", e);
                    }
                }
            }, "mqtt-sn-sender");
            senderThread.start();
            senderThread.setPriority(Thread.NORM_PRIORITY);
        }
    }

    @Override
    public void stop() throws MqttsnException {
        super.stop();
        senderThread.interrupt();
        senderThread = null;
    }

    @Override
    protected void send(DatagramPacket packet) throws IOException {
        try {
            queue.put(packet);
        } catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}
