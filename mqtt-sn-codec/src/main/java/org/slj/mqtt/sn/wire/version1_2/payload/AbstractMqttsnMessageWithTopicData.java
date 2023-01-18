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

package org.slj.mqtt.sn.wire.version1_2.payload;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

public abstract class AbstractMqttsnMessageWithTopicData extends AbstractMqttsnMessageWithFlagsField {

    protected byte[] topicData;

    public String getTopicName() {
        if (topicType == MqttsnConstants.TOPIC_PREDEFINED){

        }
//            throw new IllegalStateException("unable to parse string data from predefined topic alias");
        if(topicType == MqttsnConstants.TOPIC_SHORT){
            //-- handle single char short topic names
            if(topicData.length == 2 && topicData[1] == 0x00)
                return new String(new byte[]{topicData[0]}, MqttsnConstants.CHARSET);
        }
        return new String(topicData, MqttsnConstants.CHARSET);
    }

    public void setTopicName(String topicName) {
        setTopicType(topicName != null && topicName.length() <= 2 ? MqttsnConstants.TOPIC_SHORT : MqttsnConstants.TOPIC_NORMAL);
        if(topicName.length() == 1){
            topicData = new byte[]{topicName.getBytes(MqttsnConstants.CHARSET)[0], 0x00};
        } else {
            topicData = topicName.getBytes(MqttsnConstants.CHARSET);
        }
    }

    public void setPredefinedTopicAlias(int topicAlias) {
        setTopicType(MqttsnConstants.TOPIC_PREDEFINED);
        setTopicAliasId(topicAlias);
    }

    public void setNormalTopicAlias(int topicAlias) {
        setTopicType(MqttsnConstants.TOPIC_NORMAL);
        setTopicAliasId(topicAlias);
    }

    public int readTopicDataAsInteger() {
        return MqttsnWireUtils.read16bit(topicData[0], topicData[1]);
    }

    public byte[] getTopicData() {
        return topicData;
    }

    protected void setTopicData(byte[] data) {
        topicData = data;
    }

    protected void setTopicAliasId(int topicAlias) {
        topicData = new byte[2];
        topicData[0] = (byte) ((topicAlias >> 8) & 0xFF);
        topicData[1] = (byte) (topicAlias & 0xFF);
    }
}
