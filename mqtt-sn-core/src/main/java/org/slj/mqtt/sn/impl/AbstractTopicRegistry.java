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
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.TopicInfo;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public abstract class AbstractTopicRegistry <T extends IMqttsnRuntimeRegistry>
        extends AbstractRationalTopicService<T>
        implements IMqttsnTopicRegistry<T> {

    @Override
    public TopicInfo register(IMqttsnContext context, String topicPath) throws MqttsnException {
        Map<String, Integer> map = getRegistrationsInternal(context, false);
        if(map.size() >= registry.getOptions().getMaxTopicsInRegistry()){
            logger.log(Level.WARNING, String.format("max number of registered topics reached for client [%s] >= [%s]", context, map.size()));
            throw new MqttsnException("max number of registered topics reached for client");
        }
        synchronized (map){
            int alias = 0;
            if(map.containsKey(topicPath)){
                alias = map.get(topicPath);
                addOrUpdateRegistration(context, rationalizeTopic(context, topicPath), alias);
            } else {
                alias = MqttsnUtils.getNextLeaseId(map.values(), Math.max(1, registry.getOptions().getAliasStartAt()));
                addOrUpdateRegistration(context, rationalizeTopic(context, topicPath), alias);
            }
            TopicInfo info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.NORMAL,  alias);
            return info;
        }
    }

    @Override
    public void register(IMqttsnContext context, String topicPath, int topicAlias) throws MqttsnException {

        logger.log(Level.INFO, String.format("registering topic path [%s] -> [%s]", topicPath, topicAlias));
        Map<String, Integer> map = getRegistrationsInternal(context, false);
        if(map.containsKey(topicPath)){
            //update existing
            addOrUpdateRegistration(context, rationalizeTopic(context, topicPath), topicAlias);
        } else {
            if(map.size() >= registry.getOptions().getMaxTopicsInRegistry()){
                logger.log(Level.WARNING, String.format("max number of registered topics reached for client [%s] >= [%s]", context, map.size()));
                throw new MqttsnException("max number of registered topics reached for client");
            }
            addOrUpdateRegistration(context, rationalizeTopic(context, topicPath), topicAlias);
        }
    }

    @Override
    public boolean registered(IMqttsnContext context, String topicPath) throws MqttsnException {
        Map<String, Integer> map = getRegistrationsInternal(context, false);
        return map.containsKey(rationalizeTopic(context, topicPath));
    }

    @Override
    public String topicPath(IMqttsnContext context, TopicInfo topicInfo, boolean considerContext) throws MqttsnException {
        String topicPath = null;
        switch (topicInfo.getType()){
            case SHORT:
                topicPath = topicInfo.getTopicPath();
                break;
            case PREDEFINED:
                topicPath = lookupPredefined(context, topicInfo.getTopicId());
                break;
            case NORMAL:
                if(considerContext){
                    if(context == null) throw new MqttsnExpectationFailedException("<null> context cannot be considered");
                    topicPath = lookupRegistered(context, topicInfo.getTopicId());
                }
                break;
            default:
            case RESERVED:
                break;
        }

        if(topicPath == null) {
            logger.log(Level.WARNING, String.format("unable to find matching topicPath in system for [%s] -> [%s], available was [%s]",
                    topicInfo, context, Objects.toString(getRegistrationsInternal(context, false))));
            throw new MqttsnExpectationFailedException("unable to find matching topicPath in system");
        }
        return rationalizeTopic(context, topicPath);
    }

    @Override
    public String lookupRegistered(IMqttsnContext context, int topicAlias) throws MqttsnException {
        Map<String, Integer> map = getRegistrationsInternal(context, false);
        synchronized (map){
            Iterator<String> itr = map.keySet().iterator();
            while(itr.hasNext()){
                String topicPath = itr.next();
                Integer i = map.get(topicPath);
                if(i != null && i.intValue() == topicAlias)
                    return rationalizeTopic(context, topicPath);
            }
        }
        return null;
    }

    @Override
    public Integer lookupRegistered(IMqttsnContext context, String topicPath, boolean confirmedOnly) throws MqttsnException {
        Map<String, Integer> map = getRegistrationsInternal(context, confirmedOnly);
        return map.get(rationalizeTopic(context, topicPath));
    }

    @Override
    public Integer lookupRegistered(IMqttsnContext context, String topicPath) throws MqttsnException {
        Map<String, Integer> map = getRegistrationsInternal(context, false);
        return map.get(rationalizeTopic(context, topicPath));
    }

    @Override
    public Integer lookupPredefined(IMqttsnContext context, String topicPath) throws MqttsnException {
        Map<String, Integer> predefinedMap = getPredefinedTopicsForString(context);
        return predefinedMap.get(rationalizeTopic(context, topicPath));
    }

    @Override
    public String lookupPredefined(IMqttsnContext context, int topicAlias) throws MqttsnException {
        Map<String, Integer> predefinedMap = getPredefinedTopicsForInteger(context);
        synchronized (predefinedMap){
            Iterator<String> itr = predefinedMap.keySet().iterator();
            while(itr.hasNext()){
                String topicPath = itr.next();
                Integer i = predefinedMap.get(topicPath);
                if(i != null && i.intValue() == topicAlias)
                    return rationalizeTopic(context, topicPath);
            }
        }
        return null;
    }

    @Override
    public TopicInfo lookup(IMqttsnContext context, String topicPath, boolean confirmedOnly) throws MqttsnException {
        topicPath = rationalizeTopic(context, topicPath);

        //-- check normal first
        TopicInfo info = null;
        if(registered(context, topicPath)){
            Integer topicAlias = lookupRegistered(context, topicPath, confirmedOnly);
            if(topicAlias != null){
                info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.NORMAL, topicAlias);
            }
        }

        //-- check predefined if nothing in session registry
        if(info == null){
            Integer topicAlias = lookupPredefined(context, topicPath);
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

        logger.log(Level.INFO, String.format("topic-registry lookup for [%s] => [%s] found [%s]", context, topicPath, info));
        return info;
    }

    @Override
    public TopicInfo lookup(IMqttsnContext context, String topicPath) throws MqttsnException {
        return lookup(context, topicPath, false);
    }

    @Override
    public TopicInfo normalize(byte topicIdType, byte[] topicData, boolean normalAsLong) throws MqttsnException {
        TopicInfo info = null;
        switch (topicIdType){
            case MqttsnConstants.TOPIC_SHORT:
                if(topicData.length != 2){
                    throw new MqttsnExpectationFailedException("short topics must be exactly 2 bytes");
                }
                info = new TopicInfo(MqttsnConstants.TOPIC_TYPE.SHORT, new String(topicData, MqttsnConstants.CHARSET));
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

    protected abstract boolean addOrUpdateRegistration(IMqttsnContext context, String topicPath, int alias) throws MqttsnException;

    protected abstract Map<String, Integer> getRegistrationsInternal(IMqttsnContext context, boolean confirmedOnly) throws MqttsnException;

    protected abstract Map<String, Integer> getPredefinedTopicsForInteger(IMqttsnContext context) throws MqttsnException;

    protected abstract Map<String, Integer> getPredefinedTopicsForString(IMqttsnContext context) throws MqttsnException;
}
