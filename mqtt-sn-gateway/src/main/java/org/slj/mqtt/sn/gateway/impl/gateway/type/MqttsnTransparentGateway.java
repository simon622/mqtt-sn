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

package org.slj.mqtt.sn.gateway.impl.gateway.type;

import org.slj.mqtt.sn.gateway.impl.backend.AbstractMqttsnBackendService;
import org.slj.mqtt.sn.gateway.spi.connector.IMqttsnConnectorConnection;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorException;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorOptions;
import org.slj.mqtt.sn.model.IMqttsnContext;

/**
 * TODO finish ME
 */
public class MqttsnTransparentGateway extends AbstractMqttsnBackendService {

    public MqttsnTransparentGateway(MqttsnConnectorOptions options) {
        super(options);
    }

    @Override
    protected long doWork() {
        return 0;
    }

    @Override
    protected String getDaemonName() {
        return null;
    }

    @Override
    protected void close(IMqttsnConnectorConnection connection) throws MqttsnConnectorException {

    }

    @Override
    protected IMqttsnConnectorConnection getBrokerConnectionInternal(IMqttsnContext context) throws MqttsnConnectorException {
        return null;
    }

    @Override
    public boolean isConnected(IMqttsnContext context) throws MqttsnConnectorException {
        return false;
    }

    @Override
    public void reinit() throws MqttsnConnectorException {

    }

    @Override
    public void pokeQueue() throws MqttsnConnectorException {

    }
}
