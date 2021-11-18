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

package org.slj.mqtt.sn.gateway.spi;

import org.slj.mqtt.sn.model.TopicInfo;

public class SubscribeResult extends Result {

    private TopicInfo topicInfo;
    private int grantedQoS;

    public SubscribeResult(STATUS status, int returnCode, String message) {
        super(status);
        setMessage(message);
        setReturnCode(returnCode);
    }

    public SubscribeResult(STATUS status) {
        super(status);
    }

    public SubscribeResult(TopicInfo info, int grantedQoS) {
        super(STATUS.SUCCESS);
        this.topicInfo = info;
        this.grantedQoS = grantedQoS;
    }

    public TopicInfo getTopicInfo() {
        return topicInfo;
    }

    public void setTopicInfo(TopicInfo topicInfo) {
        this.topicInfo = topicInfo;
    }

    public int getGrantedQoS() {
        return grantedQoS;
    }

    @Override
    public String toString() {
        return "SubscribeResult{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", returnCode=" + returnCode +
                ", topicInfo=" + topicInfo +
                ", grantedQoS=" + grantedQoS +
                '}';
    }

    public void setGrantedQoS(int grantedQoS) {
        this.grantedQoS = grantedQoS;
    }
}
