/*
 * Copyright (c) 2021-2026 Simon Johnson <simon622 AT gmail DOT com>, Ian Craggs
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

package org.slj.mqtt.sn.test;

import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.IPacketTXRXJob;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.IMqttsnTransport;
import org.slj.mqtt.sn.spi.ITransport;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.StringTable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * A transport that does nothing, used so in-VM test runtimes have a {@link IMqttsnTransport}
 * registered (required to construct network contexts) without needing a real network stack.
 *
 * ITransport is listed explicitly (in addition to IMqttsnTransport, which extends it) because
 * ServiceUtils.getServiceBind() only inspects interfaces declared directly on the implementing
 * class via reflection, and only ITransport carries the @MqttsnService binding annotation.
 */
public class NoopTestTransport implements IMqttsnTransport, ITransport {

    private volatile boolean running = false;

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        running = true;
    }

    @Override
    public void stop() throws MqttsnException {
        running = false;
    }

    @Override
    public boolean running() {
        return running;
    }

    @Override
    public void receiveFromTransport(INetworkContext context, byte[] data) throws MqttsnException {
    }

    @Override
    public Future<IPacketTXRXJob> writeToTransport(INetworkContext context, byte[] data) throws MqttsnException {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Future<IPacketTXRXJob> writeToTransportWithCallback(INetworkContext context, byte[] data, Runnable task) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void connectionLost(INetworkContext context, Throwable t) {
    }

    @Override
    public StringTable getTransportDetails() {
        return new StringTable();
    }

    @Override
    public String getName() {
        return "noop-test-transport";
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public String getDescription() {
        return "No-op transport used by in-VM tests";
    }

    @Override
    public Future<IPacketTXRXJob> writeToTransport(INetworkContext context, IMqttsnMessage message) throws MqttsnException {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Future<IPacketTXRXJob> writeToTransportWithCallback(INetworkContext context, IMqttsnMessage message, Runnable r) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void broadcast(IMqttsnMessage message) throws MqttsnException {
    }
}
