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

package org.slj.mqtt.sn.utils;

import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnSecurityException;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Resulting Hmac Sizes;
 * HmacSHA1 = 20 bytes
 * HmacMD5 = 16 bytes
 * HmacSHA224 = 28 bytes
 * HmacSHA256 = 32 bytes
 * HmacSHA384 = 48 bytes
 * HmacSHA512 = 64 bytes
 */
public class Security {

    public static int MINIMUM_SECRET_BYTES = 3;       //dont allow less than 128 bit secret

    public enum HMAC {

        HMAC_MD5("HmacMD5", 16),
        HMAC_SHA1("HmacSHA1", 20),
        HMAC_SHA_224("HmacSHA224", 28),
        HMAC_SHA_256("HmacSHA256", 32),
        HMAC_SHA_384("HmacSHA384", 48),
        HMAC_SHA_512("HmacSHA512", 64);

        HMAC(String value, int size){
            this.value = value;
            this.size = size;
        }
        private String value;
        private int size;

        public String getAlgorithm(){
            return value;
        }

        public int getSize(){
            return size;
        }
    }

    public enum CHECKSUM {

        CRC32("CRC-32", 4),
        ADLER32("ADLER-32", 4);

        CHECKSUM(String value, int size){
            this.value = value;
            this.size = size;
        }

        private String value;
        private int size;

        public String getAlgorithm(){
            return value;
        }

        public int getSize(){
            return size;
        }
    }

    public static byte[] hmac(HMAC algorithm, byte[] secretKey, byte[] data, boolean hex) throws MqttsnSecurityException {

        if (algorithm == null) throw new MqttsnSecurityException("hmac algorithm must be provided <null>");
        if (secretKey == null) throw new MqttsnSecurityException("hmac secret must be provided <null>");
        if (secretKey.length < MINIMUM_SECRET_BYTES) throw new MqttsnSecurityException("hmac secret must be at least 128-bit");
        if (data == null) throw new MqttsnSecurityException("hmac data must be provided <null>");

        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, algorithm.getAlgorithm());
        try {
            Mac mac = Mac.getInstance(algorithm.getAlgorithm());
            mac.init(secretKeySpec);
            if(mac.getMacLength() != algorithm.getSize()) throw new MqttsnSecurityException("algorithm mac length does not match compiled length");
            byte[] hmac = mac.doFinal(data);
            if(hmac.length != algorithm.getSize()) throw new MqttsnSecurityException("generated mac was unexpected length");
            return hex ? MqttsnWireUtils.toHex(hmac).
                    getBytes(StandardCharsets.UTF_8) : hmac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new MqttsnSecurityException("hmac failed;", e);
        }
    }

    public static int checksum(CHECKSUM algorithm, byte[] data) throws MqttsnSecurityException {

        if (algorithm == null) throw new MqttsnSecurityException("checksum algorithm must be provided <null>");
        if (data == null) throw new MqttsnSecurityException("checksum data must be provided <null>");

        long checksum = -1;
        switch (algorithm) {
            case CRC32:
                Checksum crc32 = new CRC32();
                crc32.update(data, 0, data.length);
                checksum = crc32.getValue();
                break;

            case ADLER32:
                Checksum adler32 = new Adler32();
                adler32.update(data, 0, data.length);
                checksum = adler32.getValue();
                break;
        }

        return (int) checksum;
    }

    public static byte[] createChecksumdData(CHECKSUM algorithm, byte[] data) throws MqttsnSecurityException {

        byte[] arr = new byte[algorithm.getSize() + data.length];
        int checksum = checksum(algorithm, data);
        arr[0] = (byte) (checksum >> 24);
        arr[1] = (byte) (checksum >> 16);
        arr[2] = (byte) (checksum >> 8);
        arr[3] = (byte) (checksum);
        System.arraycopy(data, 0, arr, algorithm.getSize(), data.length);
        return arr;
    }

    public static byte[] createHmacdData(HMAC algorithm, byte[] secretKey, byte[] data) throws MqttsnSecurityException {

        byte[] arr = new byte[algorithm.getSize() + data.length];
        byte[] hmac = hmac(algorithm, secretKey, data, false);
        System.arraycopy(hmac, 0, arr, 0, hmac.length);
        System.arraycopy(data, 0, arr, hmac.length, data.length);
        return arr;
    }

    public static byte[] readHeader(int len, byte[] data) {
        byte[] arr = new byte[len];
        System.arraycopy(data, 0, arr, 0, len);
        return arr;
    }

    public static byte[] readOriginalData(int offset, byte[] data) {
        byte[] arr = new byte[data.length - offset];
        System.arraycopy(data, offset, arr, 0, arr.length);
        return arr;
    }

    public static byte[] readOriginalData(HMAC algorithm, byte[] data) {
        return readOriginalData(algorithm.getSize(), data);
    }

    public static byte[] readOriginalData(CHECKSUM algorithm, byte[] data) {
        return readOriginalData(algorithm.getSize(), data);
    }

    public static boolean verifyChecksum(CHECKSUM algorithm, byte[] data) throws MqttsnSecurityException {
        byte[] checksum = readHeader(algorithm.getSize(), data);
        long value = ((checksum[0] & 0xFF) << 24) |
                ((checksum[1] & 0xFF) << 16) |
                ((checksum[2] & 0xFF) << 8) |
                ((long) (checksum[3] & 0xFF));
        long check = checksum(algorithm, readOriginalData(algorithm.getSize(), data));
        return value == check;
    }

    public static boolean verifyHMac(HMAC algorithm, byte[] secretKey, byte[] data) throws MqttsnSecurityException {
        byte[] hmac = readHeader(algorithm.getSize(), data);
        return Arrays.equals(hmac, hmac(algorithm, secretKey,
                readOriginalData(algorithm.getSize(), data), false));
    }

    public static byte[] hash(byte[] data, String alg) throws NoSuchAlgorithmException {
        //MD not thgread safe
        MessageDigest digest = MessageDigest.getInstance(alg);
        return digest.digest(data);
    }
}