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

package org.slj.mqtt.sn.console.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slj.mqtt.sn.console.http.HttpConstants;
import org.slj.mqtt.sn.console.http.HttpException;
import org.slj.mqtt.sn.console.http.HttpInternalServerError;
import org.slj.mqtt.sn.console.http.IHttpRequestResponse;
import org.slj.mqtt.sn.impl.ram.MqttsnFileBackedInMemoryMessageQueue;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;

import java.io.IOException;

public class CommandHandler extends MqttsnConsoleAjaxRealmHandler {

    static final String COMMAND = "_cmd";


    public CommandHandler(ObjectMapper mapper, IMqttsnRuntimeRegistry registry) {
        super(mapper, registry);
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse request) throws HttpException, IOException {
        try {
            String command = getMandatoryParameter(request, COMMAND);
            switch (command){
                case "clear-cache":
                    if(getRegistry().getMessageQueue() instanceof MqttsnFileBackedInMemoryMessageQueue){
                        ((MqttsnFileBackedInMemoryMessageQueue)getRegistry().getMessageQueue()).clearFilesystemOnly();
                        writeMessageBeanResponse(request, HttpConstants.SC_OK, new Message("Successfully cleared system cache"));
                        return;
                    }
                    else {
                        writeMessageBeanResponse(request, HttpConstants.SC_OK, new Message("Unable to clear system cache", false));
                        return;
                    }
            }

            writeSimpleOKResponse(request);
        } catch(Exception e){
            throw new HttpInternalServerError("error determining status", e);
        }
    }
}

