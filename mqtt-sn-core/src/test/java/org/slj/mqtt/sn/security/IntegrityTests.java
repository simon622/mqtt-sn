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

package org.slj.mqtt.sn.security;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.Security;

import java.nio.charset.StandardCharsets;

public class IntegrityTests {

    static byte[] INTEGRITY_TEST_DATA =
            "hello world".getBytes(StandardCharsets.UTF_8);

    static byte[] HMAC_KEY =
            "my-secret-test-key".getBytes(StandardCharsets.UTF_8);

    static byte[] HMAC_SMALL_KEY =
            MqttsnUtils.arrayOf(15, (byte) 0x02);

    static byte[] HMAC_BOUNDARY_KEY =
            MqttsnUtils.arrayOf(16, (byte) 0x02);

    @Before
    public void setup(){
    }

    @Test
    public void testChecksumCRC32() throws MqttsnException {
        long checksum = Security.checksum(Security.CHECKSUM.CRC32, INTEGRITY_TEST_DATA);
        Assert.assertEquals("checksum should match", 222957957, checksum);
    }

    @Test
    public void testChecksumAdler32() throws MqttsnException {
        long checksum = Security.checksum(Security.CHECKSUM.ADLER32, INTEGRITY_TEST_DATA);
        Assert.assertEquals("checksum should match", 436929629, checksum);
    }

    @Test
    public void testHMACMD5() throws MqttsnException {
        testHmac(Security.HMAC.HMAC_MD5, HMAC_KEY, INTEGRITY_TEST_DATA,
                "8257C03C4FFDE216F6503B7CFB7B9307");
    }

    @Test
    public void testHMACSHA1() throws MqttsnException {
        testHmac(Security.HMAC.HMAC_SHA1, HMAC_KEY, INTEGRITY_TEST_DATA,
                "15D070CE13B9992B921C716690A7567E644AA27E");
    }

    @Test
    public void testHMACSHA224() throws MqttsnException {
        testHmac(Security.HMAC.HMAC_SHA_224, HMAC_KEY, INTEGRITY_TEST_DATA,
                "54D7FCD5BB3453ED52D687C6A1D3803D00B4345AC468EAA60C41F84C");
    }

    @Test
    public void testHMACSHA256() throws MqttsnException {
        testHmac(Security.HMAC.HMAC_SHA_256, HMAC_KEY, INTEGRITY_TEST_DATA,
                "A2033142C60D2BE95DB786B8FB68605E61D5EAD231A557DBDE7DF1FDA09815E1");
    }

    @Test
    public void testHMACSHA384() throws MqttsnException {
        testHmac(Security.HMAC.HMAC_SHA_384, HMAC_KEY, INTEGRITY_TEST_DATA,
                "9E5C23109A3F5B1E6DE941A22D40A9C99A37A8E9505074A58004081F81B94F4977DA413848B2F64095E7C5C3F3FB2E3B");
    }

    @Test
    public void testHMACSHA512() throws MqttsnException {
        testHmac(Security.HMAC.HMAC_SHA_512, HMAC_KEY, INTEGRITY_TEST_DATA,
                "AA52317CCDD56C40F1F1233AD2158A2A40EDE7C904CB836CE94798756189FAD8BFCBA164FC3947D9EEB139A2ADEA31C7EE5D165E2EDF476A4D3378D72560868F");
    }

    @Test
    public void testHMACSHA512KeyBoundary() throws MqttsnException {
        testHmac(Security.HMAC.HMAC_SHA_512, HMAC_BOUNDARY_KEY, INTEGRITY_TEST_DATA,
                "5AF7D97E61CDAFA5639F17DA1CA605C3540B2C44543716B5CECAA5A9418988029341AD8C05225132123B3B632F18309938F81CC8D8D7256A30D9F068B500789A");
    }

    @Test
    public void testHMACMD5Wikipedia() throws MqttsnException {
        testHmac(Security.HMAC.HMAC_MD5, "key".getBytes(StandardCharsets.US_ASCII),
                "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.US_ASCII),
                "80070713463e7749b90c2dc24911e275".toUpperCase());
    }

    @Test
    public void testHMACSHA1Wikipedia() throws MqttsnException {
        testHmac(Security.HMAC.HMAC_SHA1, "key".getBytes(StandardCharsets.US_ASCII),
                "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.US_ASCII),
                "de7c9b85b8b78aa6bc8a7a36f70a90701c9db4d9".toUpperCase());
    }

    @Test
    public void testHMACSHA256Wikipedia() throws MqttsnException {
        testHmac(Security.HMAC.HMAC_SHA_256, "key".getBytes(StandardCharsets.US_ASCII),
                "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.US_ASCII),
                "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8".toUpperCase());
    }

    @Test
    public void testChecksumVerifications() throws MqttsnException {
        for(Security.CHECKSUM algorithm : Security.CHECKSUM.values()){
            byte[] macdData = Security.createChecksumdData(algorithm, INTEGRITY_TEST_DATA);
            Assert.assertTrue("checksum should pass verification", Security.verifyChecksum(algorithm, macdData));
            byte[] originalData = Security.readOriginalData(algorithm, macdData);
            Assert.assertArrayEquals("resulting data should match original", INTEGRITY_TEST_DATA, originalData);
        }
    }

    @Test
    public void testHMACVerifications() throws MqttsnException {
        for(Security.HMAC algorithm : Security.HMAC.values()){
            byte[] macdData = Security.createHmacdData(algorithm, HMAC_KEY, INTEGRITY_TEST_DATA);
            Assert.assertTrue("macdData should pass verification", Security.verifyHMac(algorithm, HMAC_KEY, macdData));
            byte[] originalData = Security.readOriginalData(algorithm, macdData);
            Assert.assertArrayEquals("resulting data should match original", INTEGRITY_TEST_DATA, originalData);
        }
    }

    private static void testHmac(Security.HMAC algoritm, byte[] secret, byte[] data, String expectedHmac) throws MqttsnException {

        String hmac = new String(Security.hmac(algoritm, secret, data, true), StandardCharsets.UTF_8);
        byte[] hmacNoHex = Security.hmac(algoritm, secret, data, false);
        Assert.assertEquals("resulting hmac length should match", expectedHmac.length(), hmac.length());
        Assert.assertEquals("resulting raw hmac length should be half hex", expectedHmac.length() / 2, hmacNoHex.length);
        Assert.assertEquals("hmac should match", expectedHmac, hmac);
        byte[] wrongSecret = MqttsnUtils.arrayOf(secret.length + 1,  (byte) 0x01);
        System.arraycopy(secret, 0, wrongSecret, 0, secret.length);
        hmac = new String(Security.hmac(algoritm, wrongSecret, data, true), StandardCharsets.UTF_8);
        Assert.assertEquals("resulting (incorrect) hmac length should match", expectedHmac.length(), hmac.length());
        Assert.assertNotEquals("resulting (incorrect) hmac should NOT match", expectedHmac, hmac);
    }
}
