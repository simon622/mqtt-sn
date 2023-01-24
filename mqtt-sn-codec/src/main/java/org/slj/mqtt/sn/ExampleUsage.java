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

package org.slj.mqtt.sn;

import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnMessageFactory;

public class ExampleUsage {

    public static void main(String[] args) throws Exception {

        //-- select the version of the protocol you wish to use. The versions
        //-- registered on the interface are thread-safe, pre-constructed singletons.
        //-- you can also manually construct your codecs and manage their lifecycle if you so desire.
        IMqttsnCodec mqttsnVersion1_2 = MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2;

        //-- each codec supplies a message factory allowing convenient construction of messages for use
        //-- in your application.
        IMqttsnMessageFactory factory = mqttsnVersion1_2.createMessageFactory();

        //-- construct a connect message with your required configuration
        IMqttsnMessage connect =
                factory.createConnect("testClientId", 60, false, false,true, 1024, 0, 0);

        System.out.println(connect);

        //-- encode the connect message for wire transport...
        byte[] arr = mqttsnVersion1_2.encode(connect);

        //-- then on the other side of the wire..
        connect = mqttsnVersion1_2.decode(arr);

        System.out.println(connect);
    }
}
