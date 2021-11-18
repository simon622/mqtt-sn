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
 * Traffic listeners can contributed to the runtime to be notified of any traffic processed by
 * the transport layer. Listeners are not able to affect the traffic in transit or the business
 * logic executed during the course of the traffic, they are merely observers.
 *
 * The listeners ~may be notified asynchronously from the application, and thus they cannot be relied
 * upon to give an absolute timeline of traffic.
 *
 */
public interface IMqttsnTrafficListener {

    /**
     * Traffic has been successfully sent by the transport layer to the context
     * @param context - the context from which the transport originated
     * @param data - the raw data
     * @param message - the data that was sent/received
     */
    void trafficSent(INetworkContext context, byte[] data, IMqttsnMessage message);

    /**
     * Traffic has been received  by the transport layer
     * @param context - the context from which the transport originated
     * @param data - the raw data
     * @param message - the data that was sent/received
     */
    void trafficReceived(INetworkContext context, byte[] data, IMqttsnMessage message);

}
