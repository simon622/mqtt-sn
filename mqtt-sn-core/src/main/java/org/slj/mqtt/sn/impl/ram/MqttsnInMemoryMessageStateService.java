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
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.InflightMessage;
import org.slj.mqtt.sn.spi.IMqttsnOriginatingMessageSource;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.Pair;

import java.util.*;

public class MqttsnInMemoryMessageStateService
        extends AbstractMqttsnMessageStateService {

    protected Map<IClientIdentifierContext, Pair<Map<Integer, InflightMessage>, Map<Integer, InflightMessage>>> inflightMessages;

    public MqttsnInMemoryMessageStateService(boolean clientMode) {
        super(clientMode);
    }

    @Override
    public synchronized void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        inflightMessages = Collections.synchronizedMap(new HashMap());
        super.start(runtime);
    }

    @Override
    protected long doWork() {
        long nextWork = super.doWork();
        synchronized (inflightMessages) {
            Iterator<IClientIdentifierContext> itr = inflightMessages.keySet().iterator();
            while (itr.hasNext()) {
                try {
                    IClientIdentifierContext context = itr.next();
                    clearInflightInternal(context, System.currentTimeMillis());
                    Pair<Map<Integer, InflightMessage>, Map<Integer, InflightMessage>> pair =
                            inflightMessages.get(context);
                    if(pair != null && pair.getLeft().size() == 0 && pair.getRight().size() == 0){
                        logger.debug("removing inflight key for context {}", context);
                        itr.remove();
                    }
                } catch(MqttsnException e){
                    logger.warn("error occurred during inflight eviction run;", e);
                }
            }
        }
        return nextWork;
    }

    @Override
    public void clear(IClientIdentifierContext context) throws MqttsnException{
        inflightMessages.remove(context);
    }

    @Override
    public InflightMessage removeInflight(IClientIdentifierContext context, IMqttsnOriginatingMessageSource source, Integer packetId) {
        Map<Integer, InflightMessage> map = getInflightMessages(context, source);
        return map.remove(packetId);
    }

    @Override
    protected void addInflightMessage(IClientIdentifierContext context, Integer messageId, InflightMessage message) {
        Map<Integer, InflightMessage> map = getInflightMessages(context, message.getOriginatingMessageSource());
        synchronized (map){
            map.put(messageId, message);
        }
    }

    @Override
    protected InflightMessage getInflightMessage(IClientIdentifierContext context, IMqttsnOriginatingMessageSource source, Integer packetId) {
        return getInflightMessages(context, source).get(packetId);
    }

    @Override
    protected boolean inflightExists(IClientIdentifierContext context, IMqttsnOriginatingMessageSource source, Integer packetId) {
        boolean exists = getInflightMessages(context, source).containsKey(packetId);
        logger.debug("context {} -> inflight exists for id {} ? {}", context, packetId, exists);
        return exists;
    }

    @Override
    public Map<Integer, InflightMessage> getInflightMessages(IClientIdentifierContext context, IMqttsnOriginatingMessageSource source) {
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
        logger.debug("inflight for {} is {}", context, pair);

        //left is sending right is receiving
        return source == IMqttsnOriginatingMessageSource.LOCAL ? pair.getLeft() : pair.getRight();
    }

    public List<IClientIdentifierContext> getActiveInflights(){
        List<IClientIdentifierContext> l = new ArrayList<>();
        synchronized (inflightMessages){
            Iterator<IClientIdentifierContext> itr = inflightMessages.keySet().iterator();
            while(itr.hasNext()){
                IClientIdentifierContext c = itr.next();
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
