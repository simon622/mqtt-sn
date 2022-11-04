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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMqttsnService implements IMqttsnService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected IMqttsnRuntimeRegistry registry;
    protected volatile boolean running = false;

    public AbstractMqttsnService(){
    }

    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        this.registry = runtime;
        running = true;
    }

    public void stop() throws MqttsnException {
        running = false;
    }

    protected IMqttsnRuntimeRegistry getRegistry(){
        return registry;
    }

    public boolean running(){
        return running;
    }
}
