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

import com.sun.net.httpserver.HttpServer;
import org.slj.mqtt.sn.http.IHttpRequestResponseHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A realisation of the HTTP handler layer using the SUN HTTP packages which are available
 * on the Oracle VM > 1.6.
 */
public class SunHttpServerBoostrap {
    private static final Logger LOG =
            Logger.getLogger(SunHttpServerBoostrap.class.getName());
    private InetSocketAddress bindAddress;
    private HttpServer server;
    private ThreadPoolExecutor threadPoolExecutor;
    private volatile boolean running = false;

    public SunHttpServerBoostrap(InetSocketAddress bindAddress) throws IOException {
        this.bindAddress = bindAddress;
        init();
    }

    public synchronized void init() throws IOException {
        if(!running && server == null){
            LOG.log(Level.INFO, String.format("bootstrapping sun-http-server to [%s]", bindAddress));
            server = HttpServer.create(bindAddress, 0);
            threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
            server.setExecutor(threadPoolExecutor);
        }
    }

    public void registerContext(String contextPath, IHttpRequestResponseHandler handler){
        if(running) throw new RuntimeException("unable to add context to running server");
        if(handler != null && contextPath != null){
            server.createContext(contextPath, new SunHttpHandlerProxy(handler));
        }
    }

    public void startServer(){
        if(server != null && !running){
            server.start();
            running = true;
        }
    }

    public void stopServer(){
        if(running){
            try {
                if(server != null){
                    server.stop(0);
                }
            }
            finally {
                try {
                    if(!threadPoolExecutor.isShutdown()){
                        threadPoolExecutor.shutdown();
                        try {
                            threadPoolExecutor.awaitTermination(10000, TimeUnit.SECONDS);
                        } catch(InterruptedException e){
                            Thread.currentThread().interrupt();
                        } finally {
                            if(!threadPoolExecutor.isShutdown()){
                                threadPoolExecutor.shutdownNow();
                            }
                        }
                    }
                } finally {
                    running = false;
                    server = null;
                    threadPoolExecutor = null;
                }
            }
        }
    }
}