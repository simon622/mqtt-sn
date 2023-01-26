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

package org.slj.mqtt.sn.wire.version2_0.payload;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.wire.version1_2.payload.Mqttsn1_2WireTests;

public class Mqttsn2_0WireTests extends Mqttsn1_2WireTests {

    @Before
    public void setup(){
        codec = MqttsnCodecs.MQTTSN_CODEC_VERSION_2_0;
        factory = codec.createMessageFactory();
    }

    @Test
    public void testMqttsnConnect() throws MqttsnCodecException {
        IMqttsnMessage message = factory.createConnect("THIS-IS-CLIENT-ID",98, false, false, 500, 4, 5000);
        testWireMessage(message);

        message = factory.createConnect("THIS-IS-CLIENT-ID",98, false, false, 500, 0, 5000);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnConnectNoClientId() throws MqttsnCodecException {
        IMqttsnMessage message = factory.createConnect("",98, false, false, 500, 4, 5000);
        testWireMessage(message);

        message = factory.createConnect("",98, false, false, 500, 0, 5000);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnConnack() throws MqttsnCodecException {
        IMqttsnMessage message = factory.createConnack(MqttsnConstants.RETURN_CODE_ACCEPTED, true, "XXXXX", 240 * 240);
        testWireMessage(message);

        message = factory.createConnack(MqttsnConstants.RETURN_CODE_ACCEPTED, false, null, 0);
        testWireMessage(message);
    }

    @Test
    public void testUnsuback() throws MqttsnCodecException {
        IMqttsnMessage message = factory.createUnsuback(45);
        testWireMessage(message);
    }

    @Test
    public void testSubscribe() throws MqttsnCodecException {
        IMqttsnMessage message = factory.createSubscribe(1, "this/is/a/long/topicname");
        testWireMessage(message);

        message = factory.createSubscribe(1, "ab");
        testWireMessage(message);

        message = factory.createSubscribe(1, MqttsnConstants.TOPIC_TYPE.NORMAL, 12);
        testWireMessage(message);

        message = factory.createSubscribe(1, MqttsnConstants.TOPIC_TYPE.PREDEFINED, 23);
        testWireMessage(message);
    }

    @Test
    public void testUnsubscribe() throws MqttsnCodecException {
        IMqttsnMessage message = factory.createUnsubscribe("this/is/a/long/topicname");
        testWireMessage(message);

        message = factory.createUnsubscribe("ab");
        testWireMessage(message);

        message = factory.createUnsubscribe(MqttsnConstants.TOPIC_TYPE.NORMAL, 12);
        testWireMessage(message);

        message = factory.createUnsubscribe(MqttsnConstants.TOPIC_TYPE.PREDEFINED, 23);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnDisconnectWithSessionExpiry() throws MqttsnCodecException {
        IMqttsnMessage message = factory.createDisconnect(MqttsnConstants.UNSIGNED_MAX_32 / 2, false);
        testWireMessage(message);

        byte[] arr = codec.encode(message);
        MqttsnDisconnect_V2_0 disconnect = (MqttsnDisconnect_V2_0) codec.decode(arr);
        Assert.assertEquals("session expiry interval should match",
                MqttsnConstants.UNSIGNED_MAX_32 / 2, disconnect.getSessionExpiryInterval());

        Assert.assertNull("reason string should be empty", disconnect.getReasonString());
        Assert.assertEquals("reason code should be empty", 0, disconnect.getReturnCode());
    }

    @Test
    public void testMqttsnDisconnectWithReason() throws MqttsnCodecException {

        String reason  = "This is some description of an invalid reason for disconnect";
        IMqttsnMessage message = factory.createDisconnect(MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID, reason);
        testWireMessage(message);

        byte[] arr = codec.encode(message);
        MqttsnDisconnect_V2_0 disconnect = (MqttsnDisconnect_V2_0) codec.decode(arr);
        Assert.assertEquals("session expiry interval should be empty",
                0, disconnect.getSessionExpiryInterval());
        Assert.assertEquals("reason string should be match", reason, disconnect.getReasonString());
        Assert.assertEquals("reason code should match", MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID, disconnect.getReturnCode());
    }

    @Test
    public void testMqttsnPublishQoSM1ContainsProtocol() throws MqttsnCodecException {

        MqttsnPublish_V2_0 message = (MqttsnPublish_V2_0)
                factory.createPublish(MqttsnConstants.QoSM1, false, false, MqttsnConstants.TOPIC_TYPE.PREDEFINED, _alias, payload(4));
        testWireMessage(message);

        message = (MqttsnPublish_V2_0)
                factory.createPublish(MqttsnConstants.QoSM1, true, false, MqttsnConstants.TOPIC_TYPE.PREDEFINED, _alias, payload(4));
        testWireMessage(message);

        message = (MqttsnPublish_V2_0)
                factory.createPublish(MqttsnConstants.QoSM1, true, true, MqttsnConstants.TOPIC_TYPE.PREDEFINED, _alias, payload(4));
        testWireMessage(message);

        message = (MqttsnPublish_V2_0)
                factory.createPublish(MqttsnConstants.QoSM1, true, true, "ab", payload(4));
        testWireMessage(message);
    }

    @Test
    public void testMqttsnDisconnectSimple() throws MqttsnCodecException {

        MqttsnDisconnect_V2_0 message = (MqttsnDisconnect_V2_0)
                factory.createDisconnect();
        testWireMessage(message);
    }

    @Test
    public void testMqttsnDisconnectWithSEI() throws MqttsnCodecException {

        MqttsnDisconnect_V2_0 message = (MqttsnDisconnect_V2_0)
                factory.createDisconnect(6006, true);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnDisconnectWithReturnCodeAndReason() throws MqttsnCodecException {

        MqttsnDisconnect_V2_0 message = (MqttsnDisconnect_V2_0)
                factory.createDisconnect(MqttsnConstants.RETURN_CODE_SERVER_UNAVAILABLE, "The server is presently unavailable at this time");
        testWireMessage(message);
    }

    @Test
    public void testMqttsnDisconnectWithRetainRegistrations() throws MqttsnCodecException {


        MqttsnDisconnect_V2_0 message = (MqttsnDisconnect_V2_0)
                factory.createDisconnect(6006, false);
        testWireMessage(message);

        message = (MqttsnDisconnect_V2_0)
                factory.createDisconnect(MqttsnConstants.UNSIGNED_MAX_32 - 1, true);
        testWireMessage(message);
    }
}