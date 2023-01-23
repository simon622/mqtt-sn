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

import org.slj.mqtt.sn.cloud.MqttsnCloudAccount;
import org.slj.mqtt.sn.model.AbstractOptions;

public class MqttsnConsoleOptions extends AbstractOptions {

    /**
     * Should the console be enabled by default
     */
    public static final boolean DEFAULT_CONSOLE_ENABLED = true;
    public static final int DEFAULT_CONSOLE_PORT = 8080;
    public static final String DEFAULT_CONSOLE_HOST_NAME = "0.0.0.0";
    public static final String DEFAULT_CONSOLE_USERNAME = "admin";
    public static final String DEFAULT_CONSOLE_PASSWORD = "password";
    public static final int DEFAULT_TCP_BACKLOG = 100;
    public static final int DEFAULT_SERVER_THREADS = 2;
    public static final int DEFAULT_MAX_TRAFFIC_LOGS = 250;
    public boolean consoleEnabled = DEFAULT_CONSOLE_ENABLED;
    public int consolePort = DEFAULT_CONSOLE_PORT;
    public String hostName = DEFAULT_CONSOLE_HOST_NAME;
    public int tcpBacklog = DEFAULT_TCP_BACKLOG;
    public int serverThreads = DEFAULT_SERVER_THREADS;
    public int maxTrafficLogs = DEFAULT_MAX_TRAFFIC_LOGS;

    public String userName = DEFAULT_CONSOLE_USERNAME;
    public String password = DEFAULT_CONSOLE_PASSWORD;

    public MqttsnCloudAccount cloudAccount;

    public MqttsnConsoleOptions withCloudAccount(MqttsnCloudAccount cloudAccount){
        this.cloudAccount = cloudAccount;
        return this;
    }

    public MqttsnConsoleOptions withConsolePort(int consolePort){
        this.consolePort = consolePort;
        return this;
    }

    public MqttsnConsoleOptions withConsoleEnabled(boolean consoleEnabled){
        this.consoleEnabled = consoleEnabled;
        return this;
    }

    public MqttsnConsoleOptions withServerThreads(int serverThreads){
        this.serverThreads = serverThreads;
        return this;
    }

    public MqttsnConsoleOptions withTCPBacklog(int tcpBacklog){
        this.tcpBacklog = tcpBacklog;
        return this;
    }

    public MqttsnConsoleOptions withMaxTrafficLogs(int maxTrafficLogs){
        this.maxTrafficLogs = maxTrafficLogs;
        return this;
    }

    public MqttsnConsoleOptions withHostname(String hostName){
        this.hostName = hostName;
        return this;
    }

    public MqttsnConsoleOptions withBasicAuthUsername(String userName){
        this.userName = userName;
        return this;
    }

    public MqttsnConsoleOptions withBasicAuthPassword(String password){
        this.password = password;
        return this;
    }

    public boolean isConsoleEnabled() {
        return consoleEnabled;
    }

    public int getConsolePort() {
        return consolePort;
    }

    public String getHostName() {
        return hostName;
    }

    public int getTcpBacklog() {
        return tcpBacklog;
    }

    public int getServerThreads() {
        return serverThreads;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public int getMaxTrafficLogs() {
        return maxTrafficLogs;
    }

    public MqttsnCloudAccount getCloudAccount() {
        return cloudAccount;
    }

    @Override
    public String toString() {
        return "MqttsnConsoleOptions{" +
                "consoleEnabled=" + consoleEnabled +
                ", consolePort=" + consolePort +
                ", hostName='" + hostName + '\'' +
                ", tcpBacklog=" + tcpBacklog +
                ", serverThreads=" + serverThreads +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
