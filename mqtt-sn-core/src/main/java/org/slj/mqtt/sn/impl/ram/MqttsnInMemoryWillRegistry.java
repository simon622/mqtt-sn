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
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.IMqttsnWillData;
import org.slj.mqtt.sn.model.session.impl.MqttsnWillDataImpl;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class MqttsnInMemoryWillRegistry
        extends AbstractWillRegistry {

    @Override
    public void updateWillTopic(IMqttsnSession session, String topicPath, int qos, boolean retained) {

        MqttsnWillDataImpl willData = getWillMessageInternal(session);
        if(willData == null){
            synchronized (this){
                if(willData == null){
                    willData = new MqttsnWillDataImpl();
                    setWillMessage(session, willData);
                }
            }
        }
        if(willData != null){
            willData.setQos(qos);
            willData.setRetained(retained);
            willData.setTopicPath(new TopicPath(topicPath));
        }

        logger.log(Level.INFO, String.format("updating will data for [%s], becomes [%s]", session, willData));
    }

    @Override
    public void updateWillMessage(IMqttsnSession session, byte[] data) {
        MqttsnWillDataImpl willData = getWillMessageInternal(session);
        if(willData == null){
            synchronized (this){
                if(willData == null){
                    willData = new MqttsnWillDataImpl();
                    setWillMessage(session, willData);
                }
            }
        }
        if(willData != null){
            willData.setData(data);
        }

        logger.log(Level.INFO, String.format("updating will data for [%s], becomes [%s]", session, willData));
    }

    @Override
    public void setWillMessage(IMqttsnSession session, IMqttsnWillData willData) {
        getSessionBean(session).setWillData(willData);
    }

    @Override
    public IMqttsnWillData getWillMessage(IMqttsnSession session) {
        return getSessionBean(session).getWillData();
    }

    public MqttsnWillDataImpl getWillMessageInternal(IMqttsnSession session) {
        return (MqttsnWillDataImpl) getSessionBean(session).getWillData();
    }

    @Override
    public boolean hasWillMessage(IMqttsnSession session) {
        return getWillMessage(session) != null;
    }

    @Override
    public void clear(IMqttsnSession session) {
        getSessionBean(session).setWillData(null);
    }
}