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

import org.slj.mqtt.sn.model.INetworkContext;

/**
 * Used to verify message integrity and provide other security services;
 */
public interface IMqttsnSecurityService {

    /**
     * Check to see if protocol integrity is enabled on the runtime
     */
    boolean protocolIntegrityEnabled();

    /**
     * Check to see if payload (publish-data) integrity is enabled on the runtime
     */
    boolean payloadIntegrityEnabled();

    /**
     * Given the bytes which contain the integrity field, read the original bytes back with integrity remove, confirming integrity
     * in the process
     * @throws MqttsnSecurityException - Payload failed integrity checks
     */
    byte[] readVerified(INetworkContext networkContext, byte[] data) throws MqttsnSecurityException;

    /**
     * Given content to protect, return payload with integrity field applied
     * @throws MqttsnSecurityException - Unable to apply integrity field to given payload
     */
    byte[] writeVerified(INetworkContext networkContext, byte[] data) throws MqttsnSecurityException;

}
