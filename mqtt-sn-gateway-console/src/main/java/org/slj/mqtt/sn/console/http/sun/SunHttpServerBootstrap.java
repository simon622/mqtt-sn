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

package org.slj.mqtt.sn.console.http.sun;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.console.http.IHttpRequestResponseHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A realisation of the HTTP handler layer using the SUN HTTP packages which are available
 * on the Oracle VM > 1.6.
 */
public class SunHttpServerBootstrap {
    private static final Logger logger =
            LoggerFactory.getLogger(SunHttpServerBootstrap.class);
    private InetSocketAddress bindAddress;
    private HttpServer server;
    private ExecutorService threadPoolExecutor;
    private volatile boolean running = false;

    public SunHttpServerBootstrap(InetSocketAddress bindAddress, int tcpBacklog, ExecutorService httpExecutor) throws IOException {
        this.bindAddress = bindAddress;
        this.threadPoolExecutor = httpExecutor;
        init(tcpBacklog);
    }

    public synchronized void init(int tcpBacklog) throws IOException {
        if(!running && server == null){
            logger.info("bootstrapping sun-http-server to [{}], tcpBacklog={}}", bindAddress, tcpBacklog);
            server = HttpServer.create(bindAddress, tcpBacklog);
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
                running = false;
                if(server != null){
                    server.stop(1);
                    logger.info("stopped server... closing down pools...");
                }
            }
            finally {
                server = null;
                try {
                    if(threadPoolExecutor != null){
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
                    }
                } finally {
                    threadPoolExecutor = null;
                }
            }
        }
    }
}
