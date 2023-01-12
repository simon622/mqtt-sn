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

import org.slj.mqtt.sn.impl.AbstractMqttsnTransport;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
import org.slj.mqtt.sn.utils.StringTable;

import java.util.concurrent.Future;

/**
 * The transport layer is responsible for managing the receiving and sending of messages over some connection.
 * No session is assumed by the application and the connection is considered stateless at this point.
 * It is envisaged implementations will include UDP (with and without DTLS), TCP-IP (with and without TLS),
 * BLE and ZigBee.
 *
 * Please refer to {@link AbstractMqttsnTransport} and sub-class your own implementations
 * or choose an existing implementation out of the box.
 *
 * @see {@link MqttsnUdpTransport} for an example of an out of the box implementation.
 */
@MqttsnService(order = MqttsnService.FIRST)
public interface IMqttsnTransport extends IMqttsnService {

    void receiveFromTransport(INetworkContext context, byte[] data);

    Future<INetworkContext> writeToTransport(INetworkContext context, IMqttsnMessage message) throws MqttsnException ;

    void writeToTransportWithWork(INetworkContext context, IMqttsnMessage message, Runnable runnable) throws MqttsnException ;

    void broadcast(IMqttsnMessage message) throws MqttsnException ;

    void connectionLost(INetworkContext context, Throwable t);

    boolean restartOnLoss();

    StringTable getTransportDetails();

}
