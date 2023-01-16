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

package org.slj.mqtt.sn.gateway.spi.bridge;

import org.slj.mqtt.sn.gateway.spi.*;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.utils.TopicPath;

import java.io.Closeable;

public interface IProtocolBridgeConnection extends Closeable {

    boolean isConnected() throws ProtocolBridgeException;

    void close();

    DisconnectResult disconnect(IClientIdentifierContext context) throws ProtocolBridgeException;

    ConnectResult connect(IClientIdentifierContext context) throws ProtocolBridgeException;

    SubscribeResult subscribe(IClientIdentifierContext context, TopicPath topicPath) throws ProtocolBridgeException;

    UnsubscribeResult unsubscribe(IClientIdentifierContext context, TopicPath topicPath) throws ProtocolBridgeException;

    PublishResult publish(IClientIdentifierContext context, TopicPath topicPath, int qos, boolean retained, byte[] payload) throws ProtocolBridgeException;

    boolean canAccept(IClientIdentifierContext context, TopicPath topicPath, byte[] payload) throws ProtocolBridgeException;
}
