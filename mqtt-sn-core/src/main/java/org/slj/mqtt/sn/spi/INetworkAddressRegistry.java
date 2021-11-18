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
import org.slj.mqtt.sn.net.NetworkAddress;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * The network registry maintains a list of known network contexts against a remote address ({@link NetworkAddress}).
 * It exposes functionality to wait for discovered contexts as well as returning a list of valid broadcast addresses.
 */
public interface INetworkAddressRegistry {

    INetworkContext getContext(NetworkAddress address) throws NetworkRegistryException ;

    INetworkContext getContext(IMqttsnContext sessionContext);

    IMqttsnContext getSessionContext(INetworkContext networkContext);

    Optional<INetworkContext> first() throws NetworkRegistryException ;

    void putContext(INetworkContext context) throws NetworkRegistryException ;

    void bindContexts(INetworkContext context, IMqttsnContext sessionContext);

    Optional<INetworkContext> waitForContext(int time, TimeUnit unit) throws InterruptedException, NetworkRegistryException;

    List<InetAddress> getAllBroadcastAddresses() throws NetworkRegistryException ;

    Iterator<INetworkContext> iterator() throws NetworkRegistryException ;

    boolean removeExistingClientId(String clientId);

    boolean hasBoundSessionContext(INetworkContext context);

    Optional<IMqttsnContext> findForClientId(String clientId);
}
