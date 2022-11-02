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

package org.slj.mqtt.sn.client.impl.cli;

import org.slj.mqtt.sn.cli.AbstractInteractiveCli;
import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.client.impl.MqttsnClient;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntime;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.model.session.impl.MqttsnWillDataImpl;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.TopicPath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

public abstract class MqttsnInteractiveClient extends AbstractInteractiveCli {

    static final String TEST_TOPIC = "test/topic";
    static final int TEST_PAUSE = 500;


    enum COMMANDS {
        LOOP("Create <count> messages in a loop", new String[]{"int count", "String* topicName", "String* data", "int QoS"}),
        STATS("View statistics for runtime", new String[0]),
        TD("Output a thread dump", new String[0]),
        QUIET("Switch off the sent and receive listeners", new String[0]),
        LOUD("Switch on the sent and receive listeners", new String[0]),
        RESET("Reset the stats and the local queue", new String[0]),
        CONNECT("Connect to the gateway and establish a new session", new String[]{"boolean cleanSession", "int16 keepAlive"}),
        WILL("Set will data on your runtime", new String[0]),
        SUBSCRIBE("Subscribe to a topic", new String[]{"String* topicName", "int QoS"}),
        DISCONNECT("Disconnect from the gateway session", new String[0]),
        SLEEP("Send the remote session to sleep", new String[]{"int16 duration"}),
        WAKE("Wake from a sleep to check for messages", new String[0]),
        PUBLISH("Publish a new message", new String[]{"String* topicName", "String* data", "int QoS"}),
        UNSUBSCRIBE("Unsubscribe an existing topic subscription", new String[]{"String* topicName"}),
        STATUS("Obtain the status of the client", new String[0]),
        SESSION("Obtain the local session details", new String[0]),
        NETWORK("Get network details", new String[0]),
        HELO("Send a HELO message to gateway", new String[0]),
        PREDEFINE("Add a predefined topic alias", new String[]{"String* topicName",  "int16 topicAlias"}),
        HELP("List this message", new String[0]),
        GC("Run the garbage collector", new String[0]),
        QUIT("Quit the application", new String[0]),
        EXIT("Quit the application", new String[0], true),
        QC("Quick connect configuration", new String[0], true),
        BYE("Quit the application", new String[0], true);

        private String description;
        private String[] arguments;
        private boolean hidden = false;

        COMMANDS(String description, String[] arguments, boolean hidden){
            this(description, arguments);
            this.hidden = hidden;
        }

        COMMANDS(String description, String[] arguments){
            this.description = description;
            this.arguments = arguments;
        }

        public boolean isHidden() {
            return hidden;
        }

        public String[] getArguments() {
            return arguments;
        }

        public String getDescription(){
            return description;
        }
    }

    protected boolean processCommand(String command) throws Exception {
        COMMANDS c = COMMANDS.valueOf(command.toUpperCase());
        processCommand(c);
        if(c == COMMANDS.QUIT || c == COMMANDS.BYE || c == COMMANDS.EXIT){
            return false;
        }
        return true;
    }

    protected void processCommand(COMMANDS command) throws IOException {
        try {
            switch (command){
                case HELP:
                    for(COMMANDS c : COMMANDS.values()){
                        if(c.isHidden()) continue;
                        StringBuilder sb = new StringBuilder();
                        for(String a : c.getArguments()){
                            if(sb.length() > 0){
                                sb.append(", ");
                            }
                            sb.append(a);
                        }
                        output.println("\t" + c.name());
                        output.println("\t\t" + c.getDescription());
                    }
                    break;
                case TD:
                    threadDump();
                    break;
                case GC:
                    gc();
                    break;
                case QC:
                    quickConnect();
                    break;
                case CONNECT:
                    connect(
                            captureMandatoryBoolean(input, output, "Would you like a clean session?"),
                            captureMandatoryInt(input, output, "How long would you like your keepAlive to be (in seconds)?", null));
                    break;
                case WILL:
                    will(
                            captureMandatoryBoolean(input, output, "Is your will message a retained message?"),
                            captureMandatoryString(input, output, "Which topic is your will message destined for?"),
                            captureMandatoryString(input, output, "What is the will message you would like to publish?"),
                            captureMandatoryInt(input, output, "At which QoS would you like to subscribe (0,1,2)?", ALLOWED_QOS));
                    break;
                case SUBSCRIBE:
                    subscribe(
                            captureMandatoryString(input, output, "Which topic would you like to subscribe to?"),
                            captureMandatoryInt(input, output, "At which QoS would you like to subscribe (0,1,2)?", ALLOWED_QOS));
                    break;
                case UNSUBSCRIBE:
                    unsubscribe(
                            captureMandatoryString(input, output, "Which topic would you like to unsubscribe from?"));
                    break;
                case LOOP:
                    loop(
                            captureMandatoryInt(input, output, "How many messages would you like to send?", null),
                            captureMandatoryString(input, output, "Which topic would you like to publish to?"),
                            captureMandatoryInt(input, output, "At which QoS would you like to publish (-1,0,1,2)?", ALLOWED_QOS));
                    break;
                case PUBLISH:
                    publish(
                            captureMandatoryString(input, output, "Which topic would you like to publish to?"),
                            captureMandatoryInt(input, output, "At which QoS would you like to publish (-1,0,1,2)?", ALLOWED_QOS),
                            captureMandatoryBoolean(input, output, "Is this a retained publish?"),
                            captureString(input, output, "What is the message you would like to publish?"));
                    break;
                case SLEEP:
                    sleep(captureMandatoryInt(input, output, "How long would you like to sleep for (in seconds)?", null));
                    break;
                case WAKE:
                    wake();
                    break;
                case HELO:
                    helo();
                    break;
                case STATS:
                    stats();
                    break;
                case NETWORK:
                    network();
                    break;
                case STATUS:
                    status();
                    break;
                case SESSION:
                    String sessionId = storageService.getStringPreference(RuntimeConfig.CLIENTID, null);
                    if(sessionId == null){
                        sessionId = captureMandatoryString(input, output, "Could not determine clientId from runtime, please supply to view session?");
                    }
                    session(sessionId);
                    break;
                case QUIET:
                    disableOutput();
                    break;
                case LOUD:
                    enableOutput();
                    break;
                case PREDEFINE:
                    predefine(
                            captureMandatoryString(input, output, "What is the topic you would like to predefine?"),
                            captureMandatoryInt(input, output, "What is the alias for the topic?", null));
                    break;
                case EXIT:
                case BYE:
                case QUIT:
                    quit();
                case DISCONNECT:
                    disconnect();
                    break;
            }
        } catch(Exception e){
            error( "An error occurred running your command.", e);
        }
    }

