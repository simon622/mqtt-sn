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

package org.slj.mqtt.sn.gateway.cli;

import org.slj.mqtt.sn.cli.AbstractInteractiveCli;
import org.slj.mqtt.sn.gateway.impl.MqttsnGateway;
import org.slj.mqtt.sn.gateway.impl.MqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.gateway.impl.gateway.MqttsnGatewaySessionService;
import org.slj.mqtt.sn.gateway.impl.gateway.type.MqttsnAggregatingGateway;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBackendException;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntime;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.impl.AbstractMqttsnUdpTransport;
import org.slj.mqtt.sn.impl.ram.MqttsnInMemoryMessageStateService;
import org.slj.mqtt.sn.model.*;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.MqttsnClientState;
import org.slj.mqtt.sn.model.session.IMqttsnSubscription;
import org.slj.mqtt.sn.model.session.IMqttsnWillData;
import org.slj.mqtt.sn.model.session.impl.MqttsnSubscriptionImpl;
import org.slj.mqtt.sn.model.session.impl.MqttsnWillDataImpl;
import org.slj.mqtt.sn.net.MqttsnUdpBatchTransport;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.spi.IMqttsnOriginatingMessageSource;
import org.slj.mqtt.sn.spi.IMqttsnTransport;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.NetworkRegistryException;
import org.slj.mqtt.sn.utils.MqttsnUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class MqttsnInteractiveGateway extends AbstractInteractiveCli {

    static final String LISTEN_PORT = "listenPort";
    static final String USERNAME = "username";
    static final String PASSWORD = "password";

    protected String username;
    protected String password;

    protected int listenPort = MqttsnUdpOptions.DEFAULT_LOCAL_PORT;

    protected boolean needsBroker;

    enum COMMANDS {
        POKE("Poke the queue", new String[0], true),
        INFLIGHT("List inflight messages", new String[0], false),
        REINIT("Reinit the backend broker connection", new String[0]),
        FLUSH("Run inflight reaper on clientId", new String[0]),
        NETWORK("View network registry", new String[0]),
        SESSIONS("View sessions", new String[0]),
        TD("Output a thread dump", new String[0]),
        QUIET("Switch off the sent and receive listeners", new String[0]),
        LOUD("Switch on the sent and receive listeners", new String[0]),
        STATS("View statistics for runtime", new String[0]),
        RESET("Reset the stats", new String[0]),
        QUEUE("Queue a new message for clients", new String[]{"String* topicName, String* payload, int QoS"}),
        SESSION("Obtain the status of a client", new String[]{"String* clientId"}),
        STATUS("Obtain the status of the runtime", new String[0]),
        PREDEFINE("Add a predefined topic alias", new String[]{"String* topicName",  "int16 topicAlias"}),
        HELP("List this message", new String[0]),
        QUIT("Quit the application", new String[0]),
        EXIT("Quit the application", new String[0], true),
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

    public void init(boolean needsBroker, Scanner input, PrintStream output) {
        this.needsBroker = needsBroker;
        super.init(input, output);
    }

    protected MqttsnGatewayRuntimeRegistry getRuntimeRegistry(){
        return (MqttsnGatewayRuntimeRegistry) super.getRuntimeRegistry();
    }

    @Override
    protected boolean processCommand(String command) throws Exception {
        COMMANDS c = COMMANDS.valueOf(command.toUpperCase());
        processCommand(c);
        if(c == COMMANDS.QUIT || c == COMMANDS.BYE || c == COMMANDS.EXIT){
            return false;
        }
        return true;
    }

    protected void processCommand(COMMANDS command) throws Exception {
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
                case REINIT:
                    reinit();
                    break;
                case POKE:
                    poke();
                    break;
                case SESSION:
                    session(captureMandatoryString(input, output, "Please supply the clientId whose session you would like to see"));
                    break;
                case SESSIONS:
                    sessions();
                    break;
                case NETWORK:
                    network();
                    break;
                case STATS:
                    stats();
                    break;
                case RESET:
                    resetMetrics();
                    break;
                case STATUS:
                    status();
                    break;
                case TD:
                    threadDump();
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
                case QUEUE:
                    queue(
                            captureMandatoryString(input, output, "What is the topic you would like to publish to?"),
                            captureMandatoryString(input, output, "Supply the data to publish"),
                            captureMandatoryBoolean(input, output, "Is the message retained?"),
                            captureMandatoryInt(input, output, "What is QoS for the publish?", new int[]{0,1,2}));
                    break;
                case FLUSH:
                    flush(
                            captureMandatoryString(input, output, "What is the clientId you wish to flush?"));
                    break;
                case INFLIGHT:
                    inflight();
                    break;
                case EXIT:
                case BYE:
                case QUIT:
                    quit();
                    break;
            }
        } catch(Exception e){
            error( "An error occurred running your command.", e);
        }
    }

    protected void poke() throws MqttsnBackendException {
        getRuntimeRegistry().getBackendService().pokeQueue();
    }

    protected void reinit() throws MqttsnBackendException {
        getRuntimeRegistry().getBackendService().reinit();
    }

    @Override
    protected void resetMetrics() throws IOException {
        getRuntimeRegistry().getBackendService().clearStats();
        ((MqttsnGatewaySessionService)getRuntimeRegistry().getGatewaySessionService()).reset();
        super.resetMetrics();
    }

    @Override
    protected void stats() {
        super.stats();

        if(((MqttsnGatewayOptions)options).isRealtimeMessageCounters()){
            message(String.format("Peak Send Count: %s /ps", ((MqttsnGateway)getRuntimeRegistry().getRuntime()).getPeakMessageSentPerSecond()));
            message(String.format("Peak Receive Count: %s /ps", ((MqttsnGateway)getRuntimeRegistry().getRuntime()).getPeakMessageReceivePerSecond()));
            message(String.format("Current Send Count: %s /ps", ((MqttsnGateway)getRuntimeRegistry().getRuntime()).getCurrentMessageSentPerSecond()));
            message(String.format("Current Receive Count: %s /ps", ((MqttsnGateway)getRuntimeRegistry().getRuntime()).getCurrentMessageReceivePerSecond()));
        }

        message(String.format("Expansion Count: %s", ((MqttsnGatewaySessionService)getRuntimeRegistry().getGatewaySessionService()).getExpansionCount()));
        message(String.format("Last Publish Attempt: %s", ((MqttsnAggregatingGateway)getRuntimeRegistry().getBackendService()).getLastPublishAttempt()));
        message(String.format("Aggregated Broker Queue: %s message(s)", getRuntimeRegistry().getBackendService().getQueuedCount()));
        message(String.format("Aggregated Publish Sent: %s message(s)", getRuntimeRegistry().getBackendService().getPublishSentCount()));
        message(String.format("Aggregated Publish Received: %s message(s)", getRuntimeRegistry().getBackendService().getPublishReceiveCount()));
    }

    protected void queue(String topicName, String payload, boolean retained, int qos)
            throws MqttsnException {

        message("Enqueued publish to all subscribed sessions: " + topicName);
        getRuntimeRegistry().getGatewaySessionService().receiveToSessions(topicName, qos, retained, payload.getBytes(StandardCharsets.UTF_8));
    }

    protected void inflight(){
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = getRuntimeRegistry();
        List<IMqttsnContext> m = ((MqttsnInMemoryMessageStateService)gatewayRuntimeRegistry.getMessageStateService()).getActiveInflights();
        Iterator<IMqttsnContext> itr = m.iterator();
        while (itr.hasNext()){
            IMqttsnContext c = itr.next();
            renderInflight(c,
                ((MqttsnInMemoryMessageStateService)gatewayRuntimeRegistry.getMessageStateService()).getInflightMessages(c, IMqttsnOriginatingMessageSource.LOCAL));
            renderInflight(c,
                    ((MqttsnInMemoryMessageStateService)gatewayRuntimeRegistry.getMessageStateService()).getInflightMessages(c, IMqttsnOriginatingMessageSource.REMOTE));
        }
    }

    private void renderInflight(IMqttsnContext context, Map<Integer, InflightMessage> msgs){
        Iterator<Integer> i = msgs.keySet().iterator();
        while(i.hasNext()){
            Integer id = i.next();
            InflightMessage message = msgs.get(id);
            long sent = message.getTime();
            tabmessage(String.format("%s -> %s (%s retries) { %s (%s old)}", context.getId(), id,
                    message instanceof RequeueableInflightMessage ? ((RequeueableInflightMessage) message).getQueuedPublishMessage().getRetryCount() : 1,
                    message.getOriginatingMessageSource() + " " + message.getMessage().getMessageName(),
                    MqttsnUtils.getDurationString(System.currentTimeMillis() - sent)));
        }
    }

    protected void flush(String clientId) throws MqttsnException {

        Optional<IMqttsnContext> context =
                getRuntimeRegistry().getSessionRegistry().lookupClientIdSession(clientId);
        if(context.isPresent()) {
            IMqttsnContext c = context.get();
            getRuntimeRegistry().getMessageStateService().clearInflight(c);
            message(String.format("Inflight reaper run on: %s", clientId));
            getRuntimeRegistry().getMessageStateService().scheduleFlush(c);
        }
        else {
            message(String.format("No session found: %s", clientId));
        }
    }

    protected void status()
            throws IOException, MqttsnException {
        MqttsnGatewayOptions opts = (MqttsnGatewayOptions) getOptions();
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        if(runtime != null) {

            boolean connected = gatewayRuntimeRegistry.getBackendService().isConnected(null);

            int maxClients = opts.getMaxConnectedClients();
            int advertiseTime = opts.getGatewayAdvertiseTime();

            //-- general stuff
            message(String.format("Gateway Id: %s", opts.getGatewayId()));
            message(String.format("Advertise Interval: %s", advertiseTime));

            if(gatewayRuntimeRegistry.getTransport() instanceof AbstractMqttsnUdpTransport){
                MqttsnUdpOptions udpOptions = ((AbstractMqttsnUdpTransport)gatewayRuntimeRegistry.getTransport()).getUdpOptions();
                message(String.format("Host: %s", udpOptions.getHost()));
                message(String.format("Datagram port: %s", udpOptions.getPort()));
                message(String.format("Secure port: %s", udpOptions.getSecurePort()));
                message(String.format("Broadcast port: %s", udpOptions.getBroadcastPort()));
                message(String.format("MTU: %s", udpOptions.getMtu()));
            }

            message(String.format("Max message size: %s", getOptions().getMaxProtocolMessageSize()));
            message(String.format("Max connected clients: %s", maxClients));
            message(String.format("Message registry size: %s", getRuntimeRegistry().getMessageRegistry().size()));

            if (getOptions() != null) {
                Map<String, Integer> pTopics = getOptions().getPredefinedTopics();
                if(pTopics != null){
                    message(String.format("Predefined topic count: %s", pTopics.size()));
                    Iterator<String> itr = pTopics.keySet().iterator();
                    while(itr.hasNext()){
                        String topic = itr.next();
                        tabmessage(String.format("%s = %s", topic, pTopics.get(topic)));
                    }
                }
            }

            Iterator<IMqttsnSession> sessionItr = getRuntimeRegistry().getSessionRegistry().iterator();
            List<IMqttsnSession> allState = new ArrayList<>();
            int queuedMessages = 0;
            while(sessionItr.hasNext()){
                IMqttsnSession session = sessionItr.next();
                allState.add(session);
                queuedMessages += gatewayRuntimeRegistry.getMessageQueue().size(session);
            }

            message(String.format("Network registry count: %s", getRuntimeRegistry().getNetworkRegistry().size()));
            message(String.format("Current active/awake sessions: %s", allState.stream().filter(s -> MqttsnUtils.in(s.getClientState(), MqttsnClientState.CONNECTED, MqttsnClientState.AWAKE)).count()));
            message(String.format("Current sleeping sessions: %s", allState.stream().filter(s -> s.getClientState() == MqttsnClientState.ASLEEP).count()));
            message(String.format("Current disconnected sessions: %s", allState.stream().filter(s -> s.getClientState() == MqttsnClientState.DISCONNECTED).count()));
            message(String.format("Current lost sessions: %s", allState.stream().filter(s -> s.getClientState() == MqttsnClientState.LOST).count()));
            message(String.format("All queued session messages: %s", queuedMessages));

            //-- broker stuff
            message(String.format("Broker TCP/IP Connection State: %s", (connected ? cli_green("ESTABLISHED") : cli_red("UNESTABLISHED"))));

        } else {
            message( "Gateway status: awaiting connection..");
        }
    }

    @Override
    protected MqttsnOptions createOptions() {
        return new MqttsnGatewayOptions().
                withRealtimeMessageCounters(true).
                withMaxConnectedClients(100).
                withGatewayId(101).
                withContextId(clientId).
                withMaxMessagesInQueue(100).
                withRemoveDisconnectedSessionsSeconds(60 * 60).
                withTransportProtocolHandoffThreadCount(20).
                withQueueProcessorThreadCount(2).
                withMinFlushTime(5);
    }

    @Override
    protected IMqttsnTransport createTransport() {
        MqttsnUdpOptions udpOptions = new MqttsnUdpOptions().
                withPort(listenPort);
        return new MqttsnUdpBatchTransport(udpOptions, 2048);
    }

    @Override
    protected AbstractMqttsnRuntime createRuntime(AbstractMqttsnRuntimeRegistry registry, MqttsnOptions options) {
        MqttsnGateway gateway = new MqttsnGateway();
        return gateway;
    }

    @Override
    public void start() throws Exception {
        super.start();
        if(needsBroker){
            message(String.format("Attempting to connect to backend broker at %s:%s...", hostName, port));
        }
        try {
            getRuntime().start(getRuntimeRegistry(), false);
            if(needsBroker){
                message("Successfully connected to broker, TCP/IP connection active.");
            }
            message(String.format("Gateway listening for datagram traffic on port %s", listenPort));
        } catch(Exception e){
            message(cli_red("Unable to connect to broker"));
            message("Please check the connection details supplied");
            throw e;
        }
    }

    @Override
    protected void configure() throws IOException {
        super.configure();
        listenPort = captureMandatoryInt(input, output, "Please enter the local listen port", null);
        if(needsBroker){
            username = captureString(input, output, "Please enter a valid username for you broker connection");
            password = captureString(input, output,  "Please enter a valid password for you broker connection");
        }
    }

    @Override
    protected void loadConfigHistory(Properties props) throws IOException {
        super.loadConfigHistory(props);
        if(needsBroker){
            username = props.getProperty(USERNAME);
            password = props.getProperty(PASSWORD);
        }
        String listenPortStr = props.getProperty(LISTEN_PORT);
        if(listenPortStr != null){
            try {
                listenPort = Integer.valueOf(listenPortStr);
            } catch(Exception e){
            }
        }
    }

    @Override
    protected void saveConfigHistory(Properties props) {
        super.saveConfigHistory(props);
        if(needsBroker){
            props.setProperty(USERNAME, username);
            props.setProperty(PASSWORD, password);
        }
        props.setProperty(LISTEN_PORT, String.valueOf(listenPort));
    }

    @Override
    protected String getPropertyFileName() {
        return "gateway.properties";
    }

    @Override
    protected boolean needsHostname() {
        return needsBroker;
    }

    @Override
    protected boolean needsClientId() {
        return super.needsClientId();
    }

    @Override
    protected boolean needsPort() {
        return needsBroker;
    }
}
