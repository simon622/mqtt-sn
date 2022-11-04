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

import org.slj.mqtt.sn.model.IMqttsnContext;

/**
 * NOTE: this is optional
 * When contributed, this controller is used by the queue processor to check if the application is in a fit state to
 * offload messages to a remote (gateway or client), and is called back by the queue processor to be notified of
 * the queue being empty after having been flushed.
 */
@MqttsnService
public interface IMqttsnQueueProcessorStateService extends IMqttsnService {

    /**
     * The application can determine if its in a position to send publish messages to the remote context
     * @param context - The context who is to receive messages
     * @return - Is the context in a position to receive message
     * @throws MqttsnException - An error has occurred
     */
    boolean canReceive(IMqttsnContext context) throws MqttsnException;

    /**
     * Having flushed >= 1 messages to a context, this method will be notified to allow any final
     * session related actions to be taken
     * @return - Is the context in a position to receive message
     * @throws MqttsnException - An error has occurred
     */
    void queueEmpty(IMqttsnContext context) throws MqttsnException;
}
