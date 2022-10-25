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
import org.slj.mqtt.sn.impl.metrics.IMqttsnMetrics;
import org.slj.mqtt.sn.model.*;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.IMqttsnSubscription;
import org.slj.mqtt.sn.model.session.IMqttsnTopicRegistration;
import org.slj.mqtt.sn.model.session.IMqttsnWillData;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.VirtualMachine;
import org.slj.mqtt.sn.utils.ThreadDump;
import org.slj.mqtt.sn.utils.TopicPath;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public abstract class AbstractInteractiveCli {
    protected static final int MAX_ALLOWED_CLIENTID_LENGTH = 255;
    protected static final String HOSTNAME = "hostName";
    protected static final String CLIENTID = "clientId";
    protected static final String PORT = "port";
    protected static final String PROTOCOL_VERSION = "protocolVersion";
    protected boolean colors = false;
    protected AbstractMqttsnRuntimeRegistry runtimeRegistry;
    protected AbstractMqttsnRuntime runtime;
    protected IMqttsnStorageService storageService;

    protected PrintStream output;
    protected Scanner input;
    protected boolean enableOutput = true;
    protected volatile boolean closed = false;
    private volatile boolean lastCursorAsync = false;

    public void init(Scanner input, PrintStream output){
        this.input = input;
        this.output = output;
        colors = !java.lang.System.getProperty("os.name").contains("Windows");
    }

    public void start(IMqttsnStorageService storageService) throws Exception {
        if(input == null || output == null || storageService == null) throw new IllegalStateException("no init");
        this.storageService = storageService;
        message(String.format("Creating runtime registry.. DONE"));
        runWizard();
        MqttsnOptions options = createOptions(storageService);
        runtimeRegistry = createRuntimeRegistry(storageService, options, createTransport(storageService));
        IMqttsnCodec codec = createCodec(storageService);
        runtimeRegistry.withCodec(codec);
        message(String.format("Starting up interactive CLI with clientId=%s", options.getContextId()));
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
                if(enableOutput) asyncmessage(String.format("[>>>] Publish received [%s] bytes on [%s] from [%s] \"%s\"",
                        data.length, topic, context.getId(), new String(data)));
            } catch(Exception e){
                e.printStackTrace();
            }
        });
        runtime.registerPublishSentListener((IMqttsnContext context, UUID messageId, TopicPath topic, int qos, boolean retained, byte[] data, IMqttsnMessage message) -> {
            try {
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
                int QoS = runtimeRegistry.getCodec().getQoS(message, true);
                asyncmessage(String.format("[xxx] Publish failure (tried [%s] times) [%s] bytes on topic [%s], at QoS [%s] \"%s\"",
                        retryCount, data.length, topic, QoS, new String(data)));
            } catch(Exception e){
                e.printStackTrace();
            }
        });
        message("Adding traffic listeners.. DONE");
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

    public void runWizard() throws MqttsnException {
        String clientId = storageService.getStringPreference(CLIENTID, null);
        boolean useWizard = clientId == null;
        if(!useWizard) {
            useWizard = !captureMandatoryBoolean(input, output,
                            String.format("I managed to load clientId=%s profile from history, would you like to use it?", clientId));
        }
        if(useWizard){
            captureSettings();
        }
    }

    protected void captureSettings() throws MqttsnException {
        if(needsHostname()){
            String hostName;
            do{
                hostName = captureMandatoryString(input, output, "Please enter a valid host name or ip address");
            } while(!validHost(hostName));
            storageService.setStringPreference(HOSTNAME, hostName);
        }

        if(needsPort()){
            storageService.setIntegerPreference(PORT,
                    captureMandatoryInt(input, output, "Please enter a remote port", null));
        }

        if(needsClientId()){
            String clientId;
            do{
                clientId = captureString(input, output,  "Please enter a valid client or gateway Id");
            } while(!validClientId(clientId, MAX_ALLOWED_CLIENTID_LENGTH));
            storageService.setStringPreference(CLIENTID, clientId);
        }

        int protocolVersion;
        do{
            protocolVersion = captureMandatoryInt(input, output,  "Please enter a protocol version (1 for 1.2 or 2 for 2.0)", new int[] {1, 2});
        } while(!validProtocolVersion(protocolVersion));
        storageService.setIntegerPreference(PROTOCOL_VERSION, protocolVersion);
    }

//    protected void loadFromSettings() throws MqttsnException {
//        hostName = storageService.getStringPreference(HOSTNAME, null);
//        port = storageService.getIntegerPreference(PORT, null);
//        protocolVersion = storageService.getIntegerPreference(PROTOCOL_VERSION, null);
//        clientId = storageService.getStringPreference(CLIENTID, null);
//    }
//
//    protected void saveToSettings() throws MqttsnException {
//        storageService.setStringPreference(HOSTNAME, hostName);
//        storageService.setIntegerPreference(PORT, port);
//        storageService.setIntegerPreference(PROTOCOL_VERSION, protocolVersion);
//        storageService.setStringPreference(CLIENTID, clientId);
//    }

//    protected boolean configOk(){
//        if(needsHostname()){
//            if(hostName == null) return false;
//        }
//        if(needsPort()){
//            if(port == 0) return false;
//        }
//        return validProtocolVersion();
//    }

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

    public void message_nochev(String message) {
        StringBuilder sb = new StringBuilder("\t\t");
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

    protected boolean validHost(String hostName){
        if(hostName == null) return false;
        if(hostName.trim().length() == 0) return false;
        Pattern p = Pattern.compile("[^a-zA-Z0-9.-]");
        return !p.matcher(hostName).find();
    }

    protected boolean validProtocolVersion(int protocolVersion){
        if(protocolVersion == 1 || protocolVersion == 2) return true;
        return false;
    }

    protected boolean validClientId(String clientId, int maxLength){
        if(clientId == null) return true;
        if(clientId.trim().length() > maxLength) return false;
        Pattern p = Pattern.compile("[a-zA-Z0-9\\-]{1,65528}");
        return p.matcher(clientId).find();
    }

    protected String getCLIName(){
        return "org.slj interactive command line";
    }

    protected void predefine(String topicName, int alias) {
        if(runtime != null && runtimeRegistry != null){
            runtimeRegistry.getOptions().getPredefinedTopics().put(topicName, alias);
            message("DONE - predefined topic registered successfully");
        } else {
            message("Cannot add a topic to an uninitialised runtime");
        }
    }

    protected void stats() {
        //-- enabled by metrics
        if(runtimeRegistry.getOptions().isMetricsEnabled()){
            IMqttsnMetricsService metricsService = runtimeRegistry.getMetrics();
            if(metricsService != null){
                message_nochev("======== Network Stats ========");
                message_nochev(String.format("Publish Sent Count: %s messages(s)", getValueSafe(metricsService.getTotalValue(IMqttsnMetrics.PUBLISH_MESSAGE_OUT))));
                message_nochev(String.format("Publish Receive Count: %s messages(s)", getValueSafe(metricsService.getTotalValue(IMqttsnMetrics.PUBLISH_MESSAGE_IN))));
                message_nochev(String.format("Network Bytes Sent: %s byte(s)", getValueSafe(metricsService.getTotalValue(IMqttsnMetrics.NETWORK_BYTES_OUT))));
                message_nochev(String.format("Network Bytes Received: %s byte(s)", getValueSafe(metricsService.getTotalValue(IMqttsnMetrics.NETWORK_BYTES_IN))));
                message_nochev(String.format("Peak Send Count: %s /ps", getValueSafe(metricsService.getMaxValue(IMqttsnMetrics.PUBLISH_MESSAGE_OUT))));
                message_nochev(String.format("Peak Receive Count: %s /ps", getValueSafe(metricsService.getMaxValue(IMqttsnMetrics.PUBLISH_MESSAGE_IN))));
                message_nochev(String.format("Current Send Count: %s /ps", getValueSafe(metricsService.getLatestValue(IMqttsnMetrics.PUBLISH_MESSAGE_OUT))));
                message_nochev(String.format("Current Receive Count: %s /ps", getValueSafe(metricsService.getLatestValue(IMqttsnMetrics.PUBLISH_MESSAGE_IN))));
                message_nochev("");
                message_nochev("======== Session Stats ========");
                message_nochev(String.format("Current Active Sessions: %s", getValueSafe(metricsService.getLatestValue(IMqttsnMetrics.SESSION_ACTIVE_REGISTRY_COUNT))));
                message_nochev(String.format("Current Asleep Sessions: %s", getValueSafe(metricsService.getLatestValue(IMqttsnMetrics.SESSION_ASLEEP_REGISTRY_COUNT))));
                message_nochev(String.format("Current Awake Sessions: %s", getValueSafe(metricsService.getLatestValue(IMqttsnMetrics.SESSION_AWAKE_REGISTRY_COUNT))));
                message_nochev(String.format("Current Disconnected Sessions: %s", getValueSafe(metricsService.getLatestValue(IMqttsnMetrics.SESSION_DISCONNECTED_REGISTRY_COUNT))));
                message_nochev(String.format("Current Lost Sessions: %s", getValueSafe(metricsService.getLatestValue(IMqttsnMetrics.SESSION_LOST_REGISTRY_COUNT))));
                message_nochev("");
                message_nochev("======== System Stats ========");
                message_nochev(String.format("JVM Used Memory: %s", VirtualMachine.getMemoryString(getValueSafe(metricsService.getLatestValue(IMqttsnMetrics.SYSTEM_VM_MEMORY_USED)))));
                message_nochev(String.format("JVM Threads: %s", getValueSafe(metricsService.getLatestValue(IMqttsnMetrics.SYSTEM_VM_THREADS_USED))));
            } else {
                message("metrics disabled by service");
            }
        } else {
            message("metrics disabled by config");
        }
    }

    public static long getValueSafe(Long l){
        return l == null ? 0 : l.longValue();
    }

    protected void session(String clientId)
            throws MqttsnException {

        Optional<IMqttsnContext> context =
                getRuntimeRegistry().getSessionRegistry().lookupClientIdSession(clientId);
        if(context.isPresent()){
            IMqttsnContext c = context.get();
            IMqttsnSession session = getRuntimeRegistry().getSessionRegistry().getSession(c, false);
            if(session != null){
                message(String.format("Session: %s", clientId));
                message(String.format("Session started: %s", format(session.getSessionStarted())));
                message(String.format("Last seen:  %s", session.getLastSeen() == null ? "<null>" : format(session.getLastSeen())));
                message(String.format("Keep alive (seconds):  %s", session.getKeepAlive()));
                message(String.format("Session expiry interval (seconds):  %s", session.getSessionExpiryInterval()));

                if(session.getSessionStarted() != null){
                    message(String.format("Time since connect (seconds):  %s", ((java.lang.System.currentTimeMillis() - session.getSessionStarted().getTime()) / 1000)));
                }

                if(session.getLastSeen() != null){
                    message(String.format("Time since last seen (seconds):  %s", ((java.lang.System.currentTimeMillis() - session.getLastSeen().getTime()) / 1000)));
                }

                message(String.format("State:  %s", getColorForState(session.getClientState())));
                message(String.format("Queue size:  %s", runtimeRegistry.getMessageQueue().size(session)));
                message(String.format("Inflight (Egress):  %s", runtimeRegistry.getMessageStateService().countInflight(
                        session.getContext(), IMqttsnOriginatingMessageSource.LOCAL)));
                message(String.format("Inflight (Ingress):  %s", runtimeRegistry.getMessageStateService().countInflight(
                        session.getContext(), IMqttsnOriginatingMessageSource.REMOTE)));

                Set<IMqttsnSubscription> subs = runtimeRegistry.getSubscriptionRegistry().readSubscriptions(session);
                message("Subscription(s): ");
                Iterator<IMqttsnSubscription> itr = subs.iterator();
                while(itr.hasNext()){
                    IMqttsnSubscription s = itr.next();
                    tabmessage(String.format("%s -> %s", s.getTopicPath(), s.getGrantedQoS()));
                }

                Set<IMqttsnTopicRegistration> regs = runtimeRegistry.getTopicRegistry().getRegistrations(session);
                message("Registrations(s): ");
                Iterator<IMqttsnTopicRegistration> regItr = regs.iterator();
                while(regItr.hasNext()){
                    IMqttsnTopicRegistration s = regItr.next();
                    tabmessage(String.format("%s -> %s (%s)", s.getTopicPath(), s.getAliasId(), s.isConfirmed()));
                }

                if(runtimeRegistry.getWillRegistry().hasWillMessage(session)){
                    IMqttsnWillData data = runtimeRegistry.getWillRegistry().getWillMessage(session);
                    message(String.format("Will QoS: %s", data.getQos()));
                    message(String.format("Will Topic: %s", data.getTopicPath()));
                    message(String.format("Will Retained: %s", data.isRetained()));
                    message(String.format("Will Data: %s", data.getData().length));
                }
                INetworkContext networkContext = runtimeRegistry.getNetworkRegistry().getContext(c);
                message(String.format("Network Address(s): %s", networkContext.getNetworkAddress()));
            }
            else {
                message(String.format("No session found: %s", clientId));
            }
        } else {
            message(String.format("Invalid clientId: %s", clientId));
        }
    }

    protected void sessions() throws MqttsnException {
        Iterator<IMqttsnSession> itr = getRuntimeRegistry().getSessionRegistry().iterator();
        message("Sessions(s): ");
        while(itr.hasNext()){
            IMqttsnSession session = itr.next();
            INetworkContext networkContext = getRuntimeRegistry().getNetworkRegistry().getContext(session.getContext());
            tabmessage(String.format("%s (%s) [%s] -> %s", session.getContext().getId(),
                    getRuntimeRegistry().getMessageQueue().size(session),
                    networkContext.getNetworkAddress(), getColorForState(session.getClientState())));
        }
    }

    protected void network() throws NetworkRegistryException {
        message("Network registry: ");
        Iterator<INetworkContext> itr = getRuntimeRegistry().getNetworkRegistry().iterator();
        while(itr.hasNext()){
            INetworkContext c = itr.next();
            message("\t" + c + " -> " + getRuntimeRegistry().getNetworkRegistry().getMqttsnContext(c));
        }
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

    protected static String format(Date d){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return sdf.format(d);
    }

    public String getColorForState(MqttsnClientState state){
        if(state == null) return cli_reset("N/a");
        switch(state){
            case AWAKE:
            case ACTIVE: return cli_green(state.toString());
            case ASLEEP: return cli_blue(state.toString());
            default: return cli_red(state.toString());
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

    protected abstract boolean processCommand(String command) throws Exception;

    protected abstract MqttsnOptions createOptions(IMqttsnStorageService storageService) ;

    protected abstract IMqttsnTransport createTransport(IMqttsnStorageService storageService);

    protected abstract AbstractMqttsnRuntimeRegistry createRuntimeRegistry(IMqttsnStorageService storageService, MqttsnOptions options, IMqttsnTransport transport);

    protected abstract AbstractMqttsnRuntime createRuntime(AbstractMqttsnRuntimeRegistry registry, MqttsnOptions options);

    protected IMqttsnCodec createCodec(IMqttsnStorageService storageService){
        int protocolVersion = storageService.getIntegerPreference(PROTOCOL_VERSION, 1);
        return protocolVersion == 1 ? MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2 :
                MqttsnCodecs.MQTTSN_CODEC_VERSION_2_0;
    }

    protected AbstractMqttsnRuntimeRegistry getRuntimeRegistry(){
        return runtimeRegistry;
    }

    protected AbstractMqttsnRuntime getRuntime(){
        return runtime;
    }
}
