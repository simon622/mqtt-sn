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

package org.slj.mqtt.sn.console.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slj.mqtt.sn.cloud.IMqttsnCloudService;
import org.slj.mqtt.sn.cloud.client.impl.HttpCloudServiceImpl;
import org.slj.mqtt.sn.console.IMqttsnConsole;
import org.slj.mqtt.sn.console.MqttsnConsoleOptions;
import org.slj.mqtt.sn.console.http.impl.handlers.AsyncContentHandler;
import org.slj.mqtt.sn.console.http.impl.handlers.HelloWorldHandler;
import org.slj.mqtt.sn.console.http.impl.handlers.RedirectHandler;
import org.slj.mqtt.sn.console.http.sun.SunHttpServerBootstrap;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnService;

import java.net.InetSocketAddress;
import java.util.logging.Level;

public class MqttsnConsoleService extends MqttsnService implements IMqttsnConsole {

    private SunHttpServerBootstrap server;
    private MqttsnConsoleOptions options;
    private ObjectMapper jsonMapper;
    private IMqttsnCloudService cloudService;

    public MqttsnConsoleService(MqttsnConsoleOptions options){
        this.options = options;
    }

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);

        if(options.isConsoleEnabled()){
            jsonMapper = new ObjectMapper();
            logger.log(Level.INFO, String.format("starting console service with - %s", options));
            cloudService = new HttpCloudServiceImpl(jsonMapper,
                    "http://mqtt-sn.cloud/api/services.json", 5000, 5000);
            startWebServer(options);
        }
    }

    @Override
    public void stop() throws MqttsnException {
        super.stop();
        logger.log(Level.INFO, String.format("stopping webserver..."));
        stopWebServer();
    }

    private void startWebServer(MqttsnConsoleOptions options) throws MqttsnException {
        try {

            logger.log(Level.INFO, String.format("starting console server listening on [%s] -> [%s]", options.getHostName(), options.getConsolePort()));

            server = new SunHttpServerBootstrap(
                    new InetSocketAddress(options.getHostName(), options.getConsolePort()),
                    options.getServerThreads(), options.getTcpBacklog());
            server.registerContext("/", new RedirectHandler(getJsonMapper(), "./console/html/index.html"));
            server.registerContext("/hello", new HelloWorldHandler(getJsonMapper()));
            server.registerContext("/console", new MqttsnStaticWebsiteHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/metrics/field", new ConsoleAsyncMetricFieldHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/chart", new ChartHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/session", new SessionHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/search", new SearchHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/config", new ConfigHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/topic", new TopicHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/dlq", new DLQHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/client/access", new ClientAccessHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/connectors", new ConnectorHandler(cloudService, getJsonMapper(), getRegistry()));
            server.registerContext("/console/connector/status", new ConnectorStatusHandler(cloudService, getJsonMapper(), getRegistry()));
            server.registerContext("/console/cloud/status", new CloudStatusHandler(cloudService, getJsonMapper(), getRegistry()));
            server.registerContext("/console/async", new AsyncContentHandler(getJsonMapper(), "httpd/html/",
                    "dashboard.html", "clients.html",  "session.html", "connectors.html", "config.html", "cluster.html", "topics.html", "settings.html", "docs.html", "system.html", "dead-letter.html"));
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

    public ObjectMapper getJsonMapper() {
        return jsonMapper;
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
