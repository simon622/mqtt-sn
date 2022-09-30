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

import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnHelo;

public class MqttsnMessageRules {

    public static boolean validResponse(IMqttsnCodec codec, IMqttsnMessage request, IMqttsnMessage response) {
        int[] clz = getResponseClasses(codec, request);
        return containsInt(clz, response.getMessageType());
    }

    private static int[] getResponseClasses(IMqttsnCodec codec, IMqttsnMessage message) {

        if(!requiresResponse(codec, message)){
            return new int[0];
        }
        switch(message.getMessageType()){
            case MqttsnConstants.AUTH:
                return new int[]{ MqttsnConstants.AUTH, MqttsnConstants.CONNACK };
            case MqttsnConstants.CONNECT:
                return new int[]{ MqttsnConstants.CONNACK };
            case MqttsnConstants.PUBLISH:
                return new int[]{ MqttsnConstants.PUBACK, MqttsnConstants.PUBREC, MqttsnConstants.PUBREL, MqttsnConstants.PUBCOMP };
            case MqttsnConstants.PUBREC:
                return new int[]{ MqttsnConstants.PUBREL };
            case MqttsnConstants.PUBREL:
                return new int[]{ MqttsnConstants.PUBCOMP };
            case MqttsnConstants.SUBSCRIBE:
                return new int[]{ MqttsnConstants.SUBACK};
            case MqttsnConstants.UNSUBSCRIBE:
                return new int[]{ MqttsnConstants.UNSUBACK };
            case MqttsnConstants.REGISTER:
                return new int[]{ MqttsnConstants.REGACK };
            case MqttsnConstants.PINGREQ:
                return new int[]{ MqttsnConstants.PINGRESP };
            case MqttsnConstants.DISCONNECT:
                return new int[]{ MqttsnConstants.DISCONNECT };
            case MqttsnConstants.SEARCHGW:
                return new int[]{ MqttsnConstants.GWINFO };
            case MqttsnConstants.WILLMSGREQ:
                return new int[]{ MqttsnConstants.WILLMSG };
            case MqttsnConstants.WILLTOPICREQ:
                return new int[]{ MqttsnConstants.WILLTOPIC };
            case MqttsnConstants.WILLTOPICUPD:
                return new int[]{ MqttsnConstants.WILLTOPICRESP };
            case MqttsnConstants.WILLMSGUPD:
                return new int[]{ MqttsnConstants.WILLMSGRESP };
            case MqttsnConstants.HELO:
                return new int[]{ MqttsnConstants.HELO };
            default:
                throw new MqttsnCodecException(
                        String.format("invalid message type detected [%s], non terminal and non response!", message.getMessageName()));
        }
    }

    public static boolean isTerminalMessage(IMqttsnCodec codec, IMqttsnMessage message) {
        switch(message.getMessageType()){
            case MqttsnConstants.PUBLISH:
                return codec.getQoS(message, false) <= 0;
            case MqttsnConstants.CONNACK:
            case MqttsnConstants.PUBACK:    //we delete QoS 1 sent PUBLISH on receipt of PUBACK
            case MqttsnConstants.PUBREL:    //we delete QoS 2 sent PUBLISH on receipt of PUBREL
            case MqttsnConstants.UNSUBACK:
            case MqttsnConstants.SUBACK:
            case MqttsnConstants.ADVERTISE:
            case MqttsnConstants.REGACK:
            case MqttsnConstants.PUBCOMP:   //we delete QoS 2 received PUBLISH on receipt of PUBCOMP
            case MqttsnConstants.PINGRESP:
            case MqttsnConstants.DISCONNECT:
            case MqttsnConstants.HELO:
            case MqttsnConstants.ENCAPSMSG:
            case MqttsnConstants.GWINFO:
            case MqttsnConstants.WILLMSG:
            case MqttsnConstants.WILLMSGRESP:
            case MqttsnConstants.WILLTOPIC:
            case MqttsnConstants.WILLTOPICRESP:
                return true;
            default:
                return false;
        }
    }

    public static boolean requiresResponse(IMqttsnCodec codec, IMqttsnMessage message) {
        switch(message.getMessageType()){
            case MqttsnConstants.HELO:
                return ((MqttsnHelo)message).getUserAgent() == null;
            case MqttsnConstants.PUBLISH:
                return codec.getQoS(message, false) > 0;
            case MqttsnConstants.CONNECT:
            case MqttsnConstants.PUBREC:
            case MqttsnConstants.PUBREL:
            case MqttsnConstants.SUBSCRIBE:
            case MqttsnConstants.UNSUBSCRIBE:
            case MqttsnConstants.REGISTER:
            case MqttsnConstants.PINGREQ:
            case MqttsnConstants.DISCONNECT:
            case MqttsnConstants.SEARCHGW:
            case MqttsnConstants.WILLMSGREQ:
            case MqttsnConstants.WILLMSGUPD:
            case MqttsnConstants.WILLTOPICREQ:
            case MqttsnConstants.WILLTOPICUPD:
                return true;
            default:
                return false;
        }
    }

    public static boolean isAck(IMqttsnMessage message, boolean sending){
        switch(message.getMessageType()){
            case MqttsnConstants.CONNACK:
            case MqttsnConstants.PUBACK:
            case MqttsnConstants.PUBREC:
            case MqttsnConstants.PUBCOMP:
            case MqttsnConstants.SUBACK:
            case MqttsnConstants.UNSUBACK:
            case MqttsnConstants.REGACK:
            case MqttsnConstants.PINGRESP:
            case MqttsnConstants.HELO:
            case MqttsnConstants.SEARCHGW:
            case MqttsnConstants.WILLTOPICREQ:
            case MqttsnConstants.WILLMSGREQ:
            case MqttsnConstants.WILLTOPICRESP:
            case MqttsnConstants.WILLMSGRESP:
                return true;
            case MqttsnConstants.DISCONNECT:
                return !sending;
            default:
                return false;
        }
    }

    public static <T extends Object> boolean containsInt(int[] haystack, int needle){
        if(haystack.length == 0) return false;
        for (int i = 0; i < haystack.length; i++) {
            if(haystack[i] == needle){
                return true;
            }
        }
        return false;
    }

}
