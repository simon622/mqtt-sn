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

package org.slj.mqtt.sn.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.spi.INetworkAddressRegistry;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;
import org.slj.mqtt.sn.spi.NetworkRegistryException;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class NetworkAddressRegistry implements INetworkAddressRegistry {

    static Logger logger = LoggerFactory.getLogger(NetworkAddressRegistry.class.getName());

    final protected Map<NetworkAddress, INetworkContext> networkRegistry;
    final protected Map<IClientIdentifierContext, INetworkContext> mqttsnContextRegistry;
    final protected Map<INetworkContext, IClientIdentifierContext> networkContextRegistry;

    final private Object mutex = new Object();

    public NetworkAddressRegistry(int initialCapacity){
        networkRegistry = new ConcurrentHashMap(initialCapacity);
        mqttsnContextRegistry = new ConcurrentHashMap(initialCapacity);
        networkContextRegistry = new ConcurrentHashMap(initialCapacity);
    }

    @Override
    public INetworkContext getContext(NetworkAddress address) throws NetworkRegistryException {
        INetworkContext context = networkRegistry.get(address);
        logger.debug("getting network context from RAM registry by address {} -> {}", address, context);
        return context;
    }

    @Override
    public INetworkContext getContext(IClientIdentifierContext sessionContext) {
        INetworkContext context = mqttsnContextRegistry.get(sessionContext);
        if(context == null)
            throw new MqttsnRuntimeException("unable to get network route for session " + sessionContext);

        logger.debug("getting network context from RAM registry by session {} -> {}", sessionContext, context);
        return context;
    }

    @Override
    public IClientIdentifierContext getMqttsnContext(INetworkContext networkContext){
        IClientIdentifierContext context = networkContextRegistry.get(networkContext);
        if(context == null)
            throw new MqttsnRuntimeException("unable to get session context for network route " + networkContext);

        logger.debug("getting session context from RAM registry network route {} -> {}", networkContext, context);
        return context;
    }

    @Override
    public Optional<INetworkContext> first() throws NetworkRegistryException {
        Iterator<NetworkAddress> itr = networkRegistry.keySet().iterator();
        while(itr.hasNext()){
            NetworkAddress address = itr.next();
            INetworkContext c = networkRegistry.get(address);
            return Optional.of(c);
        }
        return Optional.empty();
    }

    @Override
    public Optional<IClientIdentifierContext> findForClientId(String clientId) {
        if(clientId == null) return null;
            return mqttsnContextRegistry.keySet().stream().
                    filter(s -> s.getId() != null).
                    filter(s -> s.getId().equals(clientId)).findFirst();
    }

    @Override
    public void putContext(INetworkContext context) {
        networkRegistry.put(context.getNetworkAddress(), context);
        synchronized(mutex){
            mutex.notifyAll();
        }
    }

    @Override
    public void bindContexts(INetworkContext context, IClientIdentifierContext sessionContext) {
        mqttsnContextRegistry.put(sessionContext, context);
        networkContextRegistry.put(context, sessionContext);
        putContext(context);
    }

    @Override
    public boolean hasBoundSessionContext(INetworkContext context){
        IClientIdentifierContext c = networkContextRegistry.get(context);
        return c != null;
    }

    @Override
    public boolean removeExistingClientId(IClientIdentifierContext clientId){

        INetworkContext context = mqttsnContextRegistry.remove(clientId);
        if(context != null){
            if(context.getNetworkAddress() != null) networkRegistry.remove(context.getNetworkAddress());
            networkContextRegistry.remove(context);
            logger.info("removing network,session & address from RAM registry - {}", clientId);
            return true;
        }
        return false;
    }

    @Override
    public long size() {
        return networkRegistry.size();
    }

    public Optional<INetworkContext> waitForContext(int time, TimeUnit unit) throws NetworkRegistryException, InterruptedException {
        synchronized(mutex){
            try {
                while(networkRegistry.isEmpty()){
                    mutex.wait(TimeUnit.MILLISECONDS.convert(time, unit));
                }
                return first();
            } catch(InterruptedException e){
                Thread.currentThread().interrupt();
                throw e;
            }
        }
    }

    public Iterator<INetworkContext> iterator() throws NetworkRegistryException {
        return networkRegistry.values().iterator();
    }

    public List<InetAddress> getAllBroadcastAddresses() throws NetworkRegistryException {
        try {
            List<InetAddress> l = new ArrayList<>();
            Enumeration<NetworkInterface> interfaces
                    = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() ||
                        !networkInterface.isUp()) {
                    continue;
                }
                networkInterface.getInterfaceAddresses().stream()
                        .map(a -> a.getBroadcast())
                        .filter(Objects::nonNull)
                        .forEach(l::add);
            }
            return l;
        } catch(SocketException e){
            throw new NetworkRegistryException(e);
        }
    }
}
