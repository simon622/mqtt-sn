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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface MqttsnConstants {

    //-- topic path separator regex with lookahead and lookbehind to maintain tokens
    String TOPIC_SEPARATOR_REGEX = "((?<=/)|(?=/))";


    //-- protocol versions
    int PROTOCOL_VERSION_UNKNOWN = 0x00;
    int PROTOCOL_VERSION_1_2 = 0x01;
    int PROTOCOL_VERSION_2_0 = 0x02;

    //-- the restricted range
    char MIN_HIGH_UTF = '\uD800';
    char MAX_HIGH_UTF = '\uDBFF';

    //-- the optionally restricted range
    char MIN_CONTROL1_UTF = '\u0001';
    char MAX_CONTROL1_UTF = '\u001F';

    char MIN_CONTROL2_UTF = '\u007F';
    char MAX_CONTROL2_UTF = '\u009F';
    char UNICODE_ZERO = '\u0000';

    long UNSIGNED_MAX_32 = 4294967295L;
    int UNSIGNED_MAX_16 = 65535;
    int UNSIGNED_MAX_8 = 255;

    int MAX_CLIENT_ID_LENGTH_v12 = 23;
    int MAX_CLIENT_ID_LENGTH_v2 = UNSIGNED_MAX_16 - 13;

    int MAX_TOPIC_LENGTH = UNSIGNED_MAX_16;
    int MAX_PUBLISH_LENGTH = UNSIGNED_MAX_16 - (9 + 2); //9 for the normal fields, + 2 for large message type
    int MAX_ENCAPSULATED_LENGTH = UNSIGNED_MAX_16 - 7;

    Charset CHARSET = StandardCharsets.UTF_8;

    String SINGLE_LEVEL_WILDCARD = "+"; //U+002B
    String MULTI_LEVEL_WILDCARD = "#"; //U+0023

    char PATH_SEP = '/';
    char SINGLE_WILDCARD_CHAR = '+';

    /**
     * Topic Type values, shared by the runtime/registry layer across both protocol versions.
     * NB: SHORT (0b10) is a v1.2-only concept - MQTT-SN 2.0 (CSD01, Table 5 "Topic Types")
     * redefines this same 2-bit value as Reserved ("The Short Topic Name has been removed").
     * v2.0 wire code MUST NOT produce or accept TOPIC_SHORT - see the v2.0 payload classes'
     * validate() methods.
     */
    byte TOPIC_NORMAL = 0b00,
            TOPIC_PREDEFINED = 0b01,
            TOPIC_SHORT = 0b10,
            TOPIC_FULL = 0b11;

    enum TOPIC_TYPE {

        NORMAL(TOPIC_NORMAL),
        PREDEFINED(TOPIC_PREDEFINED),
        SHORT(TOPIC_SHORT),
        FULL(TOPIC_FULL);

        byte flag;

        TOPIC_TYPE(byte flag) {
            this.flag = flag;
        }

        public byte getFlag() {
            return flag;
        }

    }

    int RETAINED_SEND = 0x00,
            RETAINED_SEND_NOT_EXISTS = 0x01,
            RETAINED_NO_SEND = 0x02;

    //-- MQTT-SN v1.2 return codes (IBM spec v1.2). Kept as-is for v1.2 use.
    int RETURN_CODE_ACCEPTED = 0x00,
            RETURN_CODE_REJECTED_CONGESTION = 0x01,
            RETURN_CODE_INVALID_TOPIC_ID = 0x02,
            RETURN_CODE_SERVER_UNAVAILABLE = 0x03,
            RETURN_CODE_PAYLOAD_FORMAT_INVALID = 0x99;

    /**
     * MQTT-SN v2.0 Reason Codes (OASIS mqtt-sn-v2.0 CSD01, 05 February 2026, Table 4 "Reason
     * Codes" - section 2.3). Reason Codes below 0x80 indicate success, 0x80 or greater indicate
     * failure. Several names below share the same numeric value by design (the meaning is
     * packet-context-dependent per the spec table's "Packets" column) - use whichever name best
     * documents intent at the call site.
     */
    int RETURN_CODE_SUCCESS_V2_0 = 0x00,                                  //CONNACK, SUBACK, UNSUBACK, REGACK, PUBACK, PUBREC, PUBREL, PUBCOMP, SLEEPRESP, AUTH (server only)
            RETURN_CODE_NORMAL_DISCONNECTION_V2_0 = 0x00,                 //DISCONNECT
            RETURN_CODE_GRANTED_QOS_0_V2_0 = 0x00,                        //SUBACK
            RETURN_CODE_GRANTED_QOS_1_V2_0 = 0x01,                        //SUBACK
            RETURN_CODE_GRANTED_QOS_2_V2_0 = 0x02,                        //SUBACK
            RETURN_CODE_DISCONNECT_WITH_WILL_MESSAGE_V2_0 = 0x04,         //DISCONNECT (client only)
            RETURN_CODE_NO_MATCHING_SUBSCRIBERS_V2_0 = 0x10,              //PUBACK, PUBREC
            RETURN_CODE_NO_SUBSCRIPTION_EXISTED_V2_0 = 0x11,              //UNSUBACK
            RETURN_CODE_CONTINUE_AUTHENTICATION_V2_0 = 0x18,              //AUTH
            RETURN_CODE_RE_AUTHENTICATE_V2_0 = 0x19,                      //AUTH (client only)
            RETURN_CODE_TOPIC_ALIAS_EXISTS_V2_0 = 0x1A,                   //REGACK
            RETURN_CODE_UNSPECIFIED_ERROR_V2_0 = 0x80,                    //CONNACK, PUBACK, PUBREC, SUBACK, UNSUBACK, DISCONNECT
            RETURN_CODE_MALFORMED_PACKET_V2_0 = 0x81,                     //CONNACK, DISCONNECT
            RETURN_CODE_PROTOCOL_ERROR_V2_0 = 0x82,                       //CONNACK, DISCONNECT
            RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR_V2_0 = 0x83,        //CONNACK, PUBACK, PUBREC, REGACK, SUBACK, UNSUBACK, DISCONNECT
            RETURN_CODE_UNSUPPORTED_PROTOCOL_VERSION_V2_0 = 0x84,         //CONNACK
            RETURN_CODE_CLIENT_IDENTIFIER_NOT_VALID_V2_0 = 0x85,          //CONNACK
            RETURN_CODE_BAD_USERNAME_OR_PASSWORD_V2_0 = 0x86,             //CONNACK
            RETURN_CODE_NOT_AUTHORIZED_V2_0 = 0x87,                       //CONNACK, PUBACK, PUBREC, REGACK, SUBACK, UNSUBACK, DISCONNECT (server only)
            RETURN_CODE_SERVER_UNAVAILABLE_V2_0 = 0x88,                   //CONNACK
            RETURN_CODE_SERVER_BUSY_V2_0 = 0x89,                          //CONNACK, DISCONNECT (server only)
            RETURN_CODE_BANNED_V2_0 = 0x8A,                               //CONNACK
            RETURN_CODE_SERVER_SHUTTING_DOWN_V2_0 = 0x8B,                 //DISCONNECT (server only)
            RETURN_CODE_BAD_AUTHENTICATION_METHOD_V2_0 = 0x8C,            //CONNACK, DISCONNECT
            RETURN_CODE_KEEP_ALIVE_TIMEOUT_V2_0 = 0x8D,                   //DISCONNECT (server only)
            RETURN_CODE_SESSION_TAKEN_OVER_V2_0 = 0x8E,                   //DISCONNECT (server only)
            RETURN_CODE_TOPIC_FILTER_INVALID_V2_0 = 0x8F,                 //SUBACK, UNSUBACK, DISCONNECT (server only)
            RETURN_CODE_TOPIC_NAME_INVALID_V2_0 = 0x90,                   //CONNACK, PUBACK, PUBREC, DISCONNECT (server only)
            RETURN_CODE_PACKET_IDENTIFIER_IN_USE_V2_0 = 0x91,             //PUBACK, PUBREC, SUBACK, UNSUBACK, REGACK, PINGRESP, SLEEPRESP
            RETURN_CODE_PACKET_IDENTIFIER_NOT_FOUND_V2_0 = 0x92,          //PUBREL, PUBCOMP
            RETURN_CODE_RECEIVE_MAXIMUM_EXCEEDED_V2_0 = 0x93,             //DISCONNECT
            RETURN_CODE_TOPIC_ALIAS_INVALID_V2_0 = 0x94,                  //DISCONNECT (server only)
            RETURN_CODE_PACKET_TOO_LARGE_V2_0 = 0x95,                     //CONNACK, DISCONNECT
            RETURN_CODE_PACKET_RATE_TOO_HIGH_V2_0 = 0x96,                 //DISCONNECT
            RETURN_CODE_QUOTA_EXCEEDED_V2_0 = 0x97,                       //REGACK, SUBACK, DISCONNECT
            RETURN_CODE_ADMINISTRATIVE_ACTION_V2_0 = 0x98,                //DISCONNECT
            RETURN_CODE_PAYLOAD_FORMAT_INVALID_V2_0 = 0x99,               //PUBACK, PUBREC, DISCONNECT (server only)
            RETURN_CODE_RETAIN_NOT_SUPPORTED_V2_0 = 0x9A,                 //CONNACK, DISCONNECT (server only)
            RETURN_CODE_QOS_NOT_SUPPORTED_V2_0 = 0x9B,                    //CONNACK, DISCONNECT (server only)
            RETURN_CODE_USE_ANOTHER_SERVER_V2_0 = 0x9C,                   //CONNACK, DISCONNECT (server only)
            RETURN_CODE_SERVER_MOVED_V2_0 = 0x9D,                         //CONNACK, DISCONNECT (server only)
            RETURN_CODE_SHARED_SUBSCRIPTION_NOT_SUPPORTED_V2_0 = 0x9E,    //SUBACK, DISCONNECT (server only)
            RETURN_CODE_CONNECTION_RATE_EXCEEDED_V2_0 = 0x9F,             //CONNACK, DISCONNECT (server only)
            RETURN_CODE_MAXIMUM_CONNECT_TIME_V2_0 = 0xA0,                 //DISCONNECT (server only)
            RETURN_CODE_SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED_V2_0 = 0xA1, //SUBACK, DISCONNECT (server only)
            RETURN_CODE_WILDCARD_SUBSCRIPTION_NOT_SUPPORTED_V2_0 = 0xA2,  //SUBACK, DISCONNECT (server only)
            //-- 0xE6-0xFF: MQTT-SN dedicated reason code range
            RETURN_CODE_ONLY_PROTECTION_PACKET_SUPPORTED_V2_0 = 0xE6,     //any packet except PROTECTION and Forwarder Encapsulation
            RETURN_CODE_PROTECTION_SCHEME_INVALID_V2_0 = 0xE7,            //DISCONNECT
            RETURN_CODE_UNKNOWN_SENDER_ID_V2_0 = 0xE8,                    //DISCONNECT
            RETURN_CODE_UNKNOWN_TOPIC_ALIAS_V2_0 = 0xF0,                  //PUBACK, PUBREC, SUBACK, UNSUBACK, REGACK
            RETURN_CODE_CONGESTION_V2_0 = 0xF1,                           //SUBACK, REGACK, CONNACK, PUBACK, PUBREC
            RETURN_CODE_PROTECTION_PACKET_NOT_SUPPORTED_V2_0 = 0xF2,      //DISCONNECT
            RETURN_CODE_FORWARDER_ENCAPSULATION_NOT_SUPPORTED_V2_0 = 0xF3, //DISCONNECT
            RETURN_CODE_NO_VIRTUAL_CONNECTION_EXISTS_V2_0 = 0xF4;         //DISCONNECT
            //-- 0xF5-0xFF: reserved for MQTT-SN

    int QoS0 = 0,
            QoS1 = 1,
            QoS2 = 2,
            QoSM1 = -1;

    //-- MQTT-SN v1.2 packet types (IBM spec v1.2, 14 Nov 2013). Shared by the v1.2 codec AND
    //-- used as the fallback dispatch table for anything the v2.0 codec does not override.
    byte ADVERTISE = 0x00;
    byte SEARCHGW = 0x01;
    byte GWINFO = 0x02;
    byte AUTH = 0x03;
    byte CONNECT = 0x04;
    byte CONNACK = 0x05;
    byte WILLTOPICREQ = 0x06;
    byte WILLTOPIC = 0x07;
    byte WILLMSGREQ = 0x08;
    byte WILLMSG = 0x09;
    byte REGISTER = 0x0A;
    byte REGACK = 0x0B;
    byte PUBLISH = 0x0C;
    byte PUBLISH_M1 = 0x11;
    byte PUBACK = 0x0D;
    byte PUBCOMP = 0x0E;
    byte PUBREC = 0x0F;
    byte PUBREL = 0x10;
    byte SUBSCRIBE = 0x12;
    byte SUBACK = 0x13;
    byte UNSUBSCRIBE = 0x14;
    byte UNSUBACK = 0x15;
    byte PINGREQ = 0x16;
    byte PINGRESP = 0x17;
    byte DISCONNECT = 0x18;
    byte WILLTOPICUPD = 0x1A;
    byte WILLTOPICRESP = 0x1B;
    byte WILLMSGUPD = 0x1C;
    byte WILLMSGRESP = 0x1D;
    byte HELO = 0x2D;
    byte PROTECTION = 0x1E;

    int ENCAPSMSG = 0xFE;

    /**
     * MQTT-SN v2.0 packet types (OASIS mqtt-sn-v2.0, Committee Specification Draft 01,
     * 05 February 2026, Table 2 "MQTT-SN Control Packet Types" - section 2.1.3).
     *
     * These are a wire-incompatible renumbering versus v1.2 (e.g. CONNECT is 0x01 here but
     * 0x04 above) so they MUST NOT share identifiers with the v1.2 constants - they are kept
     * as a fully separate, independently numbered block used exclusively by the v2.0 codec
     * and v2.0 payload classes.
     */
    byte CONNECT_V2_0 = 0x01;
    byte CONNACK_V2_0 = 0x02;
    byte PUBLISH_V2_0 = 0x03;
    byte PUBACK_V2_0 = 0x04;
    byte PUBREC_V2_0 = 0x05;
    byte PUBREL_V2_0 = 0x06;
    byte PUBCOMP_V2_0 = 0x07;
    byte SUBSCRIBE_V2_0 = 0x08;
    byte SUBACK_V2_0 = 0x09;
    byte UNSUBSCRIBE_V2_0 = 0x0A;
    byte UNSUBACK_V2_0 = 0x0B;
    byte PINGREQ_V2_0 = 0x0C;
    byte PINGRESP_V2_0 = 0x0D;
    byte DISCONNECT_V2_0 = 0x0E;
    byte AUTH_V2_0 = 0x0F;
    byte REGISTER_V2_0 = 0x10;
    byte REGACK_V2_0 = 0x11;
    byte PUBWOS_V2_0 = 0x12;
    byte SLEEPREQ_V2_0 = 0x13;
    byte SLEEPRESP_V2_0 = 0x14;
    byte WAKEUP_V2_0 = 0x15;
    byte ADVERTISE_V2_0 = 0x16;
    byte SEARCHGW_V2_0 = 0x17;
    byte GWINFO_V2_0 = 0x18;
    //-- 0x19-0xFC reserved
    byte FORWARDER_ENCAPSULATION_V2_0 = (byte) 0xFD;
    byte SESSION_ENCAPSULATION_V2_0 = (byte) 0xFE;
    byte PROTECTION_ENCAPSULATION_V2_0 = (byte) 0xFF;

}
