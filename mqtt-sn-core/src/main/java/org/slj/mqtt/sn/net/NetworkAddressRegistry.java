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

import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.spi.INetworkAddressRegistry;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;
import org.slj.mqtt.sn.spi.NetworkRegistryException;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NetworkAddressRegistry implements INetworkAddressRegistry {

    static Logger logger = Logger.getLogger(NetworkAddressRegistry.class.getName());

    final protected Map<NetworkAddress, INetworkContext> networkRegistry;
    final protected Map<IMqttsnContext, INetworkContext> mqttsnContextRegistry;
    final protected Map<INetworkContext, IMqttsnContext> networkContextRegistry;

    final private Object mutex = new Object();

    public NetworkAddressRegistry(int initialCapacity){
        networkRegistry = Collections.synchronizedMap(new HashMap<>(initialCapacity));
        mqttsnContextRegistry = Collections.synchronizedMap(new HashMap<>(initialCapacity));
        networkContextRegistry = Collections.synchronizedMap(new HashMap<>(initialCapacity));
    }

    @Override
    public INetworkContext getContext(NetworkAddress address) throws NetworkRegistryException {
        INetworkContext context = networkRegistry.get(address);
        logger.log(Level.FINE, String.format("getting network context from RAM registry by address [%s] -> [%s]", address, context));
        return context;
    }

    @Override
    public INetworkContext getContext(IMqttsnContext sessionContext) {
        INetworkContext context = mqttsnContextRegistry.get(sessionContext);
        if(context == null)
            throw new MqttsnRuntimeException("unable to get network route for session " + sessionContext);
        logger.log(Level.FINE, String.format("getting network context from RAM registry by session [%s] -> [%s]", sessionContext, context));
        return context;
    }

    @Override
    public IMqttsnContext getSessionContext(INetworkContext networkContext){
        IMqttsnContext context = networkContextRegistry.get(networkContext);
        if(context == null)
            throw new MqttsnRuntimeException("unable to get session context for network route " + networkContext);
        logger.log(Level.FINE, String.format("getting session context from RAM registry network route [%s] -> [%s]", networkContext, context));
        return context;
    }

    @Override
    public Optional<INetworkContext> first() throws NetworkRegistryException {
        synchronized (networkRegistry){
            Iterator<NetworkAddress> itr = networkRegistry.keySet().iterator();
            while(itr.hasNext()){
                NetworkAddress address = itr.next();
                INetworkContext c = networkRegistry.get(address);
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<IMqttsnContext> findForClientId(String clientId) {
        if(clientId == null) return null;
        synchronized (mqttsnContextRegistry){
            return mqttsnContextRegistry.keySet().stream().
                    filter(s -> s.getId() != null).
                    filter(s -> s.getId().equals(clientId)).findFirst();
        }
    }

    @Override
    public void putContext(INetworkContext context) {
        synchronized (networkRegistry){
            networkRegistry.put(context.getNetworkAddress(), context);
            logger.log(Level.INFO, String.format("adding network context to RAM registry - [%s]", context));
            synchronized(mutex){
                mutex.notifyAll();
            }
        }
    }

    @Override
    public void bindContexts(INetworkContext context, IMqttsnContext sessionContext) {
        logger.log(Level.INFO, String.format("binding network to session contexts - [%s] -> [%s]", context, sessionContext));
        synchronized (networkRegistry){
            mqttsnContextRegistry.put(sessionContext, context);
            networkContextRegistry.put(context, sessionContext);
            putContext(context);
        }
    }

    @Override
    public boolean hasBoundSessionContext(INetworkContext context){
        IMqttsnContext c = networkContextRegistry.get(context);
        return c != null;
    }

    @Override
    public boolean removeExistingClientId(String clientId){
        synchronized (mqttsnContextRegistry) {
            Iterator<IMqttsnContext> itr = mqttsnContextRegistry.keySet().iterator();
            while (itr.hasNext()) {
                IMqttsnContext m = itr.next();
                if(m.getId().equals(clientId)){
                    INetworkContext c = mqttsnContextRegistry.get(m);
                    itr.remove();
                    networkRegistry.remove(c.getNetworkAddress());
                    networkContextRegistry.remove(c);
                    logger.log(Level.INFO, String.format("removing network,session & address from RAM registry - [%s]", clientId));
                    return true;
                }
            }
        }

        return false;
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
