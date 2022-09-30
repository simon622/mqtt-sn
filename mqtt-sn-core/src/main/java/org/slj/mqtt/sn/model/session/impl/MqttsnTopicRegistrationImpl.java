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

import org.slj.mqtt.sn.model.session.IMqttsnTopicRegistration;

public class MqttsnTopicRegistrationImpl implements IMqttsnTopicRegistration {

    private boolean confirmed;
    private final String topicPath;
    private int aliasId;

    public MqttsnTopicRegistrationImpl(final String topicPath, final int aliasId, final boolean confirmed){
        this.topicPath = topicPath;
        this.aliasId = aliasId;
        this.confirmed = confirmed;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public String getTopicPath() {
        return topicPath;
    }

    public int getAliasId() {
        return aliasId;
    }

    public void setAliasId(int aliasId) {
        this.aliasId = aliasId;
    }

    @Override
    public String toString() {
        return "MqttsnTopicRegistrationImpl{" +
                "confirmed=" + confirmed +
                ", topicPath='" + topicPath + '\'' +
                ", aliasId=" + aliasId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MqttsnTopicRegistrationImpl that = (MqttsnTopicRegistrationImpl) o;
        return topicPath.equals(that.topicPath);
    }

    @Override
    public int hashCode() {
        int result = topicPath.hashCode();
        return result;
    }
}
