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

package org.slj.mqtt.sn.impl.metrics;

public interface IMqttsnMetrics {

    int DEFAULT_MAX_SAMPLES = 100;  // each metric will by default maintain this number of samples
    int DEFAULT_SAMPLES_TIME_MILLIS = 1000;  // each metric will by default maintain this number of samples
    int DEFAULT_SNAPSHOT_TIME_MILLIS = 30000;  // each metric will by default maintain this number of samples

    String NETWORK_BYTES_IN = "NETWORK_BYTES_IN";
    String NETWORK_BYTES_OUT = "NETWORK_BYTES_OUT";

    String PUBLISH_MESSAGE_IN = "PUBLISH_MESSAGE_IN";
    String PUBLISH_MESSAGE_OUT = "PUBLISH_MESSAGE_OUT";

    String NETWORK_REGISTRY_COUNT = "NETWORK_REGISTRY_COUNT";
    String TOPIC_REGISTRY_COUNT = "TOPIC_REGISTRY_COUNT";
    String MESSAGE_REGISTRY_COUNT = "MESSAGE_REGISTRY_COUNT";
    String DLQ_REGISTRY_COUNT = "DLQ_REGISTRY_COUNT";

    String SESSION_ACTIVE_REGISTRY_COUNT = "SESSION_ACTIVE_REGISTRY_COUNT";
    String SESSION_DISCONNECTED_REGISTRY_COUNT = "SESSION_DISCONNECTED_REGISTRY_COUNT";
    String SESSION_LOST_REGISTRY_COUNT = "SESSION_LOST_REGISTRY_COUNT";
    String SESSION_ASLEEP_REGISTRY_COUNT = "SESSION_ASLEEP_REGISTRY_COUNT";
    String SESSION_AWAKE_REGISTRY_COUNT = "SESSION_AWAKE_REGISTRY_COUNT";

    String SYSTEM_VM_MEMORY_USED = "SYSTEM_VM_MEMORY_USED";
    String SYSTEM_VM_THREADS_USED = "SYSTEM_VM_THREADS_USED";
}