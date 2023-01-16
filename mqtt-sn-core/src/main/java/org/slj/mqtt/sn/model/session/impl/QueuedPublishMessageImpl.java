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

package org.slj.mqtt.sn.model.session.impl;

import org.slj.mqtt.sn.PublishData;
import org.slj.mqtt.sn.model.IDataRef;
import org.slj.mqtt.sn.model.MqttsnWaitToken;
import org.slj.mqtt.sn.model.session.IQueuedPublishMessage;

import java.io.Serializable;

/**
 * Lightweight meta-data reference to a message which will reside in client queues. NOTE: the payload of
 * the message itself NOR the topic specification are included, this can be obtained JIT from the
 * appropriate registries so we don't duplicate data across many queues.
 */
public class QueuedPublishMessageImpl implements Serializable, Comparable, IQueuedPublishMessage {

    private PublishData data;
    private long created;
    private int retryCount;
    private IDataRef dataRefId;
    private int packetId;
    private transient MqttsnWaitToken token;
    private int grantedQoS;

    public QueuedPublishMessageImpl() {
    }

    public QueuedPublishMessageImpl(IDataRef dataRefId, PublishData data) {
        this.created = System.currentTimeMillis();
        this.dataRefId = dataRefId;
        this.data = data;
        this.retryCount = 0;
        this.grantedQoS = data.getQos();
    }

    public IDataRef getDataRefId() {
        return dataRefId;
    }

    public void setDataRefId(IDataRef dataRefId) {
        this.dataRefId = dataRefId;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetry(){
        retryCount++;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public PublishData getData() {
        return data;
    }

    public void setData(PublishData data) {
        this.data = data;
    }

    public MqttsnWaitToken getToken() {
        return token;
    }

    public void setToken(MqttsnWaitToken token) {
        this.token = token;
    }

    @Override
    public int getGrantedQoS() {
        return grantedQoS;
    }

    public void setGrantedQoS(int grantedQoS) {
        this.grantedQoS = grantedQoS;
    }

    @Override
    public String toString() {
        return "MqttsnQueuedPublishMessageImpl{" +
                "data=" + data +
                ", created=" + created +
                ", retryCount=" + retryCount +
                ", dataRefId=" + dataRefId +
                ", packetId=" + packetId +
                ", token=" + token +
                ", grantedQoS=" + grantedQoS +
                '}';
    }

    public long getCreated() {
        return created;
    }

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }

    @Override
    public int compareTo(Object o) {
        if(o instanceof QueuedPublishMessageImpl){
            return (int) (created - ((QueuedPublishMessageImpl)o).getCreated());
//            return created.compareTo(((MqttsnQueuedPublishMessageImpl)o).getCreated());
        }
        return 0;
    }
}
