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

import org.slj.mqtt.sn.impl.AbstractMqttsnMessageRegistry;
import org.slj.mqtt.sn.model.IMqttsnDataRef;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class MqttsnInMemoryMessageRegistry
        extends AbstractMqttsnMessageRegistry {

    protected Map<IMqttsnDataRef, MessageImpl> messageLookup;

    @Override
    public synchronized void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        //all the messages to drop naturally using weak referencing
        messageLookup = new WeakHashMap<>();
        super.start(runtime);
    }

    @Override
    public boolean remove(IMqttsnDataRef messageId) throws MqttsnException {
        synchronized(messageLookup){
            return messageLookup.remove(messageId) != null;
        }
    }

    @Override
    protected IMqttsnDataRef storeInternal(IMqttsnDataRef ref, MessageImpl message) {
        synchronized(messageLookup){
            messageLookup.put(ref, message);
            return ref;
        }
    }

    @Override
    protected MessageImpl readInternal(IMqttsnDataRef messageId) {
        return messageLookup.get(messageId);
    }

    @Override
    public long size() {
        return messageLookup.size();
    }

    @Override
    public void tidy() {
        long now = System.currentTimeMillis();
        synchronized (messageLookup){
            Iterator<IMqttsnDataRef> itr = messageLookup.keySet().iterator();
            while(itr.hasNext()){
                IMqttsnDataRef id = itr.next();
                if(id != null){
                    MessageImpl m = messageLookup.get(id);
                    if(m.getExpires() < now){
                        logger.info("expiring message {}", id);
                        itr.remove();
                    }
                }
            }
        }
    }
}
