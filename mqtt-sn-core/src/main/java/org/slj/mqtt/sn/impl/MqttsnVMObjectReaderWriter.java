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

package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.spi.IMqttsnObjectReaderWriter;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.io.*;

/**
 * This shouldn't really be used in prod, just threw it in to save a cascade of jackson deps
 * through the dep hierarchy. At least on a capable gateway, you should use JSON to store these
 * beans
 */
public class MqttsnVMObjectReaderWriter implements IMqttsnObjectReaderWriter {

    final static int BUFFER = 128;

    @Override
    public byte[] write(Serializable o) throws MqttsnException {
        try (ByteArrayOutputStream baos
                     = new ByteArrayOutputStream(BUFFER)) {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.flush();
            return baos.toByteArray();
        } catch(Exception e){
            throw new MqttsnException("error in marshalling object to stream", e);
        }
    }

    @Override
    public <T extends Serializable> T load(Class<? extends T> clz, byte[] arr) throws MqttsnException {
        try (ByteArrayInputStream in =
                     new ByteArrayInputStream(arr)) {
            ObjectInputStream is = new ObjectInputStream(in);
            return (T) is.readObject();
        } catch(Exception e){
            throw new MqttsnException("error in marshalling object to stream", e);
        }
    }
}






