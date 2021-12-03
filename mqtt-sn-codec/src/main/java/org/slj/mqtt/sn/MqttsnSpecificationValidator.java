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

public class MqttsnSpecificationValidator {

    /**
     *  The character data in a UTF-8 Encoded String MUST be well-formed UTF-8 as defined by the Unicode
     *  specification [Unicode] and restated in RFC 3629 [RFC3629]. In particular, the character data MUST NOT
     *  include encodings of code points between U+D800 and U+DFFF
     *
     */
    public static boolean validStringData(String data, boolean allowNull){
        if(data == null && !allowNull){
            return false;
        }
        if(data.length() > MqttsnConstants.USIGNED_MAX_16){
            return false;
        }
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            if(c >= MqttsnConstants.MIN_HIGH_UTF &&
                    c <= MqttsnConstants.MAX_HIGH_UTF) return false;

            if(c >= MqttsnConstants.MIN_CONTROL1_UTF &&
                    c <= MqttsnConstants.MAX_CONTROL1_UTF) return false;

            if(c >= MqttsnConstants.MIN_CONTROL2_UTF &&
                    c <= MqttsnConstants.MAX_CONTROL2_UTF) return false;
        }
        return true;
    }

    public static boolean validTopicPath(String topicPath) {
        return validTopicPath(topicPath, MqttsnConstants.MAX_TOPIC_LENGTH);
    }

    public static boolean validClientId(String clientId, boolean allowNull) {
        return validClientId(clientId, allowNull, MqttsnConstants.MAX_CLIENT_ID_LENGTH);
    }

    public static boolean validTopicPath(String topicPath, int maxLength) {
        return MqttsnSpecificationValidator.validStringData(topicPath, false) &&
                topicPath.length() > 0 && topicPath.length() <= maxLength;
    }

    public static boolean validClientId(String clientId, boolean allowNull, int maxLength) {
        return MqttsnSpecificationValidator.validStringData(clientId, allowNull) &&
                (clientId != null && clientId.length() <= maxLength);
    }

    public static boolean valid8Bit(int field) throws MqttsnCodecException {
        if (field < 0 || field > MqttsnConstants.USIGNED_MAX_8) {
            return false;
        }
        return true;
    }

    public static boolean valid16Bit(int field) throws MqttsnCodecException {
        if (field < 0 || field > MqttsnConstants.USIGNED_MAX_16) {
            return false;
        }
        return true;
    }

    //-- these methods throw

    public static void validateStringData(String data, boolean allowNull) {
        if(!validStringData(data, allowNull))
            throw new MqttsnCodecException("invalid string data - " + data);
    }

    public static void validateClientId(String clientId) {
        if(!validClientId(clientId, false, MqttsnConstants.MAX_CLIENT_ID_LENGTH))
            throw new MqttsnCodecException("invalid clientId - " + clientId);
    }

    public static void validateTopicPath(String topicPath) {
        if(!validTopicPath(topicPath, MqttsnConstants.MAX_TOPIC_LENGTH))
            throw new MqttsnCodecException("invalid topicPath - " + topicPath);
    }

    public static void validatePublishData(byte[] data) {
        if(data == null || data.length > MqttsnConstants.MAX_PUBLISH_LENGTH)
            throw new MqttsnCodecException("invalid publish data");
    }

    public static void validateEncapsulatedData(byte[] data) {
        if(data == null || data.length > MqttsnConstants.MAX_ENCAPSULATED_LENGTH)
            throw new MqttsnCodecException("invalid encapsulated data");
    }

    public static void validateReturnCode(int field) throws MqttsnCodecException {
       validate8Bit(field);
    }

    public static void validatePacketIdentifier(int field) throws MqttsnCodecException {
        validate16Bit(field);
    }

    public static void validateKeepAlive(int field) throws MqttsnCodecException {
        validate16Bit(field);
    }

    public static void validateDuration(int duration) throws MqttsnCodecException {
        validate16Bit(duration);
    }

    public static void validateTopicAlias(int field) throws MqttsnCodecException {
        validate16Bit(field);
    }

    public static void validatePacketLength(byte[] packet){
        if(packet == null){
            throw new MqttsnCodecException("malformed mqtt-sn packet, null");
        }
        if(packet.length < 2){
            throw new MqttsnCodecException("malformed mqtt-sn packet, small");
        }
        if(packet.length > MqttsnConstants.USIGNED_MAX_16){
            throw new MqttsnCodecException("malformed mqtt-sn packet, large");
        }
    }

    public static void validate8Bit(int field) throws MqttsnCodecException {
        if (!valid8Bit(field)) {
            throw new MqttsnCodecException("invalid unsigned 8 bit number - " + field);
        }
    }

    public static void validate16Bit(int field) throws MqttsnCodecException {
        if (!valid16Bit(field)) {
            throw new MqttsnCodecException("invalid unsigned 16 bit number - " + field);
        }
    }

    public static void validateQoS(int QoS) throws MqttsnCodecException {
        if (QoS != MqttsnConstants.QoSM1 &&
                QoS != MqttsnConstants.QoS0 &&
                QoS != MqttsnConstants.QoS1 &&
                QoS != MqttsnConstants.QoS2) {
            throw new MqttsnCodecException("invalid QoS number - " + QoS);
        }
    }

    public static void main(String[] args) {
        validateClientId("simon");
    }
}
