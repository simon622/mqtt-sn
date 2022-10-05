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
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.IMqttsnTopicRegistration;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.util.*;
import java.util.logging.Level;

public abstract class AbstractTopicRegistry
        extends AbstractMqttsnSessionBeanRegistry
        implements IMqttsnTopicRegistry {

    @Override
    public TopicInfo register(IMqttsnSession session, String topicPath) throws MqttsnException {
        Map<String, Integer> map = getRegistrationMapInternal(session, false);
        if(map.size() >= registry.getOptions().getMaxTopicsInRegistry()){
            logger.log(Level.WARNING, String.format("max number of registered topics reached for client [%s] >= [%s]", session, map.size()));
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
    public void register(IMqttsnSession session, String topicPath, int topicAlias) throws MqttsnException {

        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, String.format("mqtt-sn topic-registry [%s -> %s] registering [%s] -> [%s]", registry.getOptions().getContextId(), session, topicPath, topicAlias));
        }

        Map<String, Integer> map = getRegistrationMapInternal(session, false);
        if(map.containsKey(topicPath)){
            //update existing
            addOrUpdateRegistration(session,
                    getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath), topicAlias);
        } else {
            if(map.size() >= registry.getOptions().getMaxTopicsInRegistry()){
                logger.log(Level.WARNING, String.format("max number of registered topics reached for client [%s] >= [%s]", session.getContext(), map.size()));
                throw new MqttsnException("max number of registered topics reached for client");
            }
            addOrUpdateRegistration(session,
                    getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath), topicAlias);
        }
    }

    @Override
    public boolean registered(IMqttsnSession session, String topicPath) throws MqttsnException {
        Map<String, Integer> map = getRegistrationMapInternal(session, false);
        return map.containsKey(getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath));
    }

    @Override
    public String topicPath(IMqttsnSession session, TopicInfo topicInfo, boolean considerContext) throws MqttsnException {
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
            logger.log(Level.WARNING, String.format("unable to find matching topicPath in system for [%s] -> [%s], available was [%s]",
                    topicInfo, session, Objects.toString(getRegistrationMapInternal(session, false))));
            throw new MqttsnExpectationFailedException("unable to find matching topicPath in system");
        }
        return getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath);
    }

    @Override
    public String lookupRegistered(IMqttsnSession session, int topicAlias) throws MqttsnException {
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
    public Integer lookupRegistered(IMqttsnSession session, String topicPath, boolean confirmedOnly) throws MqttsnException {
        Map<String, Integer> map = getRegistrationMapInternal(session, confirmedOnly);
        return map.get(getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath));
    }

    @Override
    public Integer lookupPredefined(IMqttsnSession session, String topicPath) throws MqttsnException {
        Map<String, Integer> predefinedMap = getPredefinedTopicsForString(session);
        return predefinedMap.get(getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath));
    }

    @Override
    public String lookupPredefined(IMqttsnSession session, int topicAlias) throws MqttsnException {
        Map<String, Integer> predefinedMap = getPredefinedTopicsForInteger(session);
        synchronized (predefinedMap){
            Iterator<String> itr = predefinedMap.keySet().iterator();
            while(itr.hasNext()){
                String topicPath = itr.next();
                Integer i = predefinedMap.get(topicPath);
                if(i != null && i.intValue() == topicAlias)
                    return getRegistry().getTopicModifier().modifyTopic(session.getContext(), topicPath);
            }
        }
        return null;
    }

    @Override
    public TopicInfo lookup(IMqttsnSession session, String topicPath, boolean confirmedOnly) throws MqttsnException {
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

        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, String.format("mqtt-sn topic-registry [%s -> %s] lookup for [%s] found [%s]", registry.getOptions().getContextId(), session, topicPath, info));
        }

        return info;
    }

    @Override
    public TopicInfo lookup(IMqttsnSession session, String topicPath) throws MqttsnException {
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
            case MqttsnConstants.TOPIC_PREDEFINED:
                if(topicData.length != 2){
                    throw new MqttsnExpectationFailedException("predefined topic aliases must be exactly 2 bytes");
                }
                info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.PREDEFINED, MqttsnWireUtils.read16bit(topicData[0],topicData[1]));
                break;
        }
        return info;
    }

    protected Map<String, Integer> getRegistrationMapInternal(IMqttsnSession session, boolean confirmedOnly) throws MqttsnException{
        Set<IMqttsnTopicRegistration> set = getRegistrations(session);
        Map<String, Integer> map = new HashMap<>();
        Iterator<IMqttsnTopicRegistration> itr = set.iterator();
        while(itr.hasNext()){
            IMqttsnTopicRegistration reg = itr.next();
            if(!confirmedOnly || reg.isConfirmed()){
                map.put(reg.getTopicPath(), reg.getAliasId());
            }
        }
        return map;
    }

    protected abstract boolean addOrUpdateRegistration(IMqttsnSession session, String topicPath, int alias) throws MqttsnException;

    protected abstract Map<String, Integer> getPredefinedTopicsForInteger(IMqttsnSession session) throws MqttsnException;

    protected abstract Map<String, Integer> getPredefinedTopicsForString(IMqttsnSession session) throws MqttsnException;
}
