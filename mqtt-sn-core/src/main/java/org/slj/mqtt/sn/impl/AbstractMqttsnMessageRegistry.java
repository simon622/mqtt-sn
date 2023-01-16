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

package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.model.IDataRef;
import org.slj.mqtt.sn.model.IntegerDataRef;
import org.slj.mqtt.sn.spi.IMqttsnMessageRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnExpectationFailedException;
import org.slj.mqtt.sn.spi.AbstractMqttsnService;

public abstract class AbstractMqttsnMessageRegistry
        extends AbstractMqttsnService implements IMqttsnMessageRegistry {

    private volatile int lastId = 0;
    private Object lock = new Object();

    @Override
    public IDataRef add(byte[] data) throws MqttsnException {
        IDataRef ref =  createNextMessageId();
        MessageImpl impl = new MessageImpl(data);
        storeInternal(ref, impl);
        return ref;
    }

    @Override
    public byte[] get(IDataRef messageId) throws MqttsnException {

        long now = System.currentTimeMillis();
        MessageImpl impl = readInternal(messageId);
        if(impl != null){
            long expires = impl.getExpires();
            if(expires < now){
                remove(messageId);
                impl = null;
            }
        }
        if(impl == null) throw new MqttsnExpectationFailedException("unable to read message by id ["+messageId+"], message not found in registry");

        return impl.getData();
    }


//    public boolean removeWhenCommitted(Integer messageId) throws MqttsnException{
//        MessageImpl msg = readInternal(messageId);
//        boolean val = false;
//        if(msg != null){
//            if(msg.isRemoveAfterRead()){
//                val = remove(messageId);
//            }
//        }
//
//        if(val) logger.log(Level.FINE, String.format("removed committed message [%s]", messageId));
//        return val;
//    }

    protected IDataRef createNextMessageId(){
        synchronized (lock){
            return new IntegerDataRef(++lastId);
        }
    }

    protected abstract IDataRef storeInternal(IDataRef ref, MessageImpl message) throws MqttsnException;

    protected abstract MessageImpl readInternal(IDataRef messageId) throws MqttsnException;

    protected static class MessageImpl {

        private long created;
        private long expires;
        private byte[] data;

        public MessageImpl(byte[] data) {
            this(data, Long.MAX_VALUE);
        }

        public MessageImpl(byte[] data, long expires) {
            this.created = System.currentTimeMillis();
            this.expires = expires;
            this.data = data;
        }

        public long getCreated() {
            return created;
        }

        public void setCreated(long created) {
            this.created = created;
        }

        public long getExpires() {
            return expires;
        }

        public void setExpires(long expires) {
            this.expires = expires;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
    }
}