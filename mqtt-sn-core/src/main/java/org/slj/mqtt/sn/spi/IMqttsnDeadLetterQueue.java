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

package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.MqttsnDeadLetterQueueBean;
import org.slj.mqtt.sn.model.session.IQueuedPublishMessage;

import java.util.List;

/**
 * The dead letter queue serves as an area that undeliverable messages reside.
 * This gives observability to failures in the runtime that may
 * otherwise go unnoticed
 */
@MqttsnService
public interface IMqttsnDeadLetterQueue extends IMqttsnService {

    void add(MqttsnDeadLetterQueueBean.REASON reason, IClientIdentifierContext context, IQueuedPublishMessage applicationMessage)
            throws MqttsnException ;

    void add(MqttsnDeadLetterQueueBean.REASON reason, String message, IClientIdentifierContext context, IQueuedPublishMessage applicationMessage)
            throws MqttsnException ;

    List<MqttsnDeadLetterQueueBean> readMostRecent(int count);

    long size();
}
