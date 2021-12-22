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

package org.slj.mqtt.sn.codec;

import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.wire.version1_2.Mqttsn_v1_2_Codec;
import org.slj.mqtt.sn.wire.version2_0.Mqttsn_v2_0_Codec;

public interface MqttsnCodecs {

    /**
     * Version 1.2 support of the MQTT-SN specification
     *
     * See Mqttsn specification at http://www.mqtt.org/new/wp-content/uploads/2009/06/MQTT-SN_spec_v1.2.pdf
     */
    IMqttsnCodec MQTTSN_CODEC_VERSION_1_2 = new Mqttsn_v1_2_Codec();

    /**
     * Version 2.0 WD 14 support of the MQTT-SN specification
     */
    IMqttsnCodec MQTTSN_CODEC_VERSION_2_0 = new Mqttsn_v2_0_Codec();

}
