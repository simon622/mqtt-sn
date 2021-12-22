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
import org.slj.mqtt.sn.model.INetworkContext;

/**
 * The message handler is delegated to by the transport layer and its job is to process
 * inbound messages and marshall into other controllers to manage state lifecycle, authentication, permission
 * etc.
 *
 * It is directly responsible for creating response messages and sending them back to the transport layer
 */
public interface IMqttsnMessageHandler<U extends IMqttsnRuntimeRegistry> extends IMqttsnService<U>{


    /**
     * Determine if a network location is considered valid for a publish -1
     * @param context - the network context from where a message originated
     * @return - true if we consider the context authorised, false otherwise
     * @throws MqttsnException - an error occurred
     */
    boolean temporaryAuthorizeContext(INetworkContext context)
            throws MqttsnException;


    /**
     * Determine if a network location and clientId are considered authorised
     * @param context - the network context from where a message originated
     * @param clientId - the clientId passed in during a CONNECT procedure
     * @return - true if we consider the context authorised, false otherwise
     * @throws MqttsnException - an error occurred
     */
    boolean authorizeContext(INetworkContext context, String clientId, int protocolVersion)
            throws MqttsnException;

    void receiveMessage(IMqttsnContext context, IMqttsnMessage message)
            throws MqttsnException;

    boolean canHandle(IMqttsnContext context, IMqttsnMessage message)
            throws MqttsnException;

    boolean validResponse(IMqttsnMessage request, IMqttsnMessage response);

    boolean requiresResponse(IMqttsnMessage message);

    boolean isTerminalMessage(IMqttsnMessage message);

    boolean isPartOfOriginatingMessage(IMqttsnMessage message);

}
