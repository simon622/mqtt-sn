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

package org.slj.mqtt.sn.wire;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.codec.MqttsnCodecException;

public class MqttsnWireUtils {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static short read8bit(byte b1) {
        return (short) (b1 & 0xFF);
    }

    public static int read16bit(byte b1, byte b2) {
        return ((b1 & 0xFF) << 8) + (b2 & 0xFF);
    }

    public static String toBinary(byte... b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; b != null && i < b.length; i++) {
            sb.append(String.format("%8s", Integer.toBinaryString(b[i] & 0xFF)).replace(' ', '0'));
            if (i < b.length - 1)
                sb.append(" ");
        }
        return sb.toString();
    }

    public static String toHex(byte... b) {
        if(b == null) return null;
        char[] hexChars = new char[b.length * 2];
        for (int j = 0; j < b.length; j++) {
            int v = b[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] readBuffer(byte[] buf, int off, int len){
        int copyLength = Math.min(len, buf.length);
        byte[] copy = new byte[copyLength];
        System.arraycopy(buf, off, copy, 0, copyLength);
        return copy;
    }

    public static int readMessageType(byte[] data) {
        int msgType;
        if (data[0] == 0x01) {
            msgType = (data[3] & 0xFF);
        } else {
            msgType = (data[1] & 0xFF);
        }
        return msgType;
    }
}