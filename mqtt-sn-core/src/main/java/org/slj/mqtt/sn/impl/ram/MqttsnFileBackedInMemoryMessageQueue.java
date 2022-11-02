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

package org.slj.mqtt.sn.impl.ram;

import org.slj.mqtt.sn.model.IMqttsnDataRef;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.model.session.IMqttsnQueuedPublishMessage;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.impl.MqttsnQueuedPublishMessageImpl;
import org.slj.mqtt.sn.spi.IMqttsnObjectReaderWriter;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;
import org.slj.mqtt.sn.utils.Files;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.TransientObjectLocks;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class MqttsnFileBackedInMemoryMessageQueue
        extends MqttsnInMemoryMessageQueue {

    static final String DIR = "_message-queues-overflow";
    private final TransientObjectLocks locks = new TransientObjectLocks();
    private final IMqttsnObjectReaderWriter readWriter;
    private Set<IMqttsnDataRef> refs;
    private Map<IMqttsnSession, AtomicInteger> countMap;
    private volatile File root = null;

    public MqttsnFileBackedInMemoryMessageQueue(IMqttsnObjectReaderWriter readWriter) {
        this.readWriter = readWriter;
    }

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        initialize();
    }

    protected void initialize(){
        if(root == null){
            synchronized (this){
                if(root == null){
                    File f = getRegistry().getStorageService().getWorkspaceRoot();
                    f = new File(f, DIR);
                    f.mkdir();
                    root = f;
                }
            }
        }
        refs = new HashSet();
        countMap = new ConcurrentHashMap<>();
    }

    @Override
    protected void offerInternal(IMqttsnSession session, IMqttsnQueuedPublishMessage message)
            throws MqttsnException, MqttsnQueueAcceptException {
        try {
            synchronized (locks.mutex(session.getContext().getId())) {

                int count = super.getSessionBean(session).getQueueSize();
                int max = getRegistry().getOptions().getMaxMessagesInQueue();
                int threshold = getRegistry().getOptions().getMessageQueueDiskStorageThreshold();
                if (MqttsnUtils.percentOf(count, max) > threshold) {
                    File f = getFileForSession(session, true);
                    logger.log(Level.FINE, "message queue threshold exceeded ({0}), overflow to disk overflow {1}",
                            new Object[]{count, session.getContext()});

                    byte[] a = readWriter.write(message);
                    byte[] c = new byte[a.length + 1];
                    System.arraycopy(a, 0, c, 0, a.length);
                    c[c.length - 1] = Files.NEW_LINE_DECIMAL;
                    Files.append(f, c);
                    incrementFileObjectCount(session, 1);
//                    Files.appendWithLock(f, readWriter.write(message), true);
                    //-- we use weak references (this is still meant to be volatile storage)
                    //-- so we need to keep hold of the datarefs so theyre not collected
                    refs.add(message.getDataRefId());
                } else {
                    super.offerInternal(session, message);
                }

            }
        } catch(IOException e){
            throw new MqttsnException("error accessing queue overflow file;", e);
        }
    }

    @Override
    public IMqttsnQueuedPublishMessage poll(IMqttsnSession session){
        synchronized (locks.mutex(session.getContext().getId())){
            return super.poll(session);
        }
    }

    @Override
    public IMqttsnQueuedPublishMessage peek(IMqttsnSession session){

        try {
            synchronized (locks.mutex(session.getContext().getId())){
                if(super.queueSize(session) == 0 &&
                        hasOverflow(session)){
                    //-- move some messages from disk into memory space
                    byte[] data = Files.consumeLinesFromStart(
                            getFileForSession(session, false), 10);

                    logger.log(Level.INFO, "consuming messages from disk overflow {0} -> ({1} bytes)",
                            new Object[] { session.getContext().getId(), data.length });

                    int idx = 0;
                    ByteArrayOutputStream baos
                            = new ByteArrayOutputStream();
                    do {
                        byte b = data[idx++];
                        if(b == Files.NEW_LINE_DECIMAL
                                || idx == data.length){
                            if(b != Files.NEW_LINE_DECIMAL){
                                baos.write(b);
                            }
                            super.offerInternal(session,
                                    readWriter.load(MqttsnQueuedPublishMessageImpl.class,
                                    baos.toByteArray()));
                            incrementFileObjectCount(session, -1);
                            baos = new ByteArrayOutputStream();
                        } else {
                            baos.write(b);
                        }
                    } while(idx < data.length);
                }
            }
            return super.peek(session);
        } catch(Exception e){
            throw new MqttsnRuntimeException(e);
        }
    }

    @Override
    public long queueSize(IMqttsnSession session) throws MqttsnException {
        try {
            long combinedQueue = super.queueSize(session);
            if(hasOverflow(session)){
//                File f = getFileForSession(session, true);
//                combinedQueue += Files.countLines(f);
                combinedQueue += getFileObjectCount(session);
            }
            return combinedQueue;
        } catch(Exception e){
            throw new MqttsnException(e);
        }
    }

    private boolean hasOverflow(IMqttsnSession session) throws IOException {
        File f = getFileForSession(session, false);
        synchronized (locks.mutex(session.getContext().getId())){
            return f.exists();
        }
    }

    private File getFileForSession(IMqttsnSession session, boolean createIfNotExists) throws IOException {
        File f = new File(root, fileNameSafe(session.getContext().getId()));
        if(createIfNotExists && !f.exists()){
            f.createNewFile();
            countMap.put(session, new AtomicInteger());
            f.deleteOnExit();
            logger.log(Level.INFO, "creating disk overflow for {0}",
                    new Object[] { session.getContext().getId()});
        }
        return f;
    }

    private void incrementFileObjectCount(IMqttsnSession session, int value){
        AtomicInteger c = countMap.get(session);
        if(c != null) {
            c.addAndGet(value);
        }
    }

    private int getFileObjectCount(IMqttsnSession session){
        AtomicInteger c = countMap.get(session);
        if(c != null) {
            return c.incrementAndGet();
        }
        return 0;
    }

    private static String fileNameSafe(String clientId){
        return MqttsnWireUtils.toHex(clientId.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void clearAll() {
        super.clearAll();
        try {
            clearFilesystemOnly();
        } catch(MqttsnException e){
            throw new MqttsnRuntimeException(e);
        }
    }

    public void clearFilesystemOnly() throws MqttsnException {
        try {
            Files.delete(root);
            countMap.clear();
            refs.clear();
        } catch(IOException e){
            throw new MqttsnException(e);
        } finally {
            root = null;
            countMap = null;
            initialize();
        }
    }
}
