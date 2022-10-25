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

package org.slj.mqtt.sn.console.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class HttpUtils {

    public static String getMimeTypeFromFileExtension(String fileExtension) throws MimeTypeNotFoundException {
        String mime = HttpConstants.MIME_MAP.get(fileExtension);
        if(mime == null) throw new MimeTypeNotFoundException("unable to find mimeType for " + fileExtension);
        return mime;
    }

    public static String getContextRelativePath(String contextPath, String requestUri){
            return requestUri.substring(requestUri.indexOf(contextPath) + contextPath.length());
    }

    public static String sanitizePath(String resource){
        return resource.replaceAll("//+", "/");
    }

    public static String combinePaths(String context, String resource){
        if(context.endsWith("/") && resource.startsWith("/")){
            resource = resource.substring(1);
        }
        if(!context.endsWith("/") && !resource.startsWith("/")){
            resource = "/" + resource;
        }
        return context + resource;
    }

    public static String getContentTypeHeaderValue(String mimeType, String encoding){
        return String.format("%s; charset=%s", mimeType, encoding);
    }

    public static Map<String, String> parseQueryString(String queryString) {
        Map<String, String> map = new HashMap<>();
        if ((queryString == null) || (queryString.equals(""))) {
            return map;
        }
        String[] params = queryString.split("&");
        for (String param : params) {
            try {
                String[] keyValuePair = param.split("=", 2);
                String name = URLDecoder.decode(keyValuePair[0], "UTF-8");
                if (name == "") {
                    continue;
                }
                String value = keyValuePair.length > 1 ? URLDecoder.decode(
                        keyValuePair[1], "UTF-8") : "";
                map.put(name, value);
            } catch (UnsupportedEncodingException e) {
                // ignore this parameter if it can't be decoded
            }
        }
        return map;
    }


}
