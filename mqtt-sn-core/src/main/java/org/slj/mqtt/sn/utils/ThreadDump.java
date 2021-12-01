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

import java.util.Iterator;
import java.util.Map;

public class ThreadDump {

    static final String LINE = System.lineSeparator();

    private static final void dumpStacks(Thread thread, StackTraceElement[] elements, StringBuilder sb) {
        sb.append(thread.toString() + " " + thread.getState()).append(LINE);
        for (int i = 0; i < elements.length; i++) {
            sb.append("\tat " + elements[i]).append(LINE);
        }
        sb.append(LINE);
    }

    public static String create() {
        StringBuilder sb = new StringBuilder();
        Map<Thread,StackTraceElement[]> map = Thread.getAllStackTraces();
        Iterator<Thread> it = map.keySet().iterator();
        while(it.hasNext()) {
            Thread th = it.next();
            dumpStacks(th, map.get(th), sb);
        }
        return sb.toString();
    }
}
