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

import org.slj.mqtt.sn.utils.Files;

import java.io.File;
import java.io.IOException;

/**
 * Standalone utility to (re)generate the .key resource files. Not a JUnit test (no assertions) -
 * named to avoid Surefire's default test discovery, and run manually, with this module's directory
 * as the working directory, when the key material needs to be regenerated.
 *
 * @author Simon L Johnson
 */
public class KeyFileGenerator {

    static byte[] gatewayProtectionKeyHmac =  new byte[] {
            (byte)0x11,(byte)0x22,(byte)0x33,(byte)0x44,(byte)0x55,(byte)0x61,(byte)0x00,(byte)0x52,(byte)0x15,(byte)0xe9,(byte)0x02,(byte)0xcd,(byte)0xfa,(byte)0x4b,(byte)0x1e,(byte)0x0b,
            (byte)0x9d,(byte)0x25,(byte)0xe4,(byte)0x97,(byte)0xea,(byte)0x71,(byte)0xd7,(byte)0x54,(byte)0x39,(byte)0x22,(byte)0x4e,(byte)0x55,(byte)0x80,(byte)0x4a,(byte)0xea,(byte)0x2e,
            (byte)0x7a,(byte)0x9c,(byte)0x97,(byte)0x53,(byte)0x16,(byte)0xd4,(byte)0x27,(byte)0xcc,(byte)0x6e,(byte)0x00,(byte)0xdb,(byte)0xe5,(byte)0xc2,(byte)0xe3,(byte)0x89,(byte)0x12,
            (byte)0x7a,(byte)0x9c,(byte)0x97,(byte)0x53,(byte)0x16,(byte)0xd4,(byte)0x27,(byte)0xcc,(byte)0x6e,(byte)0x00,(byte)0xdb,(byte)0xe5,(byte)0xc2,(byte)0xe3,(byte)0x89,(byte)0x12};
    static byte[] clientProtectionKeyHmac = new byte[] {
            (byte)0x8d,(byte)0x8c,(byte)0x0e,(byte)0x21,(byte)0x13,(byte)0x61,(byte)0x00,(byte)0x52,(byte)0x15,(byte)0xe9,(byte)0x02,(byte)0xcd,(byte)0xfa,(byte)0x4b,(byte)0x1e,(byte)0x0b,
            (byte)0x9d,(byte)0x25,(byte)0xe4,(byte)0x97,(byte)0xea,(byte)0x71,(byte)0xd7,(byte)0x54,(byte)0x39,(byte)0x22,(byte)0x4e,(byte)0x55,(byte)0x80,(byte)0x4a,(byte)0xea,(byte)0x2e,
            (byte)0x7a,(byte)0x9c,(byte)0x97,(byte)0x53,(byte)0x16,(byte)0xd4,(byte)0x27,(byte)0xcc,(byte)0x6e,(byte)0x00,(byte)0xdb,(byte)0xe5,(byte)0xc2,(byte)0xe3,(byte)0x89,(byte)0x12,
            (byte)0x7a,(byte)0x9c,(byte)0x97,(byte)0x53,(byte)0x16,(byte)0xd4,(byte)0x27,(byte)0xcc,(byte)0x6e,(byte)0x00,(byte)0xdb,(byte)0xe5,(byte)0xc2,(byte)0xe3,(byte)0x89,(byte)0x12};
    static byte[] clientProtectionKeyAes256 = new byte[] {
            (byte)0x8d,(byte)0x8c,(byte)0x0e,(byte)0x21,(byte)0x13,(byte)0x61,(byte)0x00,(byte)0x52,(byte)0x15,(byte)0xe9,(byte)0x02,(byte)0xcd,(byte)0xfa,(byte)0x4b,(byte)0x1e,(byte)0x0b,
            (byte)0x9d,(byte)0x25,(byte)0xe4,(byte)0x97,(byte)0xea,(byte)0x71,(byte)0xd7,(byte)0x54,(byte)0x39,(byte)0x22,(byte)0x4e,(byte)0x55,(byte)0x80,(byte)0x4a,(byte)0xea,(byte)0x2e};
    static byte[] clientProtectionKeyAes192 = new byte[] {
            (byte)0x8d,(byte)0x8c,(byte)0x0e,(byte)0x21,(byte)0x13,(byte)0x61,(byte)0x00,(byte)0x52,(byte)0x15,(byte)0xe9,(byte)0x02,(byte)0xcd,(byte)0xfa,(byte)0x4b,(byte)0x1e,(byte)0x0b,
            (byte)0x9d,(byte)0x25,(byte)0xe4,(byte)0x97,(byte)0xea,(byte)0x71,(byte)0xd7,(byte)0x54};
    static byte[] clientProtectionKeyAes128 = new byte[] {
            (byte)0x8d,(byte)0x8c,(byte)0x0e,(byte)0x21,(byte)0x13,(byte)0x61,(byte)0x00,(byte)0x52,(byte)0x15,(byte)0xe9,(byte)0x02,(byte)0xcd,(byte)0xfa,(byte)0x4b,(byte)0x1e,(byte)0x0b};

    public static void main(String[] args) throws IOException {
        writeToFile("client-protection-aes128.key", clientProtectionKeyAes128);
        writeToFile("client-protection-aes192.key", clientProtectionKeyAes192);
        writeToFile("client-protection-aes256.key", clientProtectionKeyAes256);
        writeToFile("client-protection-hmac.key", clientProtectionKeyHmac);
        writeToFile("gateway-protection-hmac.key", gatewayProtectionKeyHmac);
    }

    public static void writeToFile(String fileName, byte[] arr) throws IOException {
        Files.writeWithLock(new File("src/main/resources", fileName), arr);
    }
}
