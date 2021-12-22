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
package org.slj.mqtt.sn.spi;

import java.io.Serializable;

/**
 * Represents a wire message that can be manipulated by a gateway OR client for transport by
 * changing its msgId.
 *
 * @author Simon Johnson <simon622 AT gmail DOT com>
 * See Mqttsn specification at http://www.mqtt.org/new/wp-content/uploads/2009/06/MQTT-SN_spec_v1.2.pdf
 */
public interface IMqttsnMessage extends Serializable {

    /**
     * Get a user friendly version of the Message object.
     * For example;
     * "MqttsnConnect"
     * "MqttsnDisconnect"
     */
    String getMessageName();

    /**
     * If the underlying message uses a msg id as part of its contract
     */
    boolean needsId();

    void setId(int msgId);

    int getId();

    int getMessageType();

    int getReturnCode();

    boolean isErrorMessage();
}
