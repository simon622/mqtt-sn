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

package org.slj.mqtt.sn.console.http.impl.handlers;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.slj.mqtt.sn.console.http.HttpUtils;
import org.slj.mqtt.sn.console.http.IHttpRequestResponse;
import org.slj.mqtt.sn.console.http.impl.AbstractHttpRequestResponseHandler;

import java.io.IOException;

public class AsyncContentHandler extends AbstractHttpRequestResponseHandler {

    private String[] allowedPages;
    private String root;

    public AsyncContentHandler(ObjectWriter writer, String root, String... allowedPages){
        super(writer);
        this.allowedPages = allowedPages;
        this.root = root;
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse request) throws IOException {
        String page = request.getParameter("page");
        if(in(page, allowedPages)){
            String filePath = HttpUtils.combinePaths(root, page);
            writeDataFromResource(request, filePath);
        }
        else {
            sendNotFoundResponse(request);
        }
    }

    public static boolean in(String needle, String... haystack){
        if(haystack == null) return false;
        if(needle == null) return false;
        for (int i = 0; i < haystack.length; i++) {
            if(haystack[i].equals(needle)) return true;
        }
        return false;
    }
}
