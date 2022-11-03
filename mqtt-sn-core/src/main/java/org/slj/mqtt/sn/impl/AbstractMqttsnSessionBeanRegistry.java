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

package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.impl.MqttsnSessionBeanImpl;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;
import org.slj.mqtt.sn.spi.MqttsnService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMqttsnSessionBeanRegistry
        extends MqttsnService {

    protected MqttsnSessionBeanImpl getSessionBean(IMqttsnSession session){
        if(session instanceof MqttsnSessionBeanImpl){
            return (MqttsnSessionBeanImpl) session;
        } else {
            throw new MqttsnRuntimeException("cannot derive session bean from session object");
        }
    }

    public void clearAll(){
        throw new UnsupportedOperationException("bean-registry provides no mechanism to clear by session");
    }

    public void clear(IMqttsnSession session){
        throw new UnsupportedOperationException("bean-registry provides no mechanism to clear by session");
    }

    public void clear(IMqttsnContext context){
        throw new UnsupportedOperationException("bean-registry provides no mechanism to clear by context");
    }
}
