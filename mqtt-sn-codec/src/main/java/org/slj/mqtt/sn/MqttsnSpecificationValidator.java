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
        if(data == null && allowNull){
            return true;
        }
        if(data.length() > MqttsnConstants.UNSIGNED_MAX_16){
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

    public static void validateSubscribePath(String topicPath) throws MqttsnCodecException {
        if(!isValidSubscriptionTopic(topicPath))
            throw new MqttsnCodecException("invalid subscribe topic - " + topicPath);
    }

    public static void validatePublishPath(String topicPath) throws MqttsnCodecException {
        if(!isValidPublishTopic(topicPath))
            throw new MqttsnCodecException("invalid publish topic - " + topicPath);
    }

    public static boolean isValidPublishTopic(String topicPath){
        return isValidPublishTopic(topicPath, MqttsnConstants.MAX_TOPIC_LENGTH);
    }

    public static boolean isValidPublishTopic(String topicPath, int maxLength){
        return isValidTopicInternal(topicPath, maxLength, false);
    }

    public static boolean isValidSubscriptionTopic(String topicPath, int maxLength){
        boolean valid = isValidTopicInternal(topicPath, maxLength, true);
        if(valid && topicPath.contains(MqttsnConstants.MULTI_LEVEL_WILDCARD)) {
            valid &= topicPath.endsWith(MqttsnConstants.MULTI_LEVEL_WILDCARD);
            if(topicPath.length() > 1){
                valid &= topicPath.charAt(topicPath.indexOf(MqttsnConstants.MULTI_LEVEL_WILDCARD) - 1)
                        == MqttsnConstants.PATH_SEP;
            }
        }

        if(valid && topicPath.contains(MqttsnConstants.SINGLE_LEVEL_WILDCARD)) {
            if(topicPath.length() > 1){
                char[] c = topicPath.toCharArray();
                for(int i = 0; i < c.length; i++){
                    if(c[i] == MqttsnConstants.SINGLE_WILDCARD_CHAR){
                        //check the preceeding char
                        if(i > 0){
                            valid &= c[i - 1] == MqttsnConstants.PATH_SEP;
                        }
                        //check the next char
                        if(c.length > (i + 1)){
                            valid &= c[i + 1] == MqttsnConstants.PATH_SEP;
                        }
                    }
                }
            }
        }

        return valid;
    }
    public static boolean isValidSubscriptionTopic(String topicPath){
        return isValidSubscriptionTopic(topicPath, MqttsnConstants.MAX_TOPIC_LENGTH);
    }

    private static boolean isValidTopicInternal(String topicPath, int maxLength, boolean allowWild){
        boolean valid = topicPath != null && topicPath.length() > 0 &&
                topicPath.length() < Math.min(maxLength, MqttsnConstants.MAX_TOPIC_LENGTH) &&
                topicPath.indexOf(MqttsnConstants.UNICODE_ZERO) == -1;
        if(valid){
            valid &= MqttsnSpecificationValidator.validStringData(topicPath, false);
        }
        if(valid && !allowWild){
            valid &= !topicPath.contains(MqttsnConstants.SINGLE_LEVEL_WILDCARD) &&
                    !topicPath.contains(MqttsnConstants.MULTI_LEVEL_WILDCARD);
        }
        return valid;
    }

    public static boolean validClientId(String clientId, boolean allowNull, int maxLength) {
        if(clientId == null){
            if(!allowNull) return false;
            else return true;
        }
        return MqttsnSpecificationValidator.validStringData(clientId, allowNull) && (clientId.length() <= maxLength);
    }

    public static boolean validUInt8(int field) {
        if (field < 0 || field > MqttsnConstants.UNSIGNED_MAX_8) {
            return false;
        }
        return true;
    }

    public static boolean validUInt16(int field) {
        if (field < 0 || field > MqttsnConstants.UNSIGNED_MAX_16) {
            return false;
        }
        return true;
    }

    public static boolean validUInt32(long field) {
        if (field < 0 || field > MqttsnConstants.UNSIGNED_MAX_32) {
            return false;
        }
        return true;
    }

    public static boolean validTopicIdType(int field) {
        return field == MqttsnConstants.TOPIC_PREDEFINED ||
                field == MqttsnConstants.TOPIC_SHORT ||
                field == MqttsnConstants.TOPIC_NORMAL || field == MqttsnConstants.TOPIC_FULL;
    }

    //-- these methods throw

    public static void validateStringData(String data, boolean allowNull) {
        if(!validStringData(data, allowNull))
            throw new MqttsnCodecException("invalid string data - " + data);
    }

    public static void validateClientId(String clientId) {
        if(!validClientId(clientId, true, MqttsnConstants.MAX_CLIENT_ID_LENGTH))
            throw new MqttsnCodecException("invalid clientId - " + clientId);
    }

    public static void validateProtocolId(int protocolId){
        if(protocolId != MqttsnConstants.PROTOCOL_VERSION_1_2 &&
                 protocolId != MqttsnConstants.PROTOCOL_VERSION_2_0 && protocolId != 0){
            throw new MqttsnCodecException("invalid protocol version - " + protocolId);
        }
    }

    public static void validateDefaultAwakeMessages(int defaultAwakeMessages){
        if(defaultAwakeMessages < 0 || defaultAwakeMessages > 15){
            throw new MqttsnCodecException("invalid default awake messages value - " + defaultAwakeMessages);
        }
    }

    public static void validatePublishData(byte[] data) {
        if(data == null)
            throw new MqttsnCodecException("publish data cannot be null");

        if(data.length > MqttsnConstants.MAX_PUBLISH_LENGTH)
            throw new MqttsnCodecException("data too long for packet " + data.length);
    }

    public static void validateEncapsulatedData(byte[] data) {
        if(data == null || data.length > MqttsnConstants.MAX_ENCAPSULATED_LENGTH)
            throw new MqttsnCodecException("invalid encapsulated data");
    }

    public static void validateReturnCode(int field) throws MqttsnCodecException {
       validateUInt8(field);
    }

    public static void validateAuthReasonCode(int field) throws MqttsnCodecException {
        validateUInt8(field);
        if(field != 0x00 && field != 0x18 && field != 0x19)
            throw new MqttsnCodecException("invalid auth reason code, must be one of 0x00, 0x18, 0x19");
    }

    public static void validatePacketIdentifier(int field) throws MqttsnCodecException {
        validateUInt16(field);
    }

    public static void validateMaxPacketSize(int field) throws MqttsnCodecException {
        validateUInt16(field);
    }

    public static void validateKeepAlive(int field) throws MqttsnCodecException {
        validateUInt16(field);
    }

    public static void validateSessionExpiry(long field) throws MqttsnCodecException {
        validateUInt32(field);
    }

    public static void validateDuration(int duration) throws MqttsnCodecException {
        validateUInt16(duration);
    }

    public static void validateTopicAlias(int field) throws MqttsnCodecException {
        validateUInt16(field);
    }

    public static void validateTopicIdType(int field) throws MqttsnCodecException {
        if (!validTopicIdType(field)) {
            throw new MqttsnCodecException("invalid topicIdType - " + field);
        }
    }

    public static void validatePacketLength(byte[] packet){
        if(packet == null){
            throw new MqttsnCodecException("malformed mqtt-sn packet, null");
        }
        if(packet.length < 2){
            throw new MqttsnCodecException("malformed mqtt-sn packet, small ("+packet.length+")");
        }
        if(packet.length > MqttsnConstants.UNSIGNED_MAX_16){
            throw new MqttsnCodecException("malformed mqtt-sn packet, large ("+packet.length+")");
        }
    }

    public static void validateUInt8(int field) throws MqttsnCodecException {
        if (!validUInt8(field)) {
            throw new MqttsnCodecException("invalid unsigned 8 bit number - " + field);
        }
    }

    public static void validateUInt16(int field) throws MqttsnCodecException {
        if (!validUInt16(field)) {
            throw new MqttsnCodecException("invalid unsigned 16 bit number - " + field);
        }
    }

    public static void validateUInt32(long field) throws MqttsnCodecException {
        if (!validUInt32(field)) {
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

    public static void validateRetainHandling(int retain) throws MqttsnCodecException {
        if (retain != MqttsnConstants.RETAINED_SEND &&
                retain != MqttsnConstants.RETAINED_NO_SEND &&
                retain != MqttsnConstants.RETAINED_SEND_NOT_EXISTS) {
            throw new MqttsnCodecException("invalid retain handling number - " + retain);
        }
    }

    public static void main(String[] args) {
        validateClientId("simon");
    }
}
