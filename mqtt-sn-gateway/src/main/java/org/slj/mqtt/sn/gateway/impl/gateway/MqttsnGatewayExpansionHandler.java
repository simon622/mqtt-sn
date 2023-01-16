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

package org.slj.mqtt.sn.gateway.impl.gateway;

import org.slj.mqtt.sn.PublishData;
import org.slj.mqtt.sn.gateway.spi.GatewayMetrics;
import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayExpansionHandler;
import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.IDataRef;
import org.slj.mqtt.sn.model.MqttsnDeadLetterQueueBean;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.model.session.impl.QueuedPublishMessageImpl;
import org.slj.mqtt.sn.spi.AbstractMqttsnService;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnIllegalFormatException;

import java.util.Set;


/**
 * The expansion handlers job is to take messages received from some input-source (for example a backend connector) and
 * expand these messages to those clients who hold valid subscriptions. NOTE: when there are thousands of
 * devices all subscribed to the same topics, this could yield high levels of object creation,
 * so care needs to be taken when coding these objects to ensure they are memory efficient both in size and shared
 * references.
 */
public class MqttsnGatewayExpansionHandler extends AbstractMqttsnService implements IMqttsnGatewayExpansionHandler {

    @Override
    public void receiveToSessions(String topicPath, int qos, boolean retained, byte[] payload) throws MqttsnException {
        Set<IClientIdentifierContext> recipients = null;
        try {
            recipients = getRegistry().getSubscriptionRegistry().matches(topicPath);
        } catch(MqttsnIllegalFormatException e){
            throw new MqttsnException("illegal format supplied", e);
        }

        logger.debug("receiving broker side message into [{}] sessions", recipients.size());

        IDataRef dataId = getRegistry().getMessageRegistry().add(payload);
        int successfulExpansion = 0;
        PublishData data = new PublishData(topicPath, qos, retained);

        for (IClientIdentifierContext context : recipients){
            try {
                ISession session = getRegistry().getSessionRegistry().getSession(context, false);
                int grantedQos = registry.getSubscriptionRegistry().getQos(session, topicPath);
                grantedQos = Math.min(grantedQos,qos);
                QueuedPublishMessageImpl impl = new QueuedPublishMessageImpl(dataId, data);
                impl.setGrantedQoS(grantedQos);
                if(session != null){
                    if(session.getMaxPacketSize() != 0 &&
                            payload.length + 9 > session.getMaxPacketSize()){
                        logger.warn("payload exceeded max size ({}) bytes configured by client, ignore this client [{}]", payload.length, context);
                        getRegistry().getDeadLetterQueue().add(
                                MqttsnDeadLetterQueueBean.REASON.MAX_SIZE_EXCEEDED,
                                context, impl);
                    } else {
                        try {
                            registry.getMessageQueue().offer(session, impl);
                            successfulExpansion++;
                        } catch(MqttsnQueueAcceptException e){
                            //-- the queue was full nothing to be done here
                        }
                    }
                } else {
                    logger.warn("detected <null> session state for subscription ({})", context);
                }
            } catch(MqttsnException e){
                logger.warn("detected issue for session receipt.. ignore client ({})", context);
            } finally {
                getRegistry().getMetrics().getMetric(GatewayMetrics.BACKEND_CONNECTOR_EXPANSION).increment(1);
            }
        }

        getRegistry().getMetrics().getMetric(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH_RECEIVE).increment(1);

        if(successfulExpansion == 0){
            registry.getMessageRegistry().remove(dataId);
        }
    }
}
