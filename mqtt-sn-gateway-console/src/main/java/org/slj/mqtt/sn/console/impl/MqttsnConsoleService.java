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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slj.mqtt.sn.cloud.IMqttsnCloudService;
import org.slj.mqtt.sn.cloud.MqttsnCloudServiceException;
import org.slj.mqtt.sn.cloud.MqttsnCloudToken;
import org.slj.mqtt.sn.cloud.client.impl.HttpCloudServiceImpl;
import org.slj.mqtt.sn.console.IMqttsnConsole;
import org.slj.mqtt.sn.console.MqttsnConsoleOptions;
import org.slj.mqtt.sn.console.http.impl.handlers.AsyncContentHandler;
import org.slj.mqtt.sn.console.http.impl.handlers.HelloWorldHandler;
import org.slj.mqtt.sn.console.http.impl.handlers.RedirectHandler;
import org.slj.mqtt.sn.console.http.sun.SunHttpServerBootstrap;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.TrafficEntry;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.RollingList;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class MqttsnConsoleService extends AbstractMqttsnService
        implements IMqttsnConsole {

    private SunHttpServerBootstrap server;
    private MqttsnConsoleOptions options;
    private ObjectMapper jsonMapper;
    private IMqttsnCloudService cloudService;
    private List<TrafficEntry> traffic;

    public MqttsnConsoleService(MqttsnConsoleOptions options){
        this.options = options;
        options.processFromSystemPropertyOverrides();
    }

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);

        if(options.isConsoleEnabled()){
            jsonMapper = new ObjectMapper();
            jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            logger.info("starting console service with - {}", options);
            cloudService = new HttpCloudServiceImpl(jsonMapper,
                    "http://mqtt-sn.cloud/api/services.json", 5000, 5000, 30000);
            startWebServer(options);
            authorizeCloud();
            traffic = new RollingList<>(options.getMaxTrafficLogs());
            getRegistry().getRuntime().registerTrafficListener(new IMqttsnTrafficListener() {
                @Override
                public void trafficSent(INetworkContext context, byte[] data, IMqttsnMessage message) {
                    traffic.add(new TrafficEntry(context, message, data, TrafficEntry.DIRECTION.EGRESS));
                }

                @Override
                public void trafficReceived(INetworkContext context, byte[] data, IMqttsnMessage message) {
                    traffic.add(new TrafficEntry(context, message, data, TrafficEntry.DIRECTION.INGRESS));
                }
            });
        }
    }

    @Override
    public void stop() throws MqttsnException {
        super.stop();
        logger.info("stopping webserver...");
        stopWebServer();
    }

    private void startWebServer(MqttsnConsoleOptions options) throws MqttsnException {
        try {

            logger.info("starting console server listening on {} -> {}", options.getHostName(), options.getConsolePort());

            ExecutorService service = getRegistry().getRuntime().createManagedExecutorService("mqtt-sn-console-http-",
                    options.getServerThreads());

            server = new SunHttpServerBootstrap(
                    new InetSocketAddress(options.getHostName(), options.getConsolePort()),
                    options.getTcpBacklog(), service);
            server.registerContext("/", new RedirectHandler(getJsonMapper(), "./console/html/index.html"));
            server.registerContext("/hello", new HelloWorldHandler(getJsonMapper()));
            server.registerContext("/console", new MqttsnStaticWebsiteHandler(options, getJsonMapper(), getRegistry()));
            server.registerContext("/console/metrics/field", new ConsoleAsyncMetricFieldHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/chart", new ChartHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/session", new SessionHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/search", new SearchHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/config", new ConfigHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/transport", new TransportHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/topic", new TopicHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/dlq", new DLQHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/command", new CommandHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/client/access", new ClientAccessHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/connectors", new ConnectorHandler(cloudService, getJsonMapper(), getRegistry()));
            server.registerContext("/console/bridges", new BridgeHandler(cloudService, getJsonMapper(), getRegistry()));
            server.registerContext("/console/connector/status", new ConnectorStatusHandler(cloudService, getJsonMapper(), getRegistry()));
            server.registerContext("/console/cloud/status", new CloudStatusHandler(cloudService, getJsonMapper(), getRegistry()));
            server.registerContext("/console/cloud", new CloudHandler(cloudService, getJsonMapper(), getRegistry()));
            server.registerContext("/console/logs", new LogHandler(getJsonMapper(), getRegistry()));
            server.registerContext("/console/async", new AsyncContentHandler(getJsonMapper(), "httpd/html/",
                    "dashboard.html", "clients.html",  "session.html", "connectors.html", "bridges.html", "config.html", "transport.html", "topics.html", "settings.html", "licenses.html", "system.html", "dead-letter.html", "logs.html", "cloud.html"));
            server.startServer();
            logger.trace("console server started...");
        } catch(Exception e){
            throw new MqttsnException(e);
        }
    }

    private void stopWebServer() {
        if(server != null){
            server.stopServer();
        }
    }

    public void authorizeCloud()  {
        try {
            if(!cloudService.isAuthorized()){
                String cloudToken = getRegistry().getStorageService().getStringPreference(IMqttsnCloudService.CLOUD_TOKEN, null);
                if(cloudToken != null){
                    Date expires = getRegistry().getStorageService().getDatePreference(IMqttsnCloudService.CLOUD_TOKEN_EXPIRES, null);
                    MqttsnCloudToken token = new MqttsnCloudToken();
                    token.setToken(cloudToken);
                    token.setExpires(expires);
                    cloudService.setToken(token);
                }
            }
        } catch(MqttsnCloudServiceException e){
            logger.error("error authorizing cloud account;", e);
        }
    }

    public ObjectMapper getJsonMapper() {
        return jsonMapper;
    }

    public List<TrafficEntry> getTraffic(){
        synchronized (traffic){
            return Collections.unmodifiableList(traffic);
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
