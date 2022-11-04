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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnWaitToken;
import org.slj.mqtt.sn.model.TopicInfo;
import org.slj.mqtt.sn.model.session.IMqttsnQueuedPublishMessage;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.spi.*;

public class MqttsnMessageQueueProcessor
        extends AbstractMqttsnService implements IMqttsnMessageQueueProcessor {

    static Logger logger = LoggerFactory.getLogger(MqttsnMessageQueueProcessor.class.getName());

    protected boolean clientMode;

    public MqttsnMessageQueueProcessor(boolean clientMode) {
        this.clientMode = clientMode;
    }

    public RESULT process(IMqttsnContext context) throws MqttsnException {

        IMqttsnQueueProcessorStateService stateCheckService = getRegistry().getQueueProcessorStateCheckService();
        IMqttsnSession session = getSessionFromContext(context);

        //-- if the queue is empty, then something will happen to retrigger this process, ie. message in or out
        //-- so safe to remove
        long count = registry.getMessageQueue().queueSize(session);

        logger.debug("processing queue size {} on thread {} in client-mode {} for {}", count, Thread.currentThread().getName(), clientMode, context);


        if(count == 0){
            if(stateCheckService != null){
                logger.debug("notifying state service of queue empty thread {} for {}", Thread.currentThread().getName(), context);
                //-- this checks on the state of any session and if its AWAKE will lead to a PINGRESP being sent
                stateCheckService.queueEmpty(context);
            }
            return RESULT.REMOVE_PROCESS;
        }

        //-- this call checks session state to ensure the client has an active or awake session
        if(stateCheckService != null && !stateCheckService.canReceive(context)) {
            return RESULT.REMOVE_PROCESS;
        }

        //-- this checks the inflight if its > 0 we cannot send
        if(!registry.getMessageStateService().canSend(context)) {
            logger.debug("state service determined cant send at the moment {}, remove process and allow protocol processor to schedule new check", context);
            return RESULT.BACKOFF_PROCESS;
        }

        IMqttsnQueuedPublishMessage queuedMessage = registry.getMessageQueue().peek(session);
        if(queuedMessage != null){
            return processNextMessage(context);
        } else {
            return clientMode ? RESULT.REPROCESS : RESULT.REMOVE_PROCESS;
        }
    }

    /**
     * Uses the next message and establishes a register if no support topic alias's exist
     */
    protected RESULT processNextMessage(IMqttsnContext context) throws MqttsnException {

        IMqttsnSession session = getSessionFromContext(context);
        IMqttsnQueuedPublishMessage queuedMessage = registry.getMessageQueue().peek(session);
        String topicPath = queuedMessage.getData().getTopicPath();
        TopicInfo info = registry.getTopicRegistry().lookup(session, topicPath, true);
        if(info == null){
            logger.debug("need to register for delivery to {} on topic {}", session.getContext(), topicPath);
            if(!clientMode){
                //-- only the server hands out alias's
                info = registry.getTopicRegistry().register(session, topicPath);
            }
            IMqttsnMessage register = registry.getMessageFactory().createRegister(info != null ? info.getTopicId() : 0, topicPath);
            try {
                MqttsnWaitToken token = registry.getMessageStateService().sendMessage(context, register);
                if(clientMode){
                    if(token != null){
                        registry.getMessageStateService().waitForCompletion(context, token);
                    }
                }
            } catch(MqttsnExpectationFailedException e){
                logger.warn("unable to send message, try again later", e);
            }
            //-- with a register we should come back when the registration is complete and attempt delivery
            return RESULT.REPROCESS;
        } else {
            //-- only deque when we have confirmed we can deliver
            return dequeAndPublishNextMessage(context, info);
        }
    }

    protected RESULT dequeAndPublishNextMessage(IMqttsnContext context, TopicInfo info) throws MqttsnException {

        IMqttsnSession session = getSessionFromContext(context);
        IMqttsnQueuedPublishMessage queuedMessage = registry.getMessageQueue().poll(session);
        if (queuedMessage != null) {
            queuedMessage.incrementRetry();
            //-- let the reaper check on delivery
            try {
                MqttsnWaitToken token = registry.getMessageStateService().sendPublishMessage(context, info, queuedMessage);
                if (clientMode) {
                    if(token != null){
                        registry.getMessageStateService().waitForCompletion(context, token);
                        if(token.isError()){
                            //error in delivery
                            throw new MqttsnException("message was not confirmed or confirmed with error");
                        }
                    }
                }

                RESULT res = ((registry.getMessageQueue().queueSize(session) > 0) ||
                        queuedMessage.getData().getQos() == 0)  ? RESULT.REPROCESS : RESULT.REMOVE_PROCESS;
                logger.debug("sending complete returning {} for {}", res, context);
                return res;
            } catch (MqttsnException e) {
                logger.warn("encountered error dequeing publish", e);
                //-- error so back off a little
                return RESULT.BACKOFF_PROCESS;
            }
        } else {
            //-- no more messages
            return RESULT.REMOVE_PROCESS;
        }
    }

    protected IMqttsnSession getSessionFromContext(IMqttsnContext context)
            throws MqttsnNotFoundException {

        try {
            if(context == null) throw new NullPointerException("unable to session from <null> context");
            IMqttsnSession session = getRegistry().getSessionRegistry().getSession(context, false);
            if(session == null){
                throw new MqttsnNotFoundException("no session found for " + context.getId());
            }
            return session;
        }
        catch(MqttsnNotFoundException e){
            throw e;
        }
        catch(MqttsnException e){
            throw new MqttsnRuntimeException("error obtaining session", e);
        }
    }
}
