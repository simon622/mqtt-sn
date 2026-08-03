/*
 * Copyright (c) 2026 Ian Craggs
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

package org.slj.mqtt.sn.protection.runtime;

import org.slj.mqtt.sn.client.impl.MqttsnClientRuntimeRegistry;
import org.slj.mqtt.sn.client.impl.cli.MqttsnInteractiveClient;
import org.slj.mqtt.sn.client.impl.cli.MqttsnInteractiveClientLauncher;
import org.slj.mqtt.sn.codec.AbstractProtectionScheme;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.protection.impl.InMemoryProtectedSenderRegistry;
import org.slj.mqtt.sn.protection.impl.MqttsnProtectionService;
import org.slj.mqtt.sn.protection.impl.ProtectionUtils;
import org.slj.mqtt.sn.protection.spi.IProtectedSenderRegistry;
import org.slj.mqtt.sn.protection.spi.MqttsnProtectionOptions;
import org.slj.mqtt.sn.protection.spi.ProtectedSender;
import org.slj.mqtt.sn.spi.IMqttsnStorageService;
import org.slj.mqtt.sn.spi.IMqttsnTransport;

import java.util.List;

/**
 * @author Simon L Johnson
 */
public class ProtectionExampleClientCli {

    public static void main(String[] args) throws Exception {
        MqttsnInteractiveClientLauncher.launch(new MqttsnInteractiveClient() {
            protected AbstractMqttsnRuntimeRegistry createRuntimeRegistry(IMqttsnStorageService storageService, MqttsnOptions options, IMqttsnTransport transport) {
                options.withWireLoggingEnabled(true);

                //-- configure my protection details
//                MqttsnProtectionOptions protectionOptions =
//                        new MqttsnProtectionOptions().
//                                withProtectionScheme(AbstractProtectionScheme.HMAC_SHA256).
//                                //-- Authentication Tag Length 0x1 = use the scheme's own nominal
//                                //-- tag size (MQTT-SN 2.0 CSD01 3.17.2.3); 0x3 is reserved.
//                                withProtectionPacketFlags(new byte[] {(byte)0x01,(byte)0x00,(byte)0x00}).
//                                withProtectionKey(ProtectionUtils.loadKey("client1", "hmac"));

                MqttsnProtectionOptions protectionOptions =
                        new MqttsnProtectionOptions().
                                withProtectionScheme(AbstractProtectionScheme.AES_CCM_128_192).
                                //-- Authentication Tag Length MUST be 0x1 for AEAD (non
                                //-- "Authentication Only") schemes (MQTT-SN 2.0 CSD01 3.17.2.3-1) -
                                //-- 0xF is a truncation value only valid for Authentication Only
                                //-- schemes.
                                withProtectionPacketFlags(new byte[] {(byte)0x01,(byte)0x03,(byte)0x02}).
                                withProtectionKey(ProtectionUtils.loadKey("client1", "aes192"));

                //-- configure the trusted sender
                ProtectedSender sender = new ProtectedSender("gateway1",
                        List.of(ProtectionUtils.loadKey("gateway1", "hmac")));
                IProtectedSenderRegistry protectedSenderRegistry =
                        new InMemoryProtectedSenderRegistry(List.of(sender), protectionOptions);

                options.withSecurityOptions(protectionOptions);

            	AbstractMqttsnRuntimeRegistry registry = MqttsnClientRuntimeRegistry.defaultConfiguration(storageService, options).
                        withTransport(transport).
                        withService(protectedSenderRegistry).
                        withSecurityService(new MqttsnProtectionService());
                return registry;
            }
        });
    }
}
