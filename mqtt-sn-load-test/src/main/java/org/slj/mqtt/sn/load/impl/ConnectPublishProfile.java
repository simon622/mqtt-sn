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

package org.slj.mqtt.sn.load.impl;

import org.slj.mqtt.sn.client.impl.MqttsnClient;
import org.slj.mqtt.sn.load.ExecutionInput;
import org.slj.mqtt.sn.load.ExecutionProgress;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectPublishProfile extends MqttsnClientProfile {

    static Logger logger = Logger.getLogger(ConnectPublishProfile.class.getName());
    static final int KEEP_ALIVE = 240;

    @Override
    public ExecutionProgress initializeProfile(ExecutionInput input) {

        super.initializeProfile(input);
        ExecutionProgress progress = new ExecutionProgress(input);
        return progress;
    }

    @Override
    protected void executeInternal(ExecutionProgress progress) {

        try {
            MqttsnClient client = createOrGetClient();
            progress.setTotalWork(1 + getClientInput().messageCount);
            bindReceiveLatch();
            bindSendLatch();
            bindFailedLatch();

            String topicPath = getClientInput().topic;
            client.connect(KEEP_ALIVE, true);
            progress.incrementProgress(1);

            String message = "%s";

            for (int i = 0; i < getClientInput().messageCount; i++){
                client.publish(topicPath, Math.min(2, Math.max(getClientInput().qos, 0)), false,
                        String.format(message, System.currentTimeMillis()).getBytes());
            }
            progress.waitForCompletion();

        } catch(Exception e){
            logger.log(Level.SEVERE, "error detected", e);
            progress.setError(e);
        }
    }
}
