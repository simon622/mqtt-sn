/*
 * Copyright (c) 2020 Simon Johnson <simon622 AT gmail DOT com>
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
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.util.*;
import java.util.logging.Level;

public class MqttsnInMemoryMessageStateService <T extends IMqttsnRuntimeRegistry>
        extends AbstractMqttsnMessageStateService<T> {

    protected Map<IMqttsnContext, Map<Integer, InflightMessage>> inflightMessages;

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
    public void clear(IMqttsnContext context) {
        inflightMessages.remove(context);
    }

    @Override
    public void clearAll() {
        inflightMessages.clear();
    }

    @Override
    public InflightMessage removeInflight(IMqttsnContext context, int msgId) throws MqttsnException {
        Map<Integer, InflightMessage> map = getInflightMessages(context);
        return map.remove(msgId);
    }

    @Override
    protected void addInflightMessage(IMqttsnContext context, Integer messageId, InflightMessage message) throws MqttsnException {
        Map<Integer, InflightMessage> map = getInflightMessages(context);
        synchronized (map){
            map.put(messageId, message);
        }
    }

    @Override
    protected InflightMessage getInflightMessage(IMqttsnContext context, Integer messageId) throws MqttsnException {
        return getInflightMessages(context).get(messageId);
    }

    @Override
    protected boolean inflightExists(IMqttsnContext context, Integer messageId) throws MqttsnException {
        return getInflightMessages(context).containsKey(messageId);
    }

    @Override
    protected Map<Integer, InflightMessage> getInflightMessages(IMqttsnContext context) {
        Map<Integer, InflightMessage> map = inflightMessages.get(context);
        if(map == null){
            synchronized (this){
                if((map = inflightMessages.get(context)) == null){
                    map = new HashMap<>();
                    inflightMessages.put(context, map);
                }
            }
        }
        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, String.format("inflight for [%s] is [%s]", context, Objects.toString(map)));
        }
        return map;
    }

    @Override
    protected String getDaemonName() {
        return "message-state";
    }
}