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

package org.slj.mqtt.sn.load.runner;

import org.slj.mqtt.sn.load.ExecutionInput;
import org.slj.mqtt.sn.load.ExecutionProfile;
import org.slj.mqtt.sn.load.ExecutionProgress;
import org.slj.mqtt.sn.load.LoadTestException;

public abstract class AbstractLoadTest {

    protected final Class<? extends ExecutionProfile> profile;
    protected final int numInstances;

    public AbstractLoadTest(Class<? extends ExecutionProfile> profile, int numInstances){
        this.profile = profile;
        this.numInstances = numInstances;
    }

    protected final ExecutionProfile createProfile(ExecutionInput input) throws LoadTestException {
        try {
            ExecutionProfile impl = profile.newInstance();
            ExecutionProgress progress = impl.initializeProfile(input);
            impl.setProgress(progress);
            return impl;
        } catch(Exception e){
            throw new LoadTestException("error creating execution profile;", e);
        }
    }

    public abstract void start(ExecutionInput input) throws LoadTestException;
}
