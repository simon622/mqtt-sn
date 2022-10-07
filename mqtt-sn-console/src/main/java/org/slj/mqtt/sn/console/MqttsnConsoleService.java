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

package org.slj.mqtt.sn.console;

import org.slj.mqtt.sn.http.impl.handlers.*;
import org.slj.mqtt.sn.http.sun.SunHttpServerBoostrap;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnService;

import java.net.InetSocketAddress;
import java.util.logging.Level;

public class MqttsnConsoleService extends MqttsnService implements IMqttsnConsole{

    private SunHttpServerBoostrap server;

    private MqttsnConsoleOptions options;

    public MqttsnConsoleService(MqttsnConsoleOptions options){
        this.options = options;
    }

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        if(options.isConsoleEnabled()){
            logger.log(Level.INFO, String.format("starting console service with - %s", options));
            startWebServer(options);
        }
    }

    @Override
    public void stop() throws MqttsnException {
        super.stop();
        stopWebServer();
    }

    private void startWebServer(MqttsnConsoleOptions options) throws MqttsnException {
        try {

            logger.log(Level.INFO, String.format("starting console server listening on [%s] -> [%s]", options.getHostName(), options.getConsolePort()));
            server = new SunHttpServerBoostrap(
                    new InetSocketAddress(options.getHostName(), options.getConsolePort()),
                    options.getServerThreads(), options.getTcpBacklog());
            server.registerContext("/", new RedirectHandler("./console/html/index.html"));
            server.registerContext("/hello", new HelloWorldHandler());
            server.registerContext("/console", new StaticFileHandler("httpd"));
            server.registerContext("/console/api", new AsyncFieldHandler());
            server.registerContext("/console/chart", new MqttsnMetricChartHandler(getRegistry()));
            server.registerContext("/console/async", new AsyncContentHandler("httpd/html/",
                    "dashboard.html", "clients.html", "backend.html", "config.html", "cluster.html", "topics.html", "settings.html", "docs.html", "backend.html", "system.html"));
            server.startServer();
            logger.log(Level.INFO, String.format("console server started..."));
        } catch(Exception e){
            throw new MqttsnException(e);
        }
    }

    private void stopWebServer() {
        if(server != null){
            server.stopServer();
        }
    }

    public static void main(String[] args) {
        try {
            MqttsnConsoleOptions options = new MqttsnConsoleOptions();
            MqttsnConsoleService console = new MqttsnConsoleService(options);
            console.startWebServer(options);
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
