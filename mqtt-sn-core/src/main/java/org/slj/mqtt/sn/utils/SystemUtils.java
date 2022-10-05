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

import java.lang.management.ManagementFactory;

public class SystemUtils {

    public static long getThreadCount(){
        try {
            //attempt to read the full count from MX (but this maybe disabled)
            //in which case fall back to the main thread group
            return ManagementFactory.getThreadMXBean().getThreadCount();
        } catch(Exception e){
            return Thread.activeCount();
        }
    }

    public static long getUsedMemory(){
        long used = getTotalMemory() - getFreeMemory();
        return used;
    }

    public static long getFreeMemory(){
        Runtime runtime = Runtime.getRuntime();
        return runtime.freeMemory();
    }

    public static long getMaxMemory(){
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory();
    }

    public static long getTotalMemory(){
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory();
    }

    public static String getMemoryString(long mem) {
        if(mem == 0) return "NaN";
        int theMem = (int) (mem / 1024L / 1024L);
        String str = Integer.toString(theMem);
        StringBuffer buff = new StringBuffer();
        while(str.length() > 3) {
            buff.insert(0, str.substring(str.length() - 3));
            buff.insert(0, ",");
            str = str.substring(0, str.length() - 3);
        }
        buff.insert(0, str);
        buff.append("mb");
        return buff.toString();

    }
}
