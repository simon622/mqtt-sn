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

import org.slj.mqtt.sn.impl.AbstractWillRegistry;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.model.session.IWillData;
import org.slj.mqtt.sn.model.session.impl.WillDataImpl;
import org.slj.mqtt.sn.utils.TopicPath;

public class MqttsnInMemoryWillRegistry
        extends AbstractWillRegistry {

    @Override
    public void updateWillTopic(ISession session, String topicPath, int qos, boolean retained) {

        WillDataImpl willData = getWillMessageInternal(session);
        if(willData == null){
            synchronized (this){
                if(willData == null){
                    willData = new WillDataImpl();
                    setWillMessage(session, willData);
                }
            }
        }
        if(willData != null){
            willData.setQos(qos);
            willData.setRetained(retained);
            willData.setTopicPath(new TopicPath(topicPath));
        }

        logger.info("updating will data for {}, becomes {}", session, willData);
    }

    @Override
    public void updateWillMessage(ISession session, byte[] data) {
        WillDataImpl willData = getWillMessageInternal(session);
        if(willData == null){
            synchronized (this){
                if(willData == null){
                    willData = new WillDataImpl();
                    setWillMessage(session, willData);
                }
            }
        }
        if(willData != null){
            willData.setData(data);
        }

        logger.info("updating will data for {}, becomes {}", session, willData);
    }

    @Override
    public void setWillMessage(ISession session, IWillData willData) {
        getSessionBean(session).setWillData(willData);
    }

    @Override
    public IWillData getWillMessage(ISession session) {
        return getSessionBean(session).getWillData();
    }

    public WillDataImpl getWillMessageInternal(ISession session) {
        return (WillDataImpl) getSessionBean(session).getWillData();
    }

    @Override
    public boolean hasWillMessage(ISession session) {
        return getWillMessage(session) != null;
    }

    @Override
    public void clear(ISession session) {
        getSessionBean(session).setWillData(null);
    }
}
