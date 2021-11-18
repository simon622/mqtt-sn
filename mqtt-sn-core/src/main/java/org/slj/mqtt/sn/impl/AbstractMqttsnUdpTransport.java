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

import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.nio.ByteBuffer;

public abstract class AbstractMqttsnUdpTransport<U extends IMqttsnRuntimeRegistry>
        extends AbstractMqttsnTransport<U> {

    protected final MqttsnUdpOptions options;

    public AbstractMqttsnUdpTransport(MqttsnUdpOptions options){
        this.options = options;
    }

    @Override
    public synchronized void start(U runtime) throws MqttsnException {
        try {
            super.start(runtime);
            bind();
        } catch(Exception e){
            throw new MqttsnException(e);
        }
    }

    public MqttsnUdpOptions getUdpOptions(){
        return options;
    }

    public boolean restartOnLoss(){
        return false;
    }

    protected abstract void bind() throws Exception;

    public abstract void writeToTransport(INetworkContext context, ByteBuffer buffer) throws MqttsnException ;
}