    @Override
    protected String getCLIName(){
        return "org slj Mqtt-sn interactive client";
    }

    protected void connect(boolean cleanSession, int keepAlive)
            throws IOException, MqttsnException {
        MqttsnClient client = (MqttsnClient) getRuntime();
        if(client != null && !client.isConnected()){
            try {
                client.connect(keepAlive, cleanSession);
                message("DONE - connect issued successfully, client is connected");
            } catch(MqttsnClientConnectException e){
                error("Client Reporting Connection Error", e);
            }
        } else {
            message("Client is already connected");
        }
    }

    protected void quickConnect()
            throws IOException, MqttsnException {
        MqttsnClient client = (MqttsnClient) getRuntime();
        if(client != null && client.isConnected()){
            client.disconnect();
            message("DONE - initially disconnected an existing session");
        }
        if(client != null && !client.isConnected()){
            try {
                client.connect(240, true);
                message("DONE - quick connect issued successfully, client is connected with clean session and keepAlive 240");
                client.subscribe("foo", 2);
                message("DONE - quick subscribe to foo issued successfully");
            } catch(MqttsnClientConnectException e){
                error("Client Reporting Connection Error", e);
            }
        } else {
            message("Client is already connected");
        }
    }

    protected void will(boolean retained, String topic, String data, int QoS) throws MqttsnException {

        MqttsnClient client = (MqttsnClient) getRuntime();
        MqttsnWillDataImpl willData = new MqttsnWillDataImpl(new TopicPath(topic), data.getBytes(StandardCharsets.UTF_8), QoS, retained);
        client.setWillData(willData);
        message("DONE - successfully set will message data on runtime");
    }

    protected void loop(int count, String topicPath, int qos)
            throws IOException, MqttsnException {
        for (int i = 0; i < count; i++){
            publish(topicPath, qos, false, "message " + (i + 1));
            try {
                //the queue ordering is done using a natural order on
                //timestamp so ensure we are always 1 ms between
                Thread.sleep(1);
            } catch(Exception e){}
        }
    }

