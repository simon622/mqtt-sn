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

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

public class Files {

    public static String getFileName(String filePath){
        int idx = filePath.lastIndexOf(File.separator);
        String name = null;
        if(idx > -1){
            name = filePath.substring(idx);
        }
        return name;
    }

    public static String getFileExtension(String filePath){
        int idx = filePath.lastIndexOf(".");
        String ext = null;
        if(idx > -1){
            ext = filePath.substring(idx + 1);
        }
        return ext;
    }

    public static void copy(InputStream is, OutputStream os, int bufSize) throws IOException {
        byte[] buf = new byte[bufSize];
        int length;
        while ((length = is.read(buf)) != -1) {
            os.write(buf, 0, length);
        }
    }

    public static byte[] read(InputStream is, int bufSize) throws IOException {
        try(ByteArrayOutputStream baos
                    = new ByteArrayOutputStream()){
            byte[] buf = new byte[bufSize];
            int length;
            while ((length = is.read(buf)) != -1) {
                baos.write(buf, 0, length);
            }
            return baos.toByteArray();
        }
    }

    public static void writeWithLock(String fileName, byte[] bytes)
            throws IOException, OverlappingFileLockException {

        RandomAccessFile stream = new RandomAccessFile(fileName, "rw");
        FileChannel channel = stream.getChannel();
        FileLock lock = null;
        try {
            lock = channel.tryLock();
            stream.write(bytes);
        }
        finally {
            try {
                if(lock != null) lock.release();
            } finally {
                if(stream != null) stream.close();
                if(channel != null) channel.close();
            }
        }
    }
}
