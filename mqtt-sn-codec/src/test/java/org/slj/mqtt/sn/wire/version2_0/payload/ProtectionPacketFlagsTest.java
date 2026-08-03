/*
 * Copyright (c) 2026 Ian Craggs
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

package org.slj.mqtt.sn.wire.version2_0.payload;

import org.junit.Assert;
import org.junit.Test;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IProtectionScheme;

/**
 * Unit tests for the MQTT-SN 2.0 (CSD01) 3.17.2.3 Authentication Tag Length rules, exercised
 * against {@link ProtectionPacketFlags} directly with a test-double {@link IProtectionScheme}
 * (concrete crypto schemes live downstream in mqtt-sn-protection-runtimes, not reachable from
 * mqtt-sn-codec's test scope).
 */
public class ProtectionPacketFlagsTest {

    private static IProtectionScheme scheme(boolean authOnly, short nominalTagLengthInBytes) {
        return new IProtectionScheme() {
            public String getName() { return authOnly ? "TEST-AUTH-ONLY" : "TEST-AEAD"; }
            public byte getIndex() { return 0x00; }
            public short getNominalTagLengthInBytes() { return nominalTagLengthInBytes; }
            public short getBlockSizeInBytes() { return 16; }
            public boolean isAuthenticationOnly() { return authOnly; }
            public byte[] getCryptoMaterial(byte cryptoMaterialLength) { return null; }
        };
    }

    @Test
    public void testAeadSchemeRequiresTagLength0x1() throws MqttsnCodecException {
        IProtectionScheme aead = scheme(false, (short) 16);

        //-- 0x1 (nominal tag size) is the only value AEAD schemes may use
        ProtectionPacketFlags flags = new ProtectionPacketFlags((byte) 0x01, (byte) 0x00, (byte) 0x00, aead);
        Assert.assertEquals("AEAD tag length should be the scheme's nominal size", 16, flags.getAuthenticationTagLengthDecoded());
    }

    @Test(expected = MqttsnCodecException.class)
    public void testAeadSchemeRejectsProviderDefined() throws MqttsnCodecException {
        new ProtectionPacketFlags((byte) 0x00, (byte) 0x00, (byte) 0x00, scheme(false, (short) 16));
    }

    @Test(expected = MqttsnCodecException.class)
    public void testAeadSchemeRejectsTruncation() throws MqttsnCodecException {
        //-- 0xF (truncation) is only valid for Authentication Only schemes
        new ProtectionPacketFlags((byte) 0x0F, (byte) 0x00, (byte) 0x00, scheme(false, (short) 16));
    }

    @Test(expected = MqttsnCodecException.class)
    public void testReservedTagLength0x2Rejected() throws MqttsnCodecException {
        new ProtectionPacketFlags((byte) 0x02, (byte) 0x00, (byte) 0x00, scheme(true, (short) 32));
    }

    @Test(expected = MqttsnCodecException.class)
    public void testReservedTagLength0x3Rejected() throws MqttsnCodecException {
        new ProtectionPacketFlags((byte) 0x03, (byte) 0x00, (byte) 0x00, scheme(true, (short) 32));
    }

    @Test
    public void testAuthOnlyProviderDefinedIsSyntacticallyValid() throws MqttsnCodecException {
        //-- 0x0 is a legal flag value for Authentication Only schemes; the *byte length* just
        //-- can't be derived generically (it's provider-defined), so the decode accessor throws.
        ProtectionPacketFlags flags = new ProtectionPacketFlags((byte) 0x00, (byte) 0x00, (byte) 0x00, scheme(true, (short) 32));
        Assert.assertEquals(0x00, flags.getAuthenticationTagLength());
        try {
            flags.getAuthenticationTagLengthDecoded();
            Assert.fail("expected MqttsnCodecException for provider-defined tag length");
        } catch (MqttsnCodecException expected) {
            //-- expected
        }
    }

    @Test
    public void testAuthOnlyNominalTagSize() throws MqttsnCodecException {
        ProtectionPacketFlags flags = new ProtectionPacketFlags((byte) 0x01, (byte) 0x00, (byte) 0x00, scheme(true, (short) 32));
        Assert.assertEquals("Authentication Only tag length 0x1 should be the scheme's nominal size",
                32, flags.getAuthenticationTagLengthDecoded());
    }

    @Test
    public void testAuthOnlyTruncation() throws MqttsnCodecException {
        //-- 0x8 => 8 * 16 bits = 128 bits = 16 bytes, within a 32 byte (256 bit) nominal tag size
        ProtectionPacketFlags flags = new ProtectionPacketFlags((byte) 0x08, (byte) 0x00, (byte) 0x00, scheme(true, (short) 32));
        Assert.assertEquals(16, flags.getAuthenticationTagLengthDecoded());
    }

    @Test(expected = MqttsnCodecException.class)
    public void testAuthOnlyTruncationCannotExceedNominalSize() throws MqttsnCodecException {
        //-- 0xF => 15 * 16 bits = 240 bits = 30 bytes, bigger than a 16 byte (128 bit) nominal
        //-- tag size - MQTT-SN-3.17.2.3-8.
        new ProtectionPacketFlags((byte) 0x0F, (byte) 0x00, (byte) 0x00, scheme(true, (short) 16));
    }
}
