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

package org.slj.mqtt.sn.cli;

import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntime;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.model.MqttsnSecurityOptions;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.ThreadDump;
import org.slj.mqtt.sn.utils.TopicPath;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.io.*;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public abstract class AbstractInteractiveCli {

    static final int MAX_ALLOWED_CLIENTID_LENGTH = 255;
    static final String HOSTNAME = "hostName";
    static final String CLIENTID = "clientId";
    static final String PORT = "port";
    static final String PROTOCOL_VERSION = "protocolVersion";


    protected final AtomicInteger sentByteCount = new AtomicInteger(0);
    protected final AtomicInteger receiveByteCount = new AtomicInteger(0);
    protected final AtomicInteger receiveCount = new AtomicInteger(0);
    protected final AtomicInteger receivedPublishBytesCount = new AtomicInteger(0);
    protected final AtomicInteger sentCount = new AtomicInteger(0);
    protected final AtomicInteger publishedBytesCount = new AtomicInteger(0);

    protected boolean colors = false;
    protected boolean useHistory = false;
    protected String hostName;
    protected int port;
    protected String clientId;
    protected int protocolVersion = 1;

    protected MqttsnOptions options;
    protected AbstractMqttsnRuntimeRegistry runtimeRegistry;
    protected AbstractMqttsnRuntime runtime;

    protected PrintStream output;
    protected Scanner input;
    protected boolean enableOutput = true;
    protected volatile boolean closed = false;
    private volatile boolean lastCursorAsync = false;

    public void init(Scanner input, PrintStream output){
        this.input = input;
        this.output = output;
        colors = !System.getProperty("os.name").contains("Windows");
    }

    public void start() throws Exception {
        if(input == null || output == null) throw new IllegalStateException("no init");
        message(String.format("Starting up interactive CLI with clientId=%s, protocolVersion=%s", clientId, protocolVersion));
        if(useHistory){
            saveConfig();
        }

        message(String.format("Creating runtime configuration.. DONE"));
        options = createOptions();
        message(String.format("Creating runtime registry.. DONE"));
        runtimeRegistry = createRuntimeRegistry(options, createTransport());
        IMqttsnCodec codec = createCodec();
        runtimeRegistry.withCodec(codec);
        message(String.format("Creating runtime with codec version.. %s", codec.getProtocolVersion()));
        if(options.getSecurityOptions() != null){
            message(String.format("Creating security configuration.. DONE"));
            message(String.format("Integrity type: %s", options.getSecurityOptions().getIntegrityType()));
            if(options.getSecurityOptions().getIntegrityType() != MqttsnSecurityOptions.INTEGRITY_TYPE.none){
                message(String.format("Integrity algorithm: %s", options.getSecurityOptions().getIntegrityType() ==
                        MqttsnSecurityOptions.INTEGRITY_TYPE.hmac ? options.getSecurityOptions().getIntegrityHmacAlgorithm() :
                        options.getSecurityOptions().getIntegrityChecksumAlgorithm()));
                message(String.format("Integrity point: %s", options.getSecurityOptions().getIntegrityPoint()));
                message(String.format("Integrity PSK: %s", new String(MqttsnUtils.arrayOf(options.getSecurityOptions().getIntegrityKey().length(), (byte) '*'))));
            }
        }
        message(String.format("Creating runtime .. DONE"));
        runtime = createRuntime(runtimeRegistry, options);

        runtime.registerPublishReceivedListener((IMqttsnContext context, TopicPath topic, int qos, boolean retained, byte[] data, IMqttsnMessage message) -> {
            try {
                receiveCount.incrementAndGet();
                receivedPublishBytesCount.addAndGet(data.length);
                if(enableOutput) asyncmessage(String.format("[>>>] Publish received [%s] bytes on [%s] from [%s] \"%s\"",
                        data.length, topic, context.getId(), new String(data)));
            } catch(Exception e){
                e.printStackTrace();
            }
        });
        runtime.registerPublishSentListener((IMqttsnContext context, UUID messageId, TopicPath topic, int qos, boolean retained, byte[] data, IMqttsnMessage message) -> {
            try {
                sentCount.incrementAndGet();
                publishedBytesCount.addAndGet(data.length);
                if(enableOutput) asyncmessage(String.format("[<<<] Publish sent [%s] bytes on [%s] to [%s] \"%s\"",
                        data.length, topic, context.getId(), new String(data)));
            } catch(Exception e){
                e.printStackTrace();
            }
        });
        enableOutput();
        message("Adding runtime listeners.. DONE");

        runtime.registerPublishFailedListener((IMqttsnContext context, UUID messageId, TopicPath topic, int qos, boolean retained, byte[] data, IMqttsnMessage message, int retryCount) -> {
            try {
                sentCount.incrementAndGet();
                int QoS = runtimeRegistry.getCodec().getQoS(message, true);
                asyncmessage(String.format("[xxx] Publish failure (tried [%s] times) [%s] bytes on topic [%s], at QoS [%s] \"%s\"",
                        retryCount, data.length, topic, QoS, new String(data)));
            } catch(Exception e){
                e.printStackTrace();
            }
        });

        message("Adding traffic listeners.. DONE");
        runtimeRegistry.withTrafficListener(new IMqttsnTrafficListener() {
            @Override
            public void trafficSent(INetworkContext context, byte[] data, IMqttsnMessage message) {
                sentByteCount.addAndGet(data.length);
            }

            @Override
            public void trafficReceived(INetworkContext context, byte[] data, IMqttsnMessage message) {
                receiveByteCount.addAndGet(data.length);
            }
        });

        message("Runtime successfully initialised.");
    }

    public void enableOutput(){
        enableOutput = true;
        message("console messaging output enabled");
    }

    public void disableOutput(){
        enableOutput = false;
        message("console messaging output disabled");
    }

    public void welcome(String welcome){
        synchronized (output){
            output.println("===============================================");
            output.println(String.format("Welcome to %s", getCLIName()));
            output.println("===============================================");

            if(welcome != null){
                output.println(String.format("%s", welcome));
            }
        }
    }

    public void exit(){
        synchronized (output) {
            output.println("===============================================");
            output.println(String.format("Goodbye from %s", getCLIName()));
            output.println("===============================================");
        }
    }

    protected void configure() throws IOException {
        if(needsHostname()){
            do{
                hostName = captureMandatoryString(input, output, "Please enter a valid host name or ip address");
            } while(!validHost());
        }

        if(needsPort()){
            port = captureMandatoryInt(input, output, "Please enter a port", null);
        }

        if(needsClientId()){
            do{
                clientId = captureString(input, output,  "Please enter a valid client or gateway Id");
            } while(!validClientId(MAX_ALLOWED_CLIENTID_LENGTH));
        }

        do{
            protocolVersion = captureMandatoryInt(input, output,  "Please enter a protocol version (1 for 1.2 or 2 for 2.0)", new int[] {1, 2});
        } while(!validProtocolVersion());
    }

    public void configureWithHistory() throws IOException {
        boolean useConfig = false;
        useHistory = true;
        if(loadConfig()){
            if(configOk()){
                useConfig = captureMandatoryBoolean(input, output,
                        String.format("I managed to load port=%s, clientId=%s from history, would you like to use it?",
                                port, clientId));
            }
        }

        if(!useConfig){
            configure();
        }
    }

    protected boolean configOk(){
        if(needsHostname()){
            if(hostName == null) return false;
        }
        if(needsPort()){
            if(port == 0) return false;
        }
        return validProtocolVersion();
    }

    protected void loadConfigHistory(Properties props) throws IOException {
        hostName = props.getProperty(HOSTNAME);
        try {
            port = Integer.valueOf(props.getProperty(PORT));
        } catch(Exception e){
        }
        try {
            protocolVersion = Integer.valueOf(props.getProperty(PROTOCOL_VERSION));
        } catch(Exception e){
        }
        clientId = props.getProperty(CLIENTID);
    }

    protected void saveConfigHistory(Properties props) {
        if(needsHostname() && hostName != null) props.setProperty(HOSTNAME, hostName);
        if(needsPort()) props.setProperty(PORT, String.valueOf(port));
        if(needsClientId() && clientId != null){
            props.setProperty(CLIENTID, clientId);
        }
        props.setProperty(PROTOCOL_VERSION, String.valueOf(protocolVersion));
    }

    protected boolean loadConfig() throws IOException {
        Properties properties = new Properties();
        File f = new File("./" + getPropertyFileName());
        if(f.exists()){
            properties.load(new FileInputStream(f));
            loadConfigHistory(properties);
            return true;
        }
        return false;
    }

    protected void saveConfig() throws IOException {
        Properties properties = new Properties();
        File f = new File("./" + getPropertyFileName());
        if(!f.exists()){
            f.createNewFile();
        }
        saveConfigHistory(properties);
        properties.store(new FileOutputStream(f), "Generated at " + new Date());
        message(String.format("History file written to %s", f.getAbsolutePath()));
    }


    public void asyncmessage(String message) {
        StringBuilder sb = new StringBuilder("\t** ");
        sb.append(message);
        synchronized (output){
            if(!lastCursorAsync){
                output.println();
                lastCursorAsync = true;
            }
            output.println(cli_purple(sb.toString()));
        }
    }

    public void message(String message) {
        StringBuilder sb = new StringBuilder("\t>> ");
        sb.append(message);
        synchronized (output){
            output.println(cli_reset(sb.toString()));
            lastCursorAsync = false;
        }
    }

    public void tabmessage(String message) {
        StringBuilder sb = new StringBuilder("\t\t>> ");
        sb.append(message);
        synchronized (output){
            output.println(cli_reset(sb.toString()));
            lastCursorAsync = false;
        }
    }

    public void error(String message, Throwable t) throws IOException {
        StringBuilder sb = new StringBuilder("\t>> ERROR ");
        sb.append(message);
        synchronized (output){
            output.println(cli_red(sb.toString()));
            if(t != null){
                output.println(cli_red(String.format("\t>> REASON %s", t.getMessage())));
                t.printStackTrace();
            }
            lastCursorAsync = false;
        }
    }

    protected String cli_reset(String value){
        StringBuilder sb = new StringBuilder();
        sb.append(colors ? "\u001B[0m" : "");
        sb.append(value);
        return sb.toString();
    }

    protected String cli_red(String value){
        StringBuilder sb = new StringBuilder();
        sb.append(colors ? "\u001B[31m" : "");
        sb.append(value);
        return sb.toString();
    }

    protected String cli_green(String value){
        StringBuilder sb = new StringBuilder();
        sb.append(colors ? "\u001B[32m" : "");
        sb.append(value);
        return sb.toString();
    }

    protected String cli_blue(String value){
        StringBuilder sb = new StringBuilder();
        sb.append(colors ? "\u001B[34m" : "");
        sb.append(value);
        return sb.toString();
    }

    protected String cli_purple(String value){
        StringBuilder sb = new StringBuilder();
        sb.append(colors ? "\u001B[55m" : "");
        sb.append(value);
        return sb.toString();
    }

    public void command() throws Exception {
        String command = null;
        do {
            command = captureMandatoryString(input, output, "Please enter a command (or type HELP)");
            try {
                if(!processCommand(command))
                    closed = true;
            } catch(IllegalArgumentException e){
                error("command not found - (type HELP for command list)", null);
            }
        } while(!closed);
    }

    protected boolean validHost(){
        if(hostName == null) return false;
        if(hostName.trim().length() == 0) return false;
        Pattern p = Pattern.compile("[^a-zA-Z0-9.-]");
        return !p.matcher(hostName).find();
    }

    protected boolean validProtocolVersion(){
        if(protocolVersion == 1 || protocolVersion == 2) return true;
        return false;
    }

    protected boolean validClientId(int maxLength){
        if(clientId == null) return true;
        if(clientId.trim().length() > maxLength) return false;
        Pattern p = Pattern.compile("[a-zA-Z0-9\\-]{1,65528}");
        return p.matcher(clientId).find();
    }

    protected String getCLIName(){
        return "org.slj interactive command line";
    }

    protected void resetMetrics() throws IOException {
        receivedPublishBytesCount.set(0);
        sentCount.set(0);
        receiveCount.set(0);
        sentByteCount.set(0);
        receiveByteCount.set(0);
        publishedBytesCount.set(0);
        message("Metrics & queue reset");
    }

    protected void predefine(String topicName, int alias) {
        if(runtime != null && runtimeRegistry != null){
            getOptions().getPredefinedTopics().put(topicName, alias);
            message("DONE - predefined topic registered successfully");
        } else {
            message("Cannot add a topic to an uninitialised runtime");
        }
    }

    protected void stats() {
        message(String.format("Publish Sent Count: %s messages(s) - (%s bytes) ", sentCount.get(), publishedBytesCount.get()));
        message(String.format("Publish Receive Count: %s messages(s) - (%s bytes)", receiveCount.get(), receivedPublishBytesCount.get()));
        message(String.format("Network Bytes Sent: %s byte(s)", sentByteCount.get()));
        message(String.format("Network Bytes Received: %s byte(s)", receiveByteCount.get()));
    }

    protected void threadDump() {
        output.println(ThreadDump.create());
    }

    protected String captureString(Scanner input, PrintStream output, String question){
        String value = null;
        while(value == null){
            output.print(cli_reset(String.format("%s : ", question)));
            value = input.nextLine();
            value = value.trim();
        }
        return value.length() == 0 ? null : value;
    }

    protected String captureMandatoryString(Scanner input, PrintStream output, String question){
        String value = null;
        while(value == null){
            output.print(cli_reset(String.format("%s : ", question)));
            value = input.nextLine();
            value = value.trim();
            if(value.length() == 0) {
                value = null;
            }
        }
        return value;
    }

    protected String captureFilePath(Scanner input, PrintStream output, String question){
        String value = null;
        while(value == null){
            output.print(cli_reset(String.format("%s : ", question)));
            value = input.nextLine();
            value = value.trim();
            if(value.length() > 0) {
                File f = new File(value);
                if(!f.exists() || !f.isFile()){
                    value = null;
                }
            }
        }
        return value.length() == 0 ? null : value;
    }

    protected String captureMandatoryFilePath(Scanner input, PrintStream output, String question){
        String value = null;
        while(value == null){
            output.print(cli_reset(String.format("%s : ", question)));
            value = input.nextLine();
            value = value.trim();
            if(value.length() == 0) {
                value = null;
            }
            File f = new File(value);
            if(!f.exists() || !f.isFile()){
                value = null;
            }
        }
        return value;
    }

    protected int captureMandatoryInt(Scanner input, PrintStream output, String question, int[] allowedValues){
        String value = null;
        while(value == null){
            output.print(cli_reset(String.format("%s : ", question)));
            value = input.nextLine();
            value = value.trim();
            try {
                int val = Integer.parseInt(value);
                if(allowedValues != null){
                    for (int a : allowedValues){
                        if(val == a) return val;
                    }
                    value = null;
                } else {
                    return val;
                }
            } catch(NumberFormatException e){
                value = null;
            }
        }
        throw new RuntimeException("cant happen");
    }

    protected boolean captureMandatoryBoolean(Scanner input, PrintStream output, String question){
        String value = null;
        while(value == null){
            output.print(cli_reset(String.format("%s : ", question)));
            value = input.nextLine();
            value = value.trim();
            if(value.equalsIgnoreCase("y")) return true;
            if(value.equalsIgnoreCase("n")) return false;
            if(value.equalsIgnoreCase("yes")) return true;
            if(value.equalsIgnoreCase("no")) return false;
            if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")){
                return Boolean.parseBoolean(value);
            }
            value = null;
        }
        throw new RuntimeException("cant happen");
    }

    public void stop()
            throws MqttsnException, IOException {
        if(runtime != null){
            runtime.close();
        }
    }

    protected void quit()
            throws MqttsnException, IOException {
        if(runtime != null){
            stop();
            message("stopped - bye :-)");
        }
    }

    protected boolean needsHostname(){
        return true;
    }

    protected boolean needsClientId(){
        return true;
    }

    protected boolean needsPort(){
        return true;
    }

    protected abstract String getPropertyFileName();

    protected abstract boolean processCommand(String command) throws Exception;

    protected abstract MqttsnOptions createOptions() throws UnknownHostException;

    protected abstract IMqttsnTransport createTransport();

    protected abstract AbstractMqttsnRuntimeRegistry createRuntimeRegistry(MqttsnOptions options, IMqttsnTransport transport);

    protected abstract AbstractMqttsnRuntime createRuntime(AbstractMqttsnRuntimeRegistry registry, MqttsnOptions options);

    protected IMqttsnCodec createCodec(){
        return protocolVersion == 1 ? MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2 :
                MqttsnCodecs.MQTTSN_CODEC_VERSION_2_0;
    }

    protected AbstractMqttsnRuntimeRegistry getRuntimeRegistry(){
        return runtimeRegistry;
    }

    protected AbstractMqttsnRuntime getRuntime(){
        return runtime;
    }

    protected MqttsnOptions getOptions(){
        return options;
    }
}