    protected void publish(String topicPath, int qos, boolean retained, String data)
            throws IOException, MqttsnException {
        MqttsnClient client = (MqttsnClient) getRuntime();
        if(client != null && (client.isConnected() || qos == -1)){
            try {
                client.publish(topicPath, qos, retained, data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8));
                if(!client.isConnected()){
                    boolean stopAfterUse = false;
                    try {
                        if(!getRuntimeRegistry().getQueueProcessor().running()){
                            getRuntimeRegistry().getQueueProcessor().start(getRuntimeRegistry());
                            stopAfterUse = true;
                        }
                        getRuntimeRegistry().getMessageStateService().scheduleFlush(
                                client.getSessionState().getContext());
                        try {
                            Thread.sleep(1000);
                        } catch(Exception e){
                        }
                    } finally{
                        if(stopAfterUse) getRuntimeRegistry().getQueueProcessor().stop();
                    }
                    message("DONE - message sent and flushed on DISCONNECTED session");
                }
                else {
                    message("DONE - message queued for sending");
                }
            } catch(MqttsnQueueAcceptException e){
                error("Client Reporting Queue Accept Error", e);
            }
        } else {
            message("Client must first be connected before issuing this command");
        }
    }

    protected void subscribe(String topicPath, int qos)
            throws IOException, MqttsnException {
        MqttsnClient client = (MqttsnClient) getRuntime();
        if(client != null && client.isConnected()){
            client.subscribe(topicPath, qos);
            message("DONE - subscribe issued successfully");
        } else {
            message("Client must first be connected before issuing this command");
        }
    }

    protected void unsubscribe(String topicPath)
            throws IOException, MqttsnException {
        MqttsnClient client = (MqttsnClient) getRuntime();
        if(client != null && client.isConnected()){
            client.unsubscribe(topicPath);
            message("DONE - unsubscribe issued successfully");
        } else {
            message("Client must first be connected before issuing this command");
        }
    }

    protected void sleep(int duration)
            throws IOException, MqttsnException {
        MqttsnClient client = (MqttsnClient) getRuntime();
        if(client != null && client.isConnected()){
            client.sleep(duration);
            message(String.format("DONE - client is sleeping for %s seconds", duration));
        } else {
            message("Client must first be connected before issuing this command");
        }
    }

    protected void wake()
            throws IOException, MqttsnException {
        MqttsnClient client = (MqttsnClient) getRuntime();
        if(client != null && client.isAsleep()){
            client.wake(60000);
            message("DONE - client received all messages and is back sleeping");
        } else {
            message("Client must first be asleep before issuing this command");
        }
    }

    protected void helo()
            throws IOException, MqttsnException {
        MqttsnClient client = (MqttsnClient) getRuntime();
        if(client != null && client.isConnected()){
            message("Sending HELO message...");
            String userAgent = client.helo();
            message(String.format("Gateway responded with [%s]", userAgent));
        }
    }


    protected void quit()
            throws MqttsnException, IOException {
        MqttsnClient client = (MqttsnClient) getRuntime();
        if(client != null){
            stop();
            message("Client stopped - bye :-)");
        }
    }

    public void stop()
            throws MqttsnException, IOException {
        MqttsnClient client = (MqttsnClient) getRuntime();
        if(client != null){
            client.close();
        }
    }

    protected void disconnect()
            throws IOException, MqttsnException {
        MqttsnClient client = (MqttsnClient) getRuntime();
        if(client != null){
            client.disconnect();
            message("DONE - client is disconnected");
        }
    }

    protected void status() {
        MqttsnClient client = (MqttsnClient) getRuntime();
        message(String.format("Remote Host: %s", storageService.getStringPreference(RuntimeConfig.HOSTNAME, null)));
        message(String.format("Remote Port: %s", storageService.getIntegerPreference(RuntimeConfig.PORT, null)));
        message(String.format("Client Id: %s", storageService.getStringPreference(RuntimeConfig.CLIENTID, null)));
        if(client != null){
            if(runtimeRegistry != null) {
                if (runtimeRegistry.getOptions() != null) {
                    Map<String, Integer> pTopics = runtimeRegistry.getOptions().getPredefinedTopics();
                    if(pTopics != null){
                        message( "Predefined Topic Count: " + pTopics.size());
                        Iterator<String> itr = pTopics.keySet().iterator();
                        while(itr.hasNext()){
                            String topic = itr.next();
                            tabmessage(String.format("%s = %s", topic, pTopics.get(topic)));
                        }
                    }
                }

                if (getRuntimeRegistry().getQueueProcessor() != null) {
                    message(String.format("Queue Processor: %s", (getRuntimeRegistry().getQueueProcessor().running() ?
                            cli_green("Running") : cli_red("Stopped"))));
                }
            }
        } else {
            message( "Client Status: Awaiting Connection..");
        }
    }

    @Override
    protected IMqttsnTransport createTransport(IMqttsnStorageService storageService) {
        MqttsnUdpOptions udpOptions = new MqttsnUdpOptions().withMtu(4096).withReceiveBuffer(4096).
                withPort(MqttsnUdpOptions.DEFAULT_LOCAL_CLIENT_PORT);
        return new MqttsnUdpTransport(udpOptions);
    }

    @Override
    protected MqttsnOptions createOptions(IMqttsnStorageService storageService) {
        try {
            MqttsnOptions options = new MqttsnOptions();
            String clientId = storageService.getStringPreference(RuntimeConfig.CLIENTID, null);
            options.withNetworkAddressEntry(clientId,
                    NetworkAddress.from(storageService.getIntegerPreference(RuntimeConfig.PORT, null),
                            storageService.getStringPreference(RuntimeConfig.HOSTNAME, null)));
            options.withContextId(clientId);
            options.withMaxProtocolMessageSize(4096);
            return options;
        } catch(Exception e){
            throw new MqttsnRuntimeException(e);
        }
    }

    @Override
    protected AbstractMqttsnRuntime createRuntime(AbstractMqttsnRuntimeRegistry registry, MqttsnOptions options) {
        MqttsnClient runtime = new MqttsnClient(true, false);
        return runtime;
    }

    @Override
    public void start(IMqttsnStorageService storageService) throws Exception {
        super.start(storageService);
        getRuntime().start(getRuntimeRegistry());
    }
}