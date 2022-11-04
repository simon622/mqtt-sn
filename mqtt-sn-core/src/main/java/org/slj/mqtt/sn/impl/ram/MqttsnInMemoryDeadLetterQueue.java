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

package org.slj.mqtt.sn.impl.ram;

import org.slj.mqtt.sn.impl.AbstractDeadLetterQueue;
import org.slj.mqtt.sn.model.MqttsnDeadLetterQueueBean;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MqttsnInMemoryDeadLetterQueue extends AbstractDeadLetterQueue {

    private static final int INITIAL_CAPACITY = 250;

    private List<MqttsnDeadLetterQueueBean> list = new ArrayList<>(INITIAL_CAPACITY);;

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
    }

    @Override
    public void stop() throws MqttsnException {
        list.clear();
    }

    @Override
    protected void storeInternal(MqttsnDeadLetterQueueBean bean) {
        synchronized (list){
            if(size() >= registry.getOptions().getMaxMessagesInDeadLetterQueue()){
                // discard from the head add to the tail
                list.remove(0);
            }
            list.add(bean);
        }
    }

    @Override
    public List<MqttsnDeadLetterQueueBean> readMostRecent(int count) {
        if(list.isEmpty()){
            return Collections.emptyList();
        }
        synchronized (list){
            int startIdx = Math.max(0, ((list.size() - count)));
            List<MqttsnDeadLetterQueueBean> sub = list.subList(
                    startIdx,
                    startIdx + Math.min(list.size(), count));
            Collections.reverse(sub);
            return sub;
        }
    }

    public long size() {
        return list.size();
    }
}
