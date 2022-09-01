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

package org.slj.mqtt.sn.impl.ram;

import org.slj.mqtt.sn.impl.AbstractMqttsnMessageStateService;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.InflightMessage;
import org.slj.mqtt.sn.spi.IMqttsnOriginatingMessageSource;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.Pair;

import java.util.*;
import java.util.logging.Level;

public class MqttsnInMemoryMessageStateService <T extends IMqttsnRuntimeRegistry>
        extends AbstractMqttsnMessageStateService<T> {

    protected Map<IMqttsnContext, Pair<Map<Integer, InflightMessage>, Map<Integer, InflightMessage>>> inflightMessages;

    public MqttsnInMemoryMessageStateService(boolean clientMode) {
        super(clientMode);
    }

    @Override
    public synchronized void start(T runtime) throws MqttsnException {
        inflightMessages = Collections.synchronizedMap(new HashMap());
        super.start(runtime);
    }

    @Override
    protected long doWork() {
        long nextWork = super.doWork();
        synchronized (inflightMessages) {
            Iterator<IMqttsnContext> itr = inflightMessages.keySet().iterator();
            while (itr.hasNext()) {
                try {
                    IMqttsnContext context = itr.next();
                    clearInflightInternal(context, System.currentTimeMillis());
                } catch(MqttsnException e){
                    logger.log(Level.WARNING, "error occurred during inflight eviction run;", e);
                }
            }
        }
        return nextWork;
    }

    @Override
    public void clear(IMqttsnContext context) throws MqttsnException{
        super.clear(context);
        inflightMessages.remove(context);
    }

    @Override
    public void clearAll() {
        inflightMessages.clear();
    }

    @Override
    public InflightMessage removeInflight(IMqttsnContext context, IMqttsnOriginatingMessageSource source, Integer packetId) {
        Map<Integer, InflightMessage> map = getInflightMessages(context, source);
        return map.remove(packetId);
    }

    @Override
    protected void addInflightMessage(IMqttsnContext context, Integer messageId, InflightMessage message) {
        Map<Integer, InflightMessage> map = getInflightMessages(context, message.getOriginatingMessageSource());
        synchronized (map){
            map.put(messageId, message);
        }
    }

    @Override
    protected InflightMessage getInflightMessage(IMqttsnContext context, IMqttsnOriginatingMessageSource source, Integer packetId) {
        return getInflightMessages(context, source).get(packetId);
    }

    @Override
    protected boolean inflightExists(IMqttsnContext context, IMqttsnOriginatingMessageSource source, Integer packetId) {
        boolean exists = getInflightMessages(context, source).containsKey(packetId);
        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, String.format("context [%s] -> inflight exists for id [%s] ? [%s]", context, packetId, exists));
        }
        return exists;
    }

    @Override
    public Map<Integer, InflightMessage> getInflightMessages(IMqttsnContext context, IMqttsnOriginatingMessageSource source) {
        Pair<Map<Integer, InflightMessage>, Map<Integer, InflightMessage>> pair = inflightMessages.get(context);
        if(pair == null){
            synchronized (this){
                if((pair = inflightMessages.get(context)) == null){
                    pair = Pair.of(Collections.synchronizedMap(new HashMap<>()),
                            Collections.synchronizedMap(new HashMap<>()));
                    inflightMessages.put(context, pair);
                }
            }
        }
        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, String.format("inflight for [%s] is [%s]", context, pair));
        }

        //left is sending right is receiving
        return source == IMqttsnOriginatingMessageSource.LOCAL ? pair.getLeft() : pair.getRight();
    }

    public List<IMqttsnContext> getActiveInflights(){
        List<IMqttsnContext> l = new ArrayList<>();
        synchronized (inflightMessages){
            Iterator<IMqttsnContext> itr = inflightMessages.keySet().iterator();
            while(itr.hasNext()){
                IMqttsnContext c = itr.next();
                l.add(c);
            }
        }
        return l;
    }

    @Override
    protected String getDaemonName() {
        return "message-state";
    }
}