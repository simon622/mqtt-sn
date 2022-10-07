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

package org.slj.mqtt.sn.http.sun;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;
import org.slj.mqtt.sn.http.HttpConstants;
import org.slj.mqtt.sn.http.IHttpRequestResponseHandler;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class SunHttpHandlerProxy implements HttpHandler  {

    protected final Logger LOG =
            Logger.getLogger(getClass().getName());

    private IHttpRequestResponseHandler handler;

    public SunHttpHandlerProxy(IHttpRequestResponseHandler handler){
        this.handler = handler;
    }

    public void handle(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        String contextPath = exchange.getHttpContext().getPath();
        String requestMethod = exchange.getRequestMethod();
        HttpConstants.METHOD method = HttpConstants.METHOD.valueOf(requestMethod);
        SunHttpRequestResponse sunHttpRequestResponse =
                new SunHttpRequestResponse(method, requestURI, contextPath, exchange);
//        printRequestInfo(exchange);
        handler.handleRequest(sunHttpRequestResponse);
    }

    public static void printRequestInfo(HttpExchange exchange) {
        System.out.println("-- headers --");
        Headers requestHeaders = exchange.getRequestHeaders();
        requestHeaders.entrySet().forEach(System.out::println);

        System.out.println("-- principle --");
        HttpPrincipal principal = exchange.getPrincipal();
        System.out.println(principal);

        System.out.println("-- HTTP method --");
        String requestMethod = exchange.getRequestMethod();
        System.out.println(requestMethod);

        System.out.println("-- query --");
        URI requestURI = exchange.getRequestURI();
        String query = requestURI.getQuery();
        System.out.println(query);
    }
}
