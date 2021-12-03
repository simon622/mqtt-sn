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

package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.model.IMqttsnContext;

/**
 * Optional - when installed it will be consulted to determine whether a remote context can perform certain
 * operations;
 *
 * CONNECT with the given clientId
 */
public interface IMqttsnAuthenticationService {

    /**
     * Is a client allowed to CONNECT successfully.
     * @param context - the context who would like to CONNECT
     * @param clientId - the client Id they provided in the CONNECT dialog
     * @return true if the client is allowed to CONNECT (yielding CONNACK ok) or false if not allowed (yielding CONNACK error)
     * @throws MqttsnException an error occurred
     */
    boolean allowConnect(IMqttsnContext context, String clientId) throws MqttsnException;

}
