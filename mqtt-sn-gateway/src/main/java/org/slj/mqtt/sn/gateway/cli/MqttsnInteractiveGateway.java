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

import org.slj.mqtt.sn.MqttsnConstants;
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
import org.slj.mqtt.sn.net.MqttsnUdpBatchTransport;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
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
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        gatewayRuntimeRegistry.getBackendService().pokeQueue();
    }

    protected void reinit() throws MqttsnBackendException {
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        gatewayRuntimeRegistry.getBackendService().reinit();
    }

    @Override
    protected void resetMetrics() throws IOException {
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        gatewayRuntimeRegistry.getBackendService().clearStats();
        ((MqttsnGatewaySessionService)gatewayRuntimeRegistry.getGatewaySessionService()).reset();
        super.resetMetrics();
    }

    @Override
    protected void stats() {
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        super.stats();

        if(((MqttsnGatewayOptions)options).isRealtimeMessageCounters()){
            message(String.format("Peak Send Count: %s /ps", ((MqttsnGateway)gatewayRuntimeRegistry.getRuntime()).getPeakMessageSentPerSecond()));
            message(String.format("Peak Receive Count: %s /ps", ((MqttsnGateway)gatewayRuntimeRegistry.getRuntime()).getPeakMessageReceivePerSecond()));
            message(String.format("Current Send Count: %s /ps", ((MqttsnGateway)gatewayRuntimeRegistry.getRuntime()).getCurrentMessageSentPerSecond()));
            message(String.format("Current Receive Count: %s /ps", ((MqttsnGateway)gatewayRuntimeRegistry.getRuntime()).getCurrentMessageReceivePerSecond()));
        }

        message(String.format("Expansion Count: %s", ((MqttsnGatewaySessionService)gatewayRuntimeRegistry.getGatewaySessionService()).getExpansionCount()));
        message(String.format("Last Publish Attempt: %s", ((MqttsnAggregatingGateway)gatewayRuntimeRegistry.getBackendService()).getLastPublishAttempt()));
        message(String.format("Aggregated Broker Queue: %s message(s)", gatewayRuntimeRegistry.getBackendService().getQueuedCount()));
        message(String.format("Aggregated Publish Sent: %s message(s)", gatewayRuntimeRegistry.getBackendService().getPublishSentCount()));
        message(String.format("Aggregated Publish Received: %s message(s)", gatewayRuntimeRegistry.getBackendService().getPublishReceiveCount()));
    }

    protected void queue(String topicName, String payload, boolean retained, int qos)
            throws MqttsnException {

        message("Enqueued publish to all subscribed sessions: " + topicName);
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        gatewayRuntimeRegistry.getGatewaySessionService().receiveToSessions(topicName, qos, retained, payload.getBytes(StandardCharsets.UTF_8));
    }

    protected void network() throws NetworkRegistryException {
        message("Network registry: ");
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        Iterator<INetworkContext> itr = gatewayRuntimeRegistry.getNetworkRegistry().iterator();
        while(itr.hasNext()){
            INetworkContext c = itr.next();
            message("\t" + c + " -> " + gatewayRuntimeRegistry.getNetworkRegistry().hasBoundSessionContext(c));
        }
    }

    protected void sessions() throws MqttsnException {
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        Iterator<IMqttsnContext> itr = gatewayRuntimeRegistry.getGatewaySessionService().iterator();
        message("Sessions(s): ");
        while(itr.hasNext()){

            IMqttsnContext c = itr.next();
            INetworkContext networkContext = gatewayRuntimeRegistry.getNetworkRegistry().getContext(c);
            IMqttsnSessionState state = gatewayRuntimeRegistry.
                    getGatewaySessionService().getSessionState(c, false);
            tabmessage(String.format("%s (%s) [%s] -> %s", c.getId(),
                    gatewayRuntimeRegistry.getMessageQueue().size(c),
                    networkContext.getNetworkAddress(), getColorForState(state.getClientState())));
        }
    }

    protected void session(String clientId)
            throws MqttsnException {
        MqttsnGatewayOptions opts = (MqttsnGatewayOptions) getOptions();
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        Optional<IMqttsnContext> context =
                gatewayRuntimeRegistry.getGatewaySessionService().lookupClientIdSession(clientId);
        if(context.isPresent()){
            IMqttsnContext c = context.get();
            IMqttsnSessionState state = gatewayRuntimeRegistry.
                    getGatewaySessionService().getSessionState(c, false);

            message(String.format("Client session: %s", clientId));
            message(String.format("Session started: %s", format(state.getSessionStarted())));
            message(String.format("Last seen:  %s", state.getLastSeen() == null ? "<null>" : format(state.getLastSeen())));
            message(String.format("Keep alive (seconds):  %s", state.getKeepAlive()));
            message(String.format("Session expiry interval (seconds):  %s", state.getSessionExpiryInterval()));
            message(String.format("Time since connect (seconds):  %s", ((System.currentTimeMillis() - state.getSessionStarted().getTime()) / 1000)));
            message(String.format("State:  %s", getColorForState(state.getClientState())));
            message(String.format("Queue size:  %s", gatewayRuntimeRegistry.getMessageQueue().size(c)));

            message(String.format("Inflight (Egress):  %s", gatewayRuntimeRegistry.getMessageStateService().countInflight(
                    state.getContext(), IMqttsnOriginatingMessageSource.LOCAL)));
            message(String.format("Inflight (Ingress):  %s", gatewayRuntimeRegistry.getMessageStateService().countInflight(
                    state.getContext(), IMqttsnOriginatingMessageSource.REMOTE)));

            Set<Subscription> subs = gatewayRuntimeRegistry.getSubscriptionRegistry().readSubscriptions(c);
            message("Subscription(s): ");
            Iterator<Subscription> itr = subs.iterator();
            while(itr.hasNext()){
                Subscription s = itr.next();
                tabmessage(String.format("%s -> %s", s.getTopicPath(), s.getQoS()));
            }

            if(gatewayRuntimeRegistry.getWillRegistry().hasWillMessage(c)){
                MqttsnWillData data = gatewayRuntimeRegistry.getWillRegistry().getWillMessage(c);
                message(String.format("Will QoS: %s", data.getQos()));
                message(String.format("Will Topic: %s", data.getTopicPath()));
                message(String.format("Will Retained: %s", data.isRetain()));
                message(String.format("Will Data: %s", data.getData().length));
            }

            INetworkContext networkContext = gatewayRuntimeRegistry.getNetworkRegistry().getContext(c);
            message(String.format("Network Address(s): %s", networkContext.getNetworkAddress()));

        } else {
            message(String.format("No session found: %s", clientId));
        }
    }

    protected void inflight(){
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
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
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        Optional<IMqttsnContext> context =
                gatewayRuntimeRegistry.getGatewaySessionService().lookupClientIdSession(clientId);
        if(context.isPresent()) {
            IMqttsnContext c = context.get();
            gatewayRuntimeRegistry.getMessageStateService().clearInflight(c);
            message(String.format("Inflight reaper run on: %s", clientId));
            gatewayRuntimeRegistry.getMessageStateService().scheduleFlush(c);
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

            Iterator<IMqttsnContext> sessionItr = gatewayRuntimeRegistry.getGatewaySessionService().iterator();
            List<IMqttsnSessionState> allState = new ArrayList<>();

            int queuedMessages = 0;
            while(sessionItr.hasNext()){
                IMqttsnContext c = sessionItr.next();
                IMqttsnSessionState state = gatewayRuntimeRegistry.getGatewaySessionService().
                        getSessionState(c, false);
                allState.add(state);
                queuedMessages += gatewayRuntimeRegistry.getMessageQueue().size(c);
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

    private static String format(Date d){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return sdf.format(d);
    }

    public String getColorForState(MqttsnClientState state){
        if(state == null) return cli_reset("N/a");
        switch(state){
            case AWAKE:
            case CONNECTED: return cli_green(state.toString());
            case ASLEEP: return cli_blue(state.toString());
            default: return cli_red(state.toString());
        }
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
