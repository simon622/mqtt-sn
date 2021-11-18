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

import java.util.UUID;

public class MqttsnWaitToken {

    private volatile boolean error = false;
    private volatile boolean complete = false;
    private volatile QueuedPublishMessage queuedPublishMessage;
    private volatile IMqttsnMessage message;
    private volatile IMqttsnMessage responseMessage;

    public MqttsnWaitToken(QueuedPublishMessage queuedPublishMessage){
        this.queuedPublishMessage = queuedPublishMessage;
    }

    public MqttsnWaitToken(IMqttsnMessage message){
        this.message = message;
    }

    public IMqttsnMessage getMessage() {
        return message;
    }

    public void setMessage(IMqttsnMessage message) {
        this.message = message;
    }

    public IMqttsnMessage getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(IMqttsnMessage responseMessage) {
        this.responseMessage = responseMessage;
    }

    public static MqttsnWaitToken from(QueuedPublishMessage queuedPublishMessage){
        return new MqttsnWaitToken(queuedPublishMessage);
    }

    public static MqttsnWaitToken from(IMqttsnMessage message){
        return new MqttsnWaitToken(message);
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isError() {
        return error;
    }

    public void markError() {
        complete = true;
        error = true;
    }

    public void markComplete() {
        this.complete = true;
        error = false;
    }

    @Override
    public String toString() {
        return "MqttsnWaitToken{" +
                "error=" + error +
                ", complete=" + complete +
                ", message=" + message +
                ", responseMessage=" + responseMessage +
                '}';
    }
}
