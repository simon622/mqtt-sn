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
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntime;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.impl.AbstractMqttsnUdpTransport;
import org.slj.mqtt.sn.model.*;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.net.MqttsnUdpTransport;
import org.slj.mqtt.sn.spi.IMqttsnTransport;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.NetworkRegistryException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class MqttsnInteractiveGateway extends AbstractInteractiveCli {

    static final String USERNAME = "username";
    static final String PASSWORD = "password";

    protected String username;
    protected String password;

    enum COMMANDS {
        NETWORK("View network registry", new String[0]),
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
                case SESSION:
                    session(captureMandatoryString(input, output, "Please supply the clientId whose session you would like to see"));
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
                case PREDEFINE:
                    predefine(
                            captureMandatoryString(input, output, "What is the topic you would like to predefine?"),
                            captureMandatoryInt(input, output, "What is the alias for the topic?", null));
                    break;
                case QUEUE:
                    queue(
                            captureMandatoryString(input, output, "What is the topic you would like to publish to?"),
                            captureMandatoryString(input, output, "Supply the data to publish"),
                            captureMandatoryInt(input, output, "What is QoS for the publish?", new int[]{0,1,2}));
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

    protected void queue(String topicName, String payload, int QoS)
            throws IOException, MqttsnException {

        message("Enqueued publish to all subscribed sessions: " + topicName);
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        gatewayRuntimeRegistry.getGatewaySessionService().receiveToSessions(topicName, payload.getBytes(StandardCharsets.UTF_8), QoS);
    }

    protected void network() throws IOException, MqttsnException, NetworkRegistryException {
        message("Network registry: ");
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        Iterator<INetworkContext> itr = gatewayRuntimeRegistry.getNetworkRegistry().iterator();
        while(itr.hasNext()){
            INetworkContext c = itr.next();
            message("\t" + c + " -> " + gatewayRuntimeRegistry.getNetworkRegistry().hasBoundSessionContext(c));
        }
    }

    protected void session(String clientId)
            throws IOException, MqttsnException {
        MqttsnGatewayOptions opts = (MqttsnGatewayOptions) getOptions();
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        Optional<IMqttsnContext> context =
                gatewayRuntimeRegistry.getGatewaySessionService().lookupClientIdSession(clientId);
        if(context.isPresent()){
            IMqttsnContext c = context.get();
            IMqttsnSessionState state = gatewayRuntimeRegistry.
                    getGatewaySessionService().getSessionState(c, false);

            message("Client session: " + clientId);
            message("Session started: " + format(state.getSessionStarted()));
            message("Last seen: " + format(state.getLastSeen()));
            message("Keep alive (seconds): " + state.getKeepAlive());
            message("Session length (seconds): " + ((System.currentTimeMillis() - state.getSessionStarted().getTime()) / 1000));
            message("State: " + getColorForState(state.getClientState()) + state.getClientState().name());
            message("Queue size: " + gatewayRuntimeRegistry.getMessageQueue().size(c));

            Set<Subscription> subs = gatewayRuntimeRegistry.getSubscriptionRegistry().readSubscriptions(c);
            Iterator<Subscription> itr = subs.iterator();
            message("Subscription(s): ");
            synchronized (subs){
                while(itr.hasNext()){
                    Subscription s = itr.next();
                    message("\t" + s.getTopicPath() + " -> " + s.getQoS());
                }
            }

            INetworkContext networkContext = gatewayRuntimeRegistry.getNetworkRegistry().getContext(c);
            message("Network Address(s): " + networkContext.getNetworkAddress());

        } else {
            message("No session found: " + clientId);
        }
    }

    protected void status()
            throws IOException, MqttsnException {
        MqttsnGatewayOptions opts = (MqttsnGatewayOptions) getOptions();
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = (MqttsnGatewayRuntimeRegistry) getRuntimeRegistry();
        if(runtime != null) {

            boolean connected = gatewayRuntimeRegistry.getBrokerService().isConnected(null);

            int maxClients = opts.getMaxConnectedClients();
            int advertiseTime = opts.getGatewayAdvertiseTime();

            //-- general stuff
            message("Gateway Id: " + opts.getGatewayId());
            message("Advertise Interval: " + advertiseTime);

            if(gatewayRuntimeRegistry.getTransport() instanceof AbstractMqttsnUdpTransport){
                MqttsnUdpOptions udpOptions = ((AbstractMqttsnUdpTransport)gatewayRuntimeRegistry.getTransport()).getUdpOptions();
                message("Host: " + udpOptions.getHost());
                message("Datagram port: " + udpOptions.getPort());
                message("Secure port: " + udpOptions.getSecurePort());
                message("Broadcast port: " + udpOptions.getBroadcastPort());
                message("MTU: " + udpOptions.getMtu());
            }

            message("Max message size: " + getOptions().getMaxProtocolMessageSize());
            message("Max connected clients: " + maxClients);

            if (getOptions() != null) {
                Map<String, Integer> pTopics = getOptions().getPredefinedTopics();
                if(pTopics != null){
                    message( "Predefined topic count: " + pTopics.size());
                    Iterator<String> itr = pTopics.keySet().iterator();
                    while(itr.hasNext()){
                        String topic = itr.next();
                        message( "\t" + topic + " = " + pTopics.get(topic));
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

            message("Currently connected clients: " + allState.size());
            message("Current buffered messages: " + queuedMessages);

            //-- broker stuff
//            message("Broker hostname: " + hostName);
//            message("Broker port: " + port);
            message("Broker TCP/IP Connection State: " + (connected ? cli_green() + "ESTABLISHED" : cli_red() + "UNESTABLISHED"));

        } else {
            message( "Gateway status: awaiting connection..");
        }
    }

    @Override
    protected MqttsnOptions createOptions() throws UnknownHostException {
        return new MqttsnGatewayOptions().
                withGatewayId(101).
                withContextId(clientId).
                withMaxMessagesInQueue(100).
                withMinFlushTime(400).
                withSleepClearsRegistrations(false);
    }

    @Override
    protected IMqttsnTransport createTransport() {
        MqttsnUdpOptions udpOptions = new MqttsnUdpOptions().
                withPort(MqttsnUdpOptions.DEFAULT_LOCAL_PORT);
        return new MqttsnUdpTransport(udpOptions);
    }

    @Override
    protected AbstractMqttsnRuntime createRuntime(AbstractMqttsnRuntimeRegistry registry, MqttsnOptions options) {
        MqttsnGateway gateway = new MqttsnGateway();
        return gateway;
    }

    @Override
    public void start() throws Exception {
        super.start();
        getRuntime().start(getRuntimeRegistry(), false);
    }

    @Override
    protected void configure() throws IOException {
        super.configure();
        username = captureMandatoryString(input, output, "Please enter a valid username for you broker connection");
        password = captureMandatoryString(input, output,  "Please enter a valid password for you broker connection");
    }

    @Override
    protected void loadConfigHistory(Properties props) throws IOException {
        super.loadConfigHistory(props);
        username = props.getProperty(USERNAME);
        password = props.getProperty(PASSWORD);
    }

    @Override
    protected void saveConfigHistory(Properties props) {
        super.saveConfigHistory(props);
        props.setProperty(USERNAME, username);
        props.setProperty(PASSWORD, password);
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
        if(state == null) return cli_reset();
        switch(state){
            case AWAKE:
            case CONNECTED: return cli_green();
            case ASLEEP: return cli_blue();
            case PENDING: return cli_reset();
            default: return cli_red();
        }
    }
}
