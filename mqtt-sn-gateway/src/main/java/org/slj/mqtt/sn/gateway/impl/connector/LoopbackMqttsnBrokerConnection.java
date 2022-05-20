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

package org.slj.mqtt.sn.gateway.impl.connector;

import org.slj.mqtt.sn.gateway.impl.backend.AbstractMqttsnBackendConnection;
import org.slj.mqtt.sn.gateway.spi.PublishResult;
import org.slj.mqtt.sn.gateway.spi.Result;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBackendException;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBackendOptions;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.logging.Logger;

/**
 * @author simonjohnson
 *
 */
public class LoopbackMqttsnBrokerConnection extends AbstractMqttsnBackendConnection {

    private Logger logger = Logger.getLogger(LoopbackMqttsnBrokerConnection.class.getName());
    protected MqttsnBackendOptions options;
    protected final String clientId;

    volatile boolean connected = false;

    public LoopbackMqttsnBrokerConnection(MqttsnBackendOptions options, String clientId) {
        this.options = options;
        this.clientId = clientId;
    }

    public void connect() throws MqttsnBackendException {
        connected  = true;
    }

    @Override
    public boolean isConnected() throws MqttsnBackendException {
        return connected;
    }


    @Override
    public void close() {
        connected = false;
    }

    @Override
    public PublishResult publish(IMqttsnContext context, TopicPath topicPath, int qos, boolean retained, byte[] payload, IMqttsnMessage message) throws MqttsnBackendException {
        try {
            if(connected){
                receive(topicPath.toString(), qos, retained, payload);
                return new PublishResult(Result.STATUS.SUCCESS);
            }
            return new PublishResult(Result.STATUS.ERROR);
        } catch(Exception e){
            throw new MqttsnBackendException(e);
        }
    }
}