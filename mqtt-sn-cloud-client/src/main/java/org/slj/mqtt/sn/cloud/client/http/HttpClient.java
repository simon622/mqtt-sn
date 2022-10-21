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

package org.slj.mqtt.sn.cloud.client.http;

import org.slj.mqtt.sn.utils.Files;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Super quick HTTP client implementation using the inbuild HttpUrlConnection in the .net packages.
 * NOTE: do not use for high traffic situations, please for those defer to HttpClient in commons.
 */
public class HttpClient {

    public static HttpResponse head(String url, int readTimeout) throws IOException {
        HttpURLConnection connection = null;
        URL serverAddress = null;
        try {
            serverAddress = new URL(url);
            connection = null;
            connection = (HttpURLConnection) serverAddress.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setDoOutput(true);
            connection.setReadTimeout(readTimeout);
            connection.connect();
            return createResponse(url, connection, false);
        } finally {
            try {connection.disconnect();} catch(Throwable t) {}
        }
    }

    public static HttpResponse get(String url, int connectTimeoutMillis, int readTimeoutMillis)
            throws IOException {

        HttpURLConnection connection = null;
        InputStream is = null;
        URL serverAddress = null;
        try {
            serverAddress = new URL(url);
            connection = null;
            connection = (HttpURLConnection) serverAddress.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setReadTimeout(readTimeoutMillis);
            connection.setConnectTimeout(connectTimeoutMillis);
            connection.connect();
            return createResponse(url, connection, true);
        } finally {
            try {connection.disconnect();} catch(Throwable t) {}
            try {is.close();} catch(Throwable t) {}
        }
    }

    public static HttpResponse post(String url, InputStream is, int connectTimeoutMillis, int readTimeoutMillis)
            throws IOException {

        HttpURLConnection connection = null;
        URL serverAddress = null;
        try {
            serverAddress = new URL(url);
            connection = null;
            connection = (HttpURLConnection) serverAddress.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setReadTimeout(readTimeoutMillis);
            connection.setConnectTimeout(connectTimeoutMillis);
            Files.copy(is, connection.getOutputStream(), 1024);
            connection.connect();
            return createResponse(url, connection, true);
        } finally {
            try {connection.disconnect();} catch(Throwable t) {}
            try {is.close();} catch(Throwable t) {}
        }
    }

    private static HttpResponse createResponse(String url, HttpURLConnection connection, boolean readBody) throws IOException {
        HttpResponse response = new HttpResponse();
        response.setRequestUrl(url);
        response.setContentLength(connection.getContentLength());
        response.setContentType(connection.getContentType());
        response.setStatusCode(connection.getResponseCode());
        response.setStatusMessage(connection.getResponseMessage());
        response.setContentEncoding(connection.getContentEncoding());
        if(readBody){
            InputStream is = null;
            try {
                int code = response.getStatusCode();
                if(code > 299) {
                    is = connection.getErrorStream();
                } else {
                    is = connection.getInputStream();
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1){
                    baos.write(buffer, 0, bytesRead);
                }
                response.setResponseBody(baos.toByteArray());
            }
            finally {
                try {
                    is.close();
                } catch(Exception e){}
            }
        }
        return response;
    }
}
