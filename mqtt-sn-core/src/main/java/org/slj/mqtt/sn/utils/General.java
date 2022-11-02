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

package org.slj.mqtt.sn.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class General {

    static final String OWASP_EMAIL_REGEX = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";

    public static boolean validCellphoneNumber(String cellphone){
        if(cellphone == null || cellphone.isEmpty() || cellphone.length() < 7) return false;
        try {
            Pattern pattern = Pattern.compile("^(\\d{3}[- .]?){2}\\d{4,7}$");
            Matcher matcher = pattern.matcher(cellphone);
            return matcher.matches();
        } catch(Exception e){
            return false;
        }
    }

    public static String extractNameFromEmailAddress(String emailAddress) {
        if(emailAddress == null || emailAddress.isEmpty() || emailAddress.length() < 3) return null;
        return emailAddress.replaceAll("((@.*)|[^a-zA-Z])+", " ").trim();
    }

    public static boolean validEmailAddress(String emailAddress){
        if(emailAddress == null || emailAddress.isEmpty() || emailAddress.length() < 3) return false;
        return Pattern.compile(OWASP_EMAIL_REGEX).matcher(emailAddress).matches();
    }

    public static Throwable getRootCause(Throwable t){
        if(t == null) return null;
        Throwable child = null;
        do {
            child = t;
            t = t.getCause();
        }
        while(t != null);
        return child;
    }

    public static String getRootCauseMessage(Throwable t){
        if(t == null) return null;
        String msg = null;
        do {
            if(t.getMessage() != null){
                msg = t.getMessage();
            }
            t = t.getCause();
        }
        while(t != null);
        return msg;
    }

    public static void reverseInline(byte[] a){
        int i;
        int len = a.length;
        for (i = 0; i < len / 2; i++) {
            byte o = a[i];
            a[i] = a[len - i - 1];
            a[len - i - 1] = o;
        }
    }
}
