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

import org.slj.mqtt.sn.impl.AbstractMqttsnMessageRegistry;
import org.slj.mqtt.sn.impl.AbstractTopicRegistry;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.util.*;
import java.util.logging.Level;

public class MqttsnInMemoryMessageRegistry<T extends IMqttsnRuntimeRegistry>
        extends AbstractMqttsnMessageRegistry<T> {

    protected Map<UUID, MessageImpl> messageLookup;

    @Override
    public synchronized void start(T runtime) throws MqttsnException {
        messageLookup = Collections.synchronizedMap(new HashMap<>());
        super.start(runtime);
    }

    @Override
    protected boolean remove(UUID messageId) throws MqttsnException {
        return messageLookup.remove(messageId) != null;
    }

    @Override
    protected UUID storeInternal(MessageImpl message) throws MqttsnException {
        messageLookup.put(message.getMessageId(), message);
        return message.getMessageId();
    }

    @Override
    protected MessageImpl readInternal(UUID messageId) throws MqttsnException {
        return messageLookup.get(messageId);
    }

    @Override
    public void clearAll() throws MqttsnException {
        messageLookup.clear();
    }

    @Override
    public void tidy() throws MqttsnException {
        Date d = new Date();
        synchronized (messageLookup){
            Iterator<UUID> itr = messageLookup.keySet().iterator();
            while(itr.hasNext()){
                UUID id = itr.next();
                MessageImpl m = messageLookup.get(id);
                if(m.getExpires() != null && m.getExpires().before(d)){
                    logger.log(Level.INFO, String.format("expiring message [%s]", id));
                    itr.remove();
                }
            }
        }
    }
}
