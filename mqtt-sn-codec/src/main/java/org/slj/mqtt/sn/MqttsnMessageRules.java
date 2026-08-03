/*
 * Copyright (c) 2021-2026 Simon Johnson <simon622 AT gmail DOT com>, Ian Craggs
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

    //-- v1.2 and v2.0 packet type bytes are independently namespaced (both span roughly 0x00-0x18), so
    //-- they collide as raw values; fold in the protocol version to disambiguate a single switch.
    private static int dispatchKey(IMqttsnCodec codec, IMqttsnMessage message) {
        int msgType = message.getMessageType() & 0xFF;
        return codec.getProtocolVersion() == MqttsnConstants.PROTOCOL_VERSION_2_0 ? (0x100 | msgType) : msgType;
    }

    private static int[] getResponseClasses(IMqttsnCodec codec, IMqttsnMessage message) {

        if(!requiresResponse(codec, message)){
            return new int[0];
        }
        switch(dispatchKey(codec, message)){
            case MqttsnConstants.AUTH:
                return new int[]{ MqttsnConstants.AUTH, MqttsnConstants.CONNACK };
            case 0x100 | MqttsnConstants.AUTH_V2_0:
                return new int[]{ MqttsnConstants.AUTH_V2_0, MqttsnConstants.CONNACK_V2_0 };
            case MqttsnConstants.CONNECT:
                return new int[]{ MqttsnConstants.CONNACK };
            case 0x100 | MqttsnConstants.CONNECT_V2_0:
                return new int[]{ MqttsnConstants.CONNACK_V2_0 };
            case MqttsnConstants.PUBLISH:
                return new int[]{ MqttsnConstants.PUBACK, MqttsnConstants.PUBREC, MqttsnConstants.PUBREL, MqttsnConstants.PUBCOMP };
            case 0x100 | MqttsnConstants.PUBLISH_V2_0:
                return new int[]{ MqttsnConstants.PUBACK_V2_0, MqttsnConstants.PUBREC_V2_0, MqttsnConstants.PUBREL_V2_0, MqttsnConstants.PUBCOMP_V2_0 };
            case MqttsnConstants.PUBREC:
                return new int[]{ MqttsnConstants.PUBREL };
            case 0x100 | MqttsnConstants.PUBREC_V2_0:
                return new int[]{ MqttsnConstants.PUBREL_V2_0 };
            case MqttsnConstants.PUBREL:
                return new int[]{ MqttsnConstants.PUBCOMP };
            case 0x100 | MqttsnConstants.PUBREL_V2_0:
                return new int[]{ MqttsnConstants.PUBCOMP_V2_0 };
            case MqttsnConstants.SUBSCRIBE:
                return new int[]{ MqttsnConstants.SUBACK};
            case 0x100 | MqttsnConstants.SUBSCRIBE_V2_0:
                return new int[]{ MqttsnConstants.SUBACK_V2_0 };
            case MqttsnConstants.UNSUBSCRIBE:
                return new int[]{ MqttsnConstants.UNSUBACK };
            case 0x100 | MqttsnConstants.UNSUBSCRIBE_V2_0:
                return new int[]{ MqttsnConstants.UNSUBACK_V2_0 };
            case MqttsnConstants.REGISTER:
                return new int[]{ MqttsnConstants.REGACK };
            case MqttsnConstants.PINGREQ:
                return new int[]{ MqttsnConstants.PINGRESP };
            case 0x100 | MqttsnConstants.PINGREQ_V2_0:
                return new int[]{ MqttsnConstants.PINGRESP_V2_0 };
            case MqttsnConstants.DISCONNECT:
                return new int[]{ MqttsnConstants.DISCONNECT };
            case 0x100 | MqttsnConstants.DISCONNECT_V2_0:
                return new int[]{ MqttsnConstants.DISCONNECT_V2_0 };
            case MqttsnConstants.SEARCHGW:
                return new int[]{ MqttsnConstants.GWINFO };
            case 0x100 | MqttsnConstants.SEARCHGW_V2_0:
                return new int[]{ MqttsnConstants.GWINFO_V2_0 };
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
        switch(dispatchKey(codec, message)){
            case MqttsnConstants.PUBLISH:
            case 0x100 | MqttsnConstants.PUBLISH_V2_0:
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
            case 0x100 | MqttsnConstants.CONNACK_V2_0:
            case 0x100 | MqttsnConstants.PUBACK_V2_0:
            case 0x100 | MqttsnConstants.PUBREL_V2_0:
            case 0x100 | MqttsnConstants.UNSUBACK_V2_0:
            case 0x100 | MqttsnConstants.SUBACK_V2_0:
            case 0x100 | MqttsnConstants.ADVERTISE_V2_0:
            case 0x100 | MqttsnConstants.PUBCOMP_V2_0:
            case 0x100 | MqttsnConstants.PINGRESP_V2_0:
            case 0x100 | MqttsnConstants.DISCONNECT_V2_0:
            case 0x100 | MqttsnConstants.GWINFO_V2_0:
                return true;
            default:
                return false;
        }
    }

    public static boolean requiresResponse(IMqttsnCodec codec, IMqttsnMessage message) {
        switch(dispatchKey(codec, message)){
            case MqttsnConstants.HELO:
                return ((MqttsnHelo)message).getUserAgent() == null;
            case MqttsnConstants.PUBLISH:
            case 0x100 | MqttsnConstants.PUBLISH_V2_0:
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
            case 0x100 | MqttsnConstants.CONNECT_V2_0:
            case 0x100 | MqttsnConstants.PUBREC_V2_0:
            case 0x100 | MqttsnConstants.PUBREL_V2_0:
            case 0x100 | MqttsnConstants.SUBSCRIBE_V2_0:
            case 0x100 | MqttsnConstants.UNSUBSCRIBE_V2_0:
            case 0x100 | MqttsnConstants.PINGREQ_V2_0:
            case 0x100 | MqttsnConstants.DISCONNECT_V2_0:
            case 0x100 | MqttsnConstants.SEARCHGW_V2_0:
                return true;
            default:
                return false;
        }
    }

    public static boolean isAck(IMqttsnCodec codec, IMqttsnMessage message, boolean sending){
        switch(dispatchKey(codec, message)){
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
            case 0x100 | MqttsnConstants.CONNACK_V2_0:
            case 0x100 | MqttsnConstants.PUBACK_V2_0:
            case 0x100 | MqttsnConstants.PUBREC_V2_0:
            case 0x100 | MqttsnConstants.PUBCOMP_V2_0:
            case 0x100 | MqttsnConstants.SUBACK_V2_0:
            case 0x100 | MqttsnConstants.UNSUBACK_V2_0:
            case 0x100 | MqttsnConstants.PINGRESP_V2_0:
            case 0x100 | MqttsnConstants.SEARCHGW_V2_0:
                return true;
            case MqttsnConstants.DISCONNECT:
            case 0x100 | MqttsnConstants.DISCONNECT_V2_0:
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
