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

package org.slj.mqtt.sn.console.http.sun;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.slj.mqtt.sn.console.http.HttpConstants;
import org.slj.mqtt.sn.console.http.impl.HttpRequestResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;

public class SunHttpRequestResponse extends HttpRequestResponse {

    public HttpExchange exchange;

    public SunHttpRequestResponse(HttpConstants.METHOD method, URI httpRequestUri, String contextPath, HttpExchange exchange) {
        super(method, httpRequestUri, contextPath);
        this.exchange = exchange;
    }

    public OutputStream getResponseBody(){
        return exchange.getResponseBody();
    }

    @Override
    public InputStream getRequestBody() {
        return exchange.getRequestBody();
    }

    public void addResponseHeader(String headerKey, String headerValue){
        exchange.getResponseHeaders().add(headerKey,
                headerValue);
    }

    protected void sendResponseHeadersInternal(int httpCode, int size) throws IOException {
        exchange.sendResponseHeaders(httpCode, size);
    }

    @Override
    public String getRequestHeader(String key) {
        return exchange.getRequestHeaders().getFirst(key);
    }

    @Override
    protected Map<String, String> getRequestHeaders() {
        Headers headers = exchange.getRequestHeaders();
        Set<String> s = headers.keySet();
        Iterator<String> itr = s.iterator();
        HashMap<String, String> map = new HashMap<>();
        while(itr.hasNext()){
            String key = itr.next();
            String value = headers.getFirst(key);
            map.put(key, value);
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public void commit() {
        try {
            exchange.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
