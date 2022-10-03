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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface MqttsnConstants {

    //-- topic path separator regex with lookahead and lookbehind to maintainn tokens
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

    int MAX_CLIENT_ID_LENGTH = 1024;
    int MAX_TOPIC_LENGTH = UNSIGNED_MAX_16;
    int MAX_PUBLISH_LENGTH = UNSIGNED_MAX_16 - (9 + 2); //9 for the normal fields, + 2 for large message type
    int MAX_ENCAPSULATED_LENGTH = UNSIGNED_MAX_16 - 7;

    Charset CHARSET = StandardCharsets.UTF_8;

    String SINGLE_LEVEL_WILDCARD = "+"; //U+002B
    String MULTI_LEVEL_WILDCARD = "#"; //U+0023

    char PATH_SEP = '/';
    char SINGLE_WILDCARD_CHAR = '+';

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

    int RETURN_CODE_ACCEPTED = 0x00,
            RETURN_CODE_REJECTED_CONGESTION = 0x01,
            RETURN_CODE_INVALID_TOPIC_ID = 0x02,
            RETURN_CODE_SERVER_UNAVAILABLE = 0x03,
            RETURN_CODE_PAYLOAD_FORMAT_INVALID = 0x99;

    int QoS0 = 0,
            QoS1 = 1,
            QoS2 = 2,
            QoSM1 = -1;

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


    int ENCAPSMSG = 0xFE;

}
