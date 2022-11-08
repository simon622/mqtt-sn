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

package org.slj.mqtt.sn.model;

import org.slj.mqtt.sn.spi.IMqttsnMessage;

import java.util.Date;

public class TrafficEntry {

    public enum DIRECTION {INGRESS, EGRESS}
    private INetworkContext context;
    private IMqttsnMessage message;
    private byte[] arr;
    private DIRECTION direction;
    private Date created;
    private int size;
    private boolean error;

    public TrafficEntry(INetworkContext context, IMqttsnMessage message, byte[] arr, DIRECTION direction) {
        this.context = context;
        this.message = message;
        this.arr = arr;
        size = arr.length;
        error = message.isErrorMessage();
        this.direction = direction;
        created = new Date();
    }

    public byte[] getArr() {
        return arr;
    }

    public void setArr(byte[] arr) {
        this.arr = arr;
    }

    public DIRECTION getDirection() {
        return direction;
    }

    public void setDirection(DIRECTION direction) {
        this.direction = direction;
    }

    public INetworkContext getContext() {
        return context;
    }

    public void setContext(INetworkContext context) {
        this.context = context;
    }

    public IMqttsnMessage getMessage() {
        return message;
    }

    public void setMessage(IMqttsnMessage message) {
        this.message = message;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }
}
