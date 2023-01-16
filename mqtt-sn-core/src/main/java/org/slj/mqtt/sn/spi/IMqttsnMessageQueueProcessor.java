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

/**
 * The job of the queue processor is to (when requested) interact with a remote contexts' queue, processing
 * the next message from the HEAD of the queue, handling any topic registration, session state, marking
 * messages inflight and finally returning an indicator as to what should happen when the processing of
 * the next message is complete. Upon dealing with the next message, whether successful or not, the processor
 * needs to return an indiction;
 *
 *  REMOVE_PROCESS - The queue is empty and the context no longer needs further processing
 *  BACKOFF_PROCESS - The queue is not empty, come back after a backend to try again. Repeating this return type for the same context
 *                      will yield an exponential backoff
 *  REPROCESS (continue) - The queue is not empty, (where possible) call me back immediately to process again
 *
 */
@MqttsnService
public interface IMqttsnMessageQueueProcessor
            extends IMqttsnService {

    enum RESULT {
        REMOVE_PROCESS,
        BACKOFF_PROCESS,
        REPROCESS
    }

    RESULT process(IClientIdentifierContext context) throws MqttsnException ;
}
