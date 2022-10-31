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

import org.slj.mqtt.sn.model.IMqttsnDataRef;

/**
 * The message registry is a normalised view of transiting messages, it context the raw payload of publish operations
 * so light weight references to the payload can exist in multiple storage systems without duplication of data.
 * For example, when running in gateway mode, the same message my reside in queues for numerous devices which are
 * in different connection states. We should not store payloads N times in this case.
 *
 * The lookup is a simple UUID -> byte[] relationship. It is up to the registry implementation to decide how to store
 * this data.
 *
 */
public interface IMqttsnMessageRegistry <T extends IMqttsnRuntimeRegistry> extends IMqttsnRegistry<T>{

    void tidy() throws MqttsnException ;

    IMqttsnDataRef add(byte[] data) throws MqttsnException ;

//    Integer add(byte[] data, Date expires) throws MqttsnException;

    boolean remove(IMqttsnDataRef messageId) throws MqttsnException;

//    boolean removeWhenCommitted(Integer messageId) throws MqttsnException;

    byte[] get(IMqttsnDataRef messageId) throws MqttsnException;

    long size();
}
