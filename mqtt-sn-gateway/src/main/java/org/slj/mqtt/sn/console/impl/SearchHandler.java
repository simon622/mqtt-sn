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

import com.fasterxml.jackson.databind.ObjectWriter;
import org.slj.mqtt.sn.console.http.HttpBadRequestException;
import org.slj.mqtt.sn.console.http.HttpConstants;
import org.slj.mqtt.sn.console.http.IHttpRequestResponse;
import org.slj.mqtt.sn.console.http.impl.AbstractHttpRequestResponseHandler;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;

import java.io.IOException;

public class SearchHandler extends AbstractHttpRequestResponseHandler {

    static final String SEARCH_TERM = "searchTerm";
    private IMqttsnRuntimeRegistry registry;

    public SearchHandler(ObjectWriter writer, IMqttsnRuntimeRegistry registry) {
        super(writer);
        this.registry = registry;
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse request) throws HttpBadRequestException, IOException {

        String prefix = getMandatoryParameter(request, SEARCH_TERM);
        Object[] options = generateData(prefix);
        writeJSONResponse(request, HttpConstants.SC_OK, writer.writeValueAsBytes(options));
    }

    protected Option[] generateData(String prefix){
        Option[] a = new Option[10];
        for (int i = 0; i<10; i++){
            Option o = new Option();
            o.label = prefix + " fill" + i;
            o.value = o.label;
            a[i] = o;
        }
        return a;
    }

    class Option{
        public String label;
        public String value;
    }


    /*
    [{
                "label": "SIMON",
                "value": "FIT"
            }]
     */
}