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

package org.slj.mqtt.sn.gateway.connector.aws.iotcore;

import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.gateway.cli.MqttsnInteractiveGatewayLauncher;
import org.slj.mqtt.sn.gateway.cli.MqttsnInteractiveGatewayWithKeystore;
import org.slj.mqtt.sn.gateway.impl.MqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.gateway.impl.gateway.type.MqttsnAggregatingGateway;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.spi.IMqttsnStorageService;
import org.slj.mqtt.sn.spi.IMqttsnTransport;

public class AWSIoTCoreAggregatingGatewayInteractiveMain {
    public static void main(String[] args) throws Exception {
        MqttsnInteractiveGatewayLauncher.launch(new MqttsnInteractiveGatewayWithKeystore() {
            protected AbstractMqttsnRuntimeRegistry createRuntimeRegistry(IMqttsnStorageService storageService,MqttsnOptions options, IMqttsnTransport transport) {

                IMqttsnStorageService namespacePreferences = storageService.getPreferenceNamespace(
                        AWSIoTCoreMqttsnConnector.DESCRIPTOR);
                MqttsnConnectorOptions connectorOptions = new MqttsnConnectorOptions();
                storageService.initializeFieldsFromStorage(connectorOptions);
                namespacePreferences.initializeFieldsFromStorage(connectorOptions);

                return MqttsnGatewayRuntimeRegistry.defaultConfiguration(storageService, (MqttsnGatewayOptions) options).
                        withConnector(new AWSIoTCoreMqttsnConnector(
                                AWSIoTCoreMqttsnConnector.DESCRIPTOR, connectorOptions)).
                        withBackendService(new MqttsnAggregatingGateway()).
                        withTransport(createTransport(storageService)).
                        withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2);
            }
        }, true, "Welcome to the AWS IoT Core version of the gateway. You will need to connect your gateway to your AWS IoT via the credentials available in your AWS console.");
    }
}
