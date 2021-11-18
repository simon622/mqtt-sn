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

package org.slj.mqtt.sn.wire.version1_2.payload;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnMessageFactory;

import java.util.Arrays;

import static org.junit.Assert.*;

public class MqttsnWireTests {

    static final byte _payload = 0x10;
    static final String _path = "/topic/path";
    static final String _clientid = "client-id";
    static final int _radius = 2;
    static final int _alias = 12;
    static final int _qos = 2;
    static final int _msgId = 254;
//    static final int _msgId = MqttsnConstants.USIGNED_MAX_16;

    IMqttsnCodec codec;
    IMqttsnMessageFactory factory;

    @Before
    public void setup(){
        codec = MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2;
        factory = codec.createMessageFactory();
    }

    @Test
    public void testMqttsnAdvertise() throws MqttsnCodecException {
        IMqttsnMessage message = factory.createAdvertise(MqttsnConstants.USIGNED_MAX_8,
                MqttsnConstants.USIGNED_MAX_16);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnConnack() throws MqttsnCodecException {
        IMqttsnMessage message = factory.createConnack(MqttsnConstants.RETURN_CODE_ACCEPTED);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnConnect() throws MqttsnCodecException {

        //-- test normal length clientId
        IMqttsnMessage message = factory.createConnect(_clientid, MqttsnConstants.USIGNED_MAX_16, false, true);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnConnectLongClientId() throws MqttsnCodecException {

        //-- test very long clientId
        StringBuilder sb = new StringBuilder(1024);
        for (int i = 0; i < 1024; i++){
            sb.append("A");
        }

        IMqttsnMessage message = factory.createConnect(sb.toString(), MqttsnConstants.USIGNED_MAX_16, false, true);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnDisconnect() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createDisconnect();
        testWireMessage(message);
    }

    @Test
    public void testMqttsnDisconnectWithDuration() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createDisconnect(MqttsnConstants.USIGNED_MAX_16);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnGwinfo() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createGwinfo(MqttsnConstants.USIGNED_MAX_8, "123:123123:0:c:12:2");
        testWireMessage(message);
    }

    @Test
    public void testMqttsnPingreq() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createPingreq(_clientid);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnPingresp() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createPingresp();
        testWireMessage(message);
    }

    @Test
    public void testMqttsnPubackError() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createPuback(_alias, MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnPublishNormalTopic() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createPublish(_qos, true, false, MqttsnConstants.TOPIC_TYPE.NORMAL, _alias, payload(4));
        message.setMsgId(25);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnPublishPredefinedTopic() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createPublish(_qos, true, false, MqttsnConstants.TOPIC_TYPE.PREDEFINED, _alias, payload(4));
        testWireMessage(message);
    }

    @Test
    public void testMqttsnPublishShortTopic() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createPublish(_qos, true, false, "ab", payload(4));
        testWireMessage(message);
    }

    @Test
    public void testMqttsnPublishLong() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createPublish(_qos, true, false, MqttsnConstants.TOPIC_TYPE.PREDEFINED, _alias, payload(MqttsnConstants.USIGNED_MAX_16));
        testWireMessage(message);
    }

    @Test
    public void testMqttsnPubrel() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createPubrel();
        testWireMessage(message);
    }

    @Test
    public void testMqttsnPubrec() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createPubrec();
        testWireMessage(message);
    }

    @Test
    public void testMqttsnPuback() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createPuback(_alias, MqttsnConstants.RETURN_CODE_ACCEPTED);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnPubcomp() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createPubcomp();
        testWireMessage(message);
    }

    @Test
    public void testMqttsnRegack() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createRegack(_alias, MqttsnConstants.RETURN_CODE_ACCEPTED);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnRegackError() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createRegack(_alias, MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnRegisterPath() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createRegister(_path);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnRegisterPathWithAlias() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createRegister(_alias, _path);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnSearchGw() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createSearchGw(_radius);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnSuback() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createSuback(_qos, _alias, MqttsnConstants.RETURN_CODE_ACCEPTED);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnSubscribePath() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createSubscribe(_qos, _path);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnSubscribePredefined() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createSubscribe(_qos, MqttsnConstants.TOPIC_TYPE.PREDEFINED, _alias);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnUnsubscribe() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createUnsubscribe(_path);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnUnsubscribePredefined() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createUnsubscribe(MqttsnConstants.TOPIC_TYPE.PREDEFINED, _alias);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnWillmsg() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createWillMsg(payload(50));
        testWireMessage(message);
    }

    @Test
    public void testMqttsnWillmsgreq() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createWillMsgReq();
        testWireMessage(message);
    }

    @Test
    public void testMqttsnWillmsgresp() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createWillMsgResp(MqttsnConstants.RETURN_CODE_ACCEPTED);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnWillmsgrespError() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createWillMsgResp(MqttsnConstants.RETURN_CODE_REJECTED_CONGESTION);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnWillmsgupd() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createWillMsgupd(payload(50));
        testWireMessage(message);
    }

    @Test
    public void testMqttsnWilltopic() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createWillTopic(_qos, true, _path);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnWilltopicreq() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createWillTopicReq();
        testWireMessage(message);
    }

    @Test
    public void testMqttsnWilltopicresp() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createWillTopicResp(MqttsnConstants.RETURN_CODE_ACCEPTED);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnWilltopicrespError() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createWillTopicResp(MqttsnConstants.RETURN_CODE_SERVER_UNAVAILABLE);
        testWireMessage(message);
    }

    @Test
    public void testMqttsnWilltopicupd() throws MqttsnCodecException {

        IMqttsnMessage message = factory.createWillTopicupd(_qos, true, _path);
        testWireMessage(message);
    }

    protected void testWireMessage(IMqttsnMessage message) throws MqttsnCodecException {

        if(message.needsMsgId()){
            message.setMsgId(_msgId);
        }

        String toString = message.toString();
        byte[] arr = codec.encode(message);

        System.out.println(String.format("before [%s] -> [%s]", toString, codec.print(message)));

        IMqttsnMessage decoded = codec.decode(arr);
        String afterToString = decoded.toString();

        System.out.println(String.format("after [%s] -> [%s]", afterToString, codec.print(message)));

        //-- first ensure the toStrings match since they contain the important data fields for each type
        Assert.assertEquals("message content should match", toString, afterToString);

        //-- re-encode to ensure a full pass of all fields
        byte[] reencoded = codec.encode(decoded);
        Assert.assertArrayEquals("binary content should match", arr, reencoded);
    }

    static byte[] payload(int size){

        byte[] arr = new byte[size];
        Arrays.fill(arr, _payload);
        return arr;
    }
}