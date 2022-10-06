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

package org.slj.mqtt.sn.http.impl;

import org.slj.mqtt.sn.http.HttpConstants;
import org.slj.mqtt.sn.http.HttpUtils;
import org.slj.mqtt.sn.http.IHttpRequestResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;

public abstract class HttpRequestResponse implements IHttpRequestResponse {

    public String contextPath;
    public URI httpRequestUri;
    public HttpConstants.METHOD method;
    public int responseCode;
    public int responseSize;

    private Map<String, String> parameters;

    public HttpRequestResponse(HttpConstants.METHOD method, URI httpRequestUri, String contextPath) {
        this.method = method;
        this.httpRequestUri = httpRequestUri;
        this.contextPath = contextPath;
    }

    public HttpConstants.METHOD getMethod() {
        return method;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getContextRelativePath() {
        return HttpUtils.getContextRelativePath(contextPath,
                httpRequestUri.getPath());
    }

    public URI getHttpRequestUri() {
        return httpRequestUri;
    }

    @Override
    public int getResponseCode() {
        return responseCode;
    }

    public int getResponseSize() {
        return responseSize;
    }

    public void setResponseContentType(String mimeType, Charset charset){
        addResponseHeader(HttpConstants.CONTENT_TYPE_HEADER,
                HttpUtils.getContentTypeHeaderValue(mimeType, charset.toString()));
    }

    public final void sendResponseHeaders(int httpCode, int responseSize) throws IOException {
        this.responseCode = httpCode;
        this.responseSize = responseSize;
        sendResponseHeadersInternal(httpCode, responseSize);
    }

    @Override
    public synchronized String getParameter(String key) {
        if(parameters == null){
            String query = httpRequestUri.getQuery();
            parameters = HttpUtils.parseQueryString(query);
        }
        return parameters.get(key);
    }

    @Override
    public String toString() {
        return "HttpRequestResponse{" +
                "contextPath='" + contextPath + '\'' +
                ", httpRequestUri=" + httpRequestUri +
                ", method=" + method +
                '}';
    }

    protected abstract void sendResponseHeadersInternal(int httpCode, int size) throws IOException ;
    protected abstract Map<String, String> getRequestHeaders();
}
