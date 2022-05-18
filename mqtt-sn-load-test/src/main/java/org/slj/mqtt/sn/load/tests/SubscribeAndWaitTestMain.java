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

package org.slj.mqtt.sn.load.tests;

import org.slj.mqtt.sn.load.impl.ConnectSubscribeWaitProfile;
import org.slj.mqtt.sn.load.impl.MqttsnClientProfile;
import org.slj.mqtt.sn.load.runner.ThreadPerProfileLoadTestRunner;

import java.util.concurrent.TimeUnit;

public class SubscribeAndWaitTestMain {

    public static void main(String[] args) {
        try {
            System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tc] %4$s %2$s - %5$s %6$s%n");
            ThreadPerProfileLoadTestRunner runner =
                    new ThreadPerProfileLoadTestRunner(ConnectSubscribeWaitProfile.class, 250, 20);

            MqttsnClientProfile.ClientInput input =
                    new MqttsnClientProfile.ClientInput(2400, TimeUnit.SECONDS);

            input.host = "localhost";
            input.port = 2442;
            input.messageCount = 100000;
            input.topic = "test";
            input.qos = 2;
            runner.start(input);

        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
