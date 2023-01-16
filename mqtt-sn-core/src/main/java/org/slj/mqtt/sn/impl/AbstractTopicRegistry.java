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

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.model.TopicInfo;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.model.session.ITopicRegistration;
import org.slj.mqtt.sn.spi.IMqttsnTopicRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnExpectationFailedException;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.util.*;

public abstract class AbstractTopicRegistry
        extends AbstractMqttsnSessionBeanRegistry
        implements IMqttsnTopicRegistry {

    @Override
    public TopicInfo register(ISession session, String topicPath) throws MqttsnException {
        Map<String, Integer> map = getRegistrationMapInternal(session, false);
        if(map.size() >= registry.getOptions().getMaxTopicsInRegistry()){
            logger.warn("max number of registered topics reached for client {} >= {}", session, map.size());
            throw new MqttsnException("max number of registered topics reached for client");
        }
        synchronized (map){
            int alias;
            if(map.containsKey(topicPath)){
                alias = map.get(topicPath);
                addOrUpdateRegistration(session,
                        getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath), alias);
            } else {
                alias = MqttsnUtils.getNextLeaseId(map.values(), Math.max(1, registry.getOptions().getAliasStartAt()));
                addOrUpdateRegistration(session,
                        getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath), alias);
            }
            TopicInfo info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.NORMAL,  alias);
            return info;
        }
    }

    @Override
    public void register(ISession session, String topicPath, int topicAlias) throws MqttsnException {

        logger.debug("mqtt-sn topic-registry [{} -> {}] registering {} -> {}", registry.getOptions().getContextId(), session, topicPath, topicAlias);

        Map<String, Integer> map = getRegistrationMapInternal(session, false);
        if(map.containsKey(topicPath)){
            //update existing
            addOrUpdateRegistration(session,
                    getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath), topicAlias);
        } else {
            if(map.size() >= registry.getOptions().getMaxTopicsInRegistry()){
                logger.warn("max number of registered topics reached for client {} >= {}", session.getContext(), map.size());
                throw new MqttsnException("max number of registered topics reached for client");
            }
            addOrUpdateRegistration(session,
                    getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath), topicAlias);
        }
    }

    @Override
    public boolean registered(ISession session, String topicPath) throws MqttsnException {
        Map<String, Integer> map = getRegistrationMapInternal(session, false);
        return map.containsKey(getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath));
    }

    @Override
    public String topicPath(ISession session, TopicInfo topicInfo, boolean considerContext) throws MqttsnException {
        String topicPath = null;
        switch (topicInfo.getType()){
            case SHORT:
                topicPath = topicInfo.getTopicPath();
                break;
            case PREDEFINED:
                topicPath = lookupPredefined(session, topicInfo.getTopicId());
                break;
            case NORMAL:
                if(considerContext){
                    if(session == null) throw new MqttsnExpectationFailedException("<null> session cannot be considered");
                    topicPath = lookupRegistered(session, topicInfo.getTopicId());
                }
                break;
            default:
            case FULL:
                break;
        }

        if(topicPath == null) {
            logger.warn("unable to find matching topicPath in system for {} -> {}", topicInfo, session);
            throw new MqttsnExpectationFailedException("unable to find matching topicPath in system");
        }
        return getRegistry().getTopicModifier().modifyTopic(session == null ? null : session.getContext(), topicPath);
    }

    @Override
    public String lookupRegistered(ISession session, int topicAlias) throws MqttsnException {
        Map<String, Integer> map = getRegistrationMapInternal(session, false);
        synchronized (map){
            Iterator<String> itr = map.keySet().iterator();
            while(itr.hasNext()){
                String topicPath = itr.next();
                Integer i = map.get(topicPath);
                if(i != null && i.intValue() == topicAlias)
                    return getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath);
            }
        }
        return null;
    }

    @Override
    public Integer lookupRegistered(ISession session, String topicPath, boolean confirmedOnly) throws MqttsnException {
        Map<String, Integer> map = getRegistrationMapInternal(session, confirmedOnly);
        return map.get(getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath));
    }

    @Override
    public Integer lookupPredefined(ISession session, String topicPath) throws MqttsnException {
        Map<String, Integer> predefinedMap = getPredefinedTopicsForString(session);
        return predefinedMap.get(getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath));
    }

    @Override
    public String lookupPredefined(ISession session, int topicAlias) throws MqttsnException {
        Map<String, Integer> predefinedMap = getPredefinedTopicsForInteger(session);
        synchronized (predefinedMap){
            Iterator<String> itr = predefinedMap.keySet().iterator();
            while(itr.hasNext()){
                String topicPath = itr.next();
                Integer i = predefinedMap.get(topicPath);
                if(i != null && i.intValue() == topicAlias)
                    return getRegistry().getTopicModifier().modifyTopic(session == null ? null : session.getContext(), topicPath);
            }
        }
        return null;
    }

    @Override
    public TopicInfo lookup(ISession session, String topicPath, boolean confirmedOnly) throws MqttsnException {
        topicPath = getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath);

        //-- check normal first
        TopicInfo info = null;
        if(registered(session, topicPath)){
            Integer topicAlias = lookupRegistered(session, topicPath, confirmedOnly);
            if(topicAlias != null){
                info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.NORMAL, topicAlias);
            }
        }

        //-- check predefined if nothing in session registry
        if(info == null){
            Integer topicAlias = lookupPredefined(session, topicPath);
            if(topicAlias != null){
                info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.PREDEFINED, topicAlias);
            }
        }

        //-- if topicPath < 2 chars
        if(info == null){
            if(topicPath.length() <= 2){
                info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.SHORT, topicPath);
            }
        }

        logger.debug("mqtt-sn topic-registry [{} -> {}] lookup for {} found {}", registry.getOptions().getContextId(), session, topicPath, info);
        return info;
    }

    @Override
    public TopicInfo lookup(ISession session, String topicPath) throws MqttsnException {
        return lookup(session, topicPath, false);
    }

    @Override
    public TopicInfo normalize(byte topicIdType, byte[] topicData, boolean normalAsLong) throws MqttsnException {
        TopicInfo info = null;
        switch (topicIdType){
            case MqttsnConstants.TOPIC_SHORT:
                if(topicData.length != 2){
                    throw new MqttsnExpectationFailedException("short topics must be exactly 2 bytes");
                }
                else if(topicData[1] == 0x00){
                    info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.SHORT,
                            new String(new byte[]{topicData[0]}, MqttsnConstants.CHARSET));
                } else {
                    info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.SHORT, new String(topicData, MqttsnConstants.CHARSET));
                }
                break;
            case MqttsnConstants.TOPIC_NORMAL:
                if(normalAsLong){ //-- in the case of a subscribe, the normal actually means the full topic name
                    info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.NORMAL, new String(topicData, MqttsnConstants.CHARSET));
                } else {
                    if(topicData.length != 2){
                        throw new MqttsnExpectationFailedException("normal topic aliases must be exactly 2 bytes");
                    }
                    info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.NORMAL, MqttsnWireUtils.read16bit(topicData[0],topicData[1]));
                }
                break;
            case MqttsnConstants.TOPIC_FULL:
                info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.FULL, new String(topicData, MqttsnConstants.CHARSET));
                break;
            case MqttsnConstants.TOPIC_PREDEFINED:
                if(topicData.length != 2){
                    throw new MqttsnExpectationFailedException("predefined topic aliases must be exactly 2 bytes");
                }
                info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.PREDEFINED, MqttsnWireUtils.read16bit(topicData[0],topicData[1]));
                break;
        }
        return info;
    }

    protected Map<String, Integer> getRegistrationMapInternal(ISession session, boolean confirmedOnly) throws MqttsnException{
        Set<ITopicRegistration> set = getRegistrations(session);
        Map<String, Integer> map = new HashMap<>();
        Iterator<ITopicRegistration> itr = set.iterator();
        while(itr.hasNext()){
            ITopicRegistration reg = itr.next();
            if(!confirmedOnly || reg.isConfirmed()){
                map.put(reg.getTopicPath(), reg.getAliasId());
            }
        }
        return map;
    }

    protected abstract boolean addOrUpdateRegistration(ISession session, String topicPath, int alias) throws MqttsnException;

    protected abstract Map<String, Integer> getPredefinedTopicsForInteger(ISession session) throws MqttsnException;

    protected abstract Map<String, Integer> getPredefinedTopicsForString(ISession session) throws MqttsnException;
}
