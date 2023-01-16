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

import org.slj.mqtt.sn.impl.AbstractMqttsnSessionBeanRegistry;
import org.slj.mqtt.sn.model.MqttsnDeadLetterQueueBean;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.model.MqttsnWaitToken;
import org.slj.mqtt.sn.model.session.IQueuedPublishMessage;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.spi.IMqttsnMessageQueue;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.TransientObjectLocks;

public class MqttsnInMemoryMessageQueue
        extends AbstractMqttsnSessionBeanRegistry implements IMqttsnMessageQueue {

    protected final TransientObjectLocks locks = new TransientObjectLocks();

    @Override
    public long queueSize(ISession session) throws MqttsnException {
        return getSessionBean(session).getQueueSize();
    }

    @Override
    public final void offer(ISession session, IQueuedPublishMessage message)
            throws MqttsnException, MqttsnQueueAcceptException {

        synchronized (locks.mutex(session.getContext().getId())){
            checkQueueSizeRestrictions(session, message);
            try {
                offerInternal(session, message);
            } finally {
                if(registry.getMessageStateService() != null)
                    registry.getMessageStateService().scheduleFlush(session.getContext());
            }
        }
    }

    @Override
    public final MqttsnWaitToken offerWithToken(ISession session, IQueuedPublishMessage message)
            throws MqttsnException, MqttsnQueueAcceptException {

        synchronized (locks.mutex(session.getContext().getId())){
            checkQueueSizeRestrictions(session, message);
            try {
                MqttsnWaitToken token = MqttsnWaitToken.from(message);
                offerInternal(session, message);
                if (token != null) message.setToken(token);
                return token;
            } finally {
                registry.getMessageStateService().scheduleFlush(session.getContext());
            }
        }
    }

    protected void checkQueueSizeRestrictions(ISession session, IQueuedPublishMessage message)
            throws MqttsnException, MqttsnQueueAcceptException {
        long size;
        if((size = queueSize(session)) >= getMaxQueueSize()){
            logger.info("max queue size reached for client {} >= {}", session, size);
            getRegistry().getDeadLetterQueue().add(
                    MqttsnDeadLetterQueueBean.REASON.QUEUE_SIZE_EXCEEDED,
                    session.getContext(), message);
            throw new MqttsnQueueAcceptException("max queue size reached for client");
        }
    }

    @Override
    public void clear(ISession session)  {
        if(session != null){
            getSessionBean(session).clearMessageQueue();
        }
    }

    protected void offerInternal(ISession session, IQueuedPublishMessage message)
            throws MqttsnException, MqttsnQueueAcceptException {

        getSessionBean(session).offer(message);
    }

    @Override
    public IQueuedPublishMessage poll(ISession session) {
        return getSessionBean(session).poll();
    }

    @Override
    public IQueuedPublishMessage peek(ISession session) {
        return getSessionBean(session).peek();
    }

    protected int getMaxQueueSize() {
        return registry.getOptions().getMaxMessagesInQueue();
    }
}
