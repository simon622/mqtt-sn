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

import org.slj.mqtt.sn.spi.IMqttsnClientIdFactory;


/**
 * Simple implementation of the clientID factory where the Client Identifier seed is used
 * as the actual clientId for the purposes of connection and thus the resolved version is
 * the same.
 *
 * Input Seed -> 'MyClientId'
 * Runtime Output -> 'MyClientId'
 * Connect Value -> 'MyClientId'
 */
public class MqttsnDefaultClientIdFactory implements IMqttsnClientIdFactory {

    @Override
    public String createClientId(String clientIdSeed) {
        return clientIdSeed;
    }

    @Override
    public String resolvedClientId(String clientIdTokenOrInput) {
        return clientIdTokenOrInput;
    }
}
