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

package org.slj.mqtt.sn.test;

import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.impl.MqttsnContextFactory;
import org.slj.mqtt.sn.impl.MqttsnMessageQueueProcessor;
import org.slj.mqtt.sn.impl.MqttsnSecurityService;
import org.slj.mqtt.sn.impl.ram.*;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.net.NetworkAddressRegistry;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;
import org.slj.mqtt.sn.wire.version1_2.Mqttsn_v1_2_Codec;

public class MqttsnTestRuntimeRegistry extends AbstractMqttsnRuntimeRegistry {

    public MqttsnTestRuntimeRegistry(final MqttsnOptions options){
        super(options);
    }

    public static MqttsnTestRuntimeRegistry defaultConfiguration(final MqttsnOptions options, final boolean clientMode){
        final MqttsnTestRuntimeRegistry registry = (MqttsnTestRuntimeRegistry) new MqttsnTestRuntimeRegistry(options).
                withMessageRegistry(new MqttsnInMemoryMessageRegistry()).
                withNetworkAddressRegistry(new NetworkAddressRegistry(options.getMaxNetworkAddressEntries())).
                withWillRegistry(new MqttsnInMemoryWillRegistry()).
                withMessageQueue(new MqttsnInMemoryMessageQueue()).
                withContextFactory(new MqttsnContextFactory()).
                withSecurityService(new MqttsnSecurityService()).
                withTopicRegistry(new MqttsnInMemoryTopicRegistry()).
                withQueueProcessor(new MqttsnMessageQueueProcessor(clientMode)).
                withSubscriptionRegistry(new MqttsnInMemorySubscriptionRegistry()).
                withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2).
                withMessageStateService(new MqttsnInMemoryMessageStateService(clientMode));
        return registry;
    }

    @Override
    protected void validateOnStartup() throws MqttsnRuntimeException {

    }
}