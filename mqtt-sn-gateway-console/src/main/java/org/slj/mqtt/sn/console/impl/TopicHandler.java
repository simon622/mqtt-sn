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

package org.slj.mqtt.sn.console.impl;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.console.http.HttpConstants;
import org.slj.mqtt.sn.console.http.HttpException;
import org.slj.mqtt.sn.console.http.HttpInternalServerError;
import org.slj.mqtt.sn.console.http.IHttpRequestResponse;
import org.slj.mqtt.sn.model.MqttsnClientCredentials;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnIllegalFormatException;

import java.io.IOException;
import java.util.*;

public class TopicHandler extends MqttsnConsoleAjaxRealmHandler {

    public TopicHandler(ObjectMapper mapper, IMqttsnRuntimeRegistry registry) {
        super(mapper, registry);
    }

    @Override
    protected void handleHttpPost(IHttpRequestResponse request) throws HttpException, IOException {

        try {
            boolean modified = false;
            if("predefined".equals(getMandatoryParameter(request, "type"))){
                Map<String, Integer> aliass = registry.getOptions().getPredefinedTopics();
                TopicForm form = readRequestBody(request, TopicForm.class);
                String topic = form.topic;
                int topicAlias = Integer.valueOf(form.topicAlias);
                if(MqttsnSpecificationValidator.isValidSubscriptionTopic(topic, 1024)){
                    if(aliass.values().contains(topicAlias) || aliass.containsKey(topic)){
                        writeMessageBeanResponse(request, HttpConstants.SC_OK, new Message("Topic already existed", false));
                    } else {
                        aliass.put(topic, topicAlias);
                        writeMessageBeanResponse(request, HttpConstants.SC_OK, new Message("New Predefined Topic Alias added", true));
                        modified = true;
                    }

                } else {
                    writeMessageBeanResponse(request, HttpConstants.SC_OK, new Message("Topic was incorrect format '"+topic+"'", false));
                }
            }
            if(modified){
                registry.getStorageService().writeRuntimeOptions(registry.getOptions());
            }
        } catch(MqttsnException e){
            throw new HttpInternalServerError("error creating client Id", e);
        }
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse request) throws HttpException, IOException {
        try {
            if(null != getParameter(request, "remove")){
                boolean modified = false;
                String topicPath = getParameter(request, "remove");
                Map<String, Integer> aliass = registry.getOptions().getPredefinedTopics();
                if(aliass.remove(topicPath) != null){
                    modified = true;
                }
                writeMessageBeanResponse(request, HttpConstants.SC_OK, new Message("Removed Client Identifier from runtime (if existed)", modified));
                if(modified){
                    registry.getStorageService().writeRuntimeOptions(registry.getOptions());
                }
            } else {
                TopicsBean bean = populateBean();
                writeJSONBeanResponse(request, HttpConstants.SC_OK, bean);
            }
        } catch(Exception e){
            throw new HttpInternalServerError("error populating bean;", e);
        }
    }

    protected TopicsBean populateBean() throws MqttsnException {
        TopicsBean bean = new TopicsBean();
        MqttsnOptions options = registry.getOptions();
        Map<String, Integer> map = options.getPredefinedTopics();
        if(map != null) map.forEach((s, integer) -> bean.addPredefined(s, integer, null));

        //-- FIX ME - this is not good for large datasets
        Set<String> paths = registry.getSubscriptionRegistry().readAllSubscribedTopicPaths();
        if(paths != null) paths.forEach((s) -> {
                    try {
                        int count = registry.getSubscriptionRegistry().matches(s).size();
                        bean.addNormalTopic(s, count, null);
                    } catch (MqttsnException | MqttsnIllegalFormatException e) {
                    }
                }
            );
        return bean;
    }

    class TopicsBean {

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = HttpConstants.DEFAULT_DATE_FORMAT)
        public Date generatedAt;

        public List<TopicBean> predefinedTopics = new ArrayList<>();
        public List<TopicBean> normalTopics = new ArrayList<>();

        public void addPredefined(String topicPath, int alias, String description){
            TopicBean bean = new TopicBean();
            bean.topicPath = topicPath;
            bean.alias = alias;
            bean.description = description;
            predefinedTopics.add(bean);
        }

        public void addNormalTopic(String topicPath, int subscriptionCount, String description){
            TopicBean bean = new TopicBean();
            bean.topicPath = topicPath;
            bean.subscriptionCount = subscriptionCount;
            bean.description = description;
            normalTopics.add(bean);
        }
    }

    class TopicBean {

        public String topicPath;
        public int subscriptionCount;
        public String description = "";
        public int alias;
    }

}

class TopicForm {

    public TopicForm() {
    }

    public String topic;
    public int topicAlias;
}


