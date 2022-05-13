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

import org.slj.mqtt.sn.load.impl.ConnectPublishWaitProfile;
import org.slj.mqtt.sn.load.runner.ThreadPerProfileLoadTestRunner;
import org.slj.mqtt.sn.load.runner.ThreadPoolLoadTestRunner;

import java.util.concurrent.TimeUnit;

public class PublishAndWaitTestMain {

    public static void main(String[] args) {
        try {
            System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tc] %4$s %2$s - %5$s %6$s%n");
            ThreadPerProfileLoadTestRunner runner =
                    new ThreadPerProfileLoadTestRunner(ConnectPublishWaitProfile.class, 400, 15);

//            ThreadPoolLoadTestRunner runner =
//                    new ThreadPoolLoadTestRunner(ConnectPublishWaitProfile.class, 1000, 20);
            ConnectPublishWaitProfile.PublishAndWaitClientInput input =
                    new ConnectPublishWaitProfile.PublishAndWaitClientInput(60, TimeUnit.SECONDS);
            input.host = "localhost";
            input.port = 2442;
            input.messageCount = 10;
            runner.start(input);

        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
