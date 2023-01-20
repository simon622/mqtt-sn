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
import org.slj.mqtt.sn.gateway.spi.GatewayConfig;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorException;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntime;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.impl.ram.MqttsnInMemoryMessageStateService;
import org.slj.mqtt.sn.model.*;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.net.MqttsnUdpBatchTransport;
import org.slj.mqtt.sn.net.MqttsnUdpOptions;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.StringTable;
import org.slj.mqtt.sn.utils.StringTableWriters;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class MqttsnInteractiveGateway extends AbstractInteractiveCli {
    protected boolean needsBroker;

    enum COMMANDS {
        LOOP("Create <count> messages in a loop", new String[]{"int count", "String* topicName", "String* data", "int QoS"}),
        POKE("Poke the queue", new String[0], true),
        INFLIGHT("List inflight messages", new String[0], false),
        REINIT("Re-init the backend broker connection", new String[0]),
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
        GC("Run the garbage collector", new String[0]),
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
                case GC:
                    gc();
                    break;
                case SESSION:
                    session(captureMandatoryString(input, output, "Please supply the clientId whose session you would like to see"));
                    break;
                case SESSIONS:
                    sessions();
                    break;
                case LOOP:
                    loop(
                            captureMandatoryInt(input, output, "How many messages would you like to send?", null),
                            captureMandatoryString(input, output, "Which topic would you like to queue to?"),
                            captureMandatoryInt(input, output, "At which QoS would you like to publish (-1,0,1,2)?", ALLOWED_QOS));
                    break;
                case NETWORK:
                    network();
                    break;
                case STATS:
                    stats();
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

    protected void loop(int count, String topicPath, int qos)
            throws IOException, MqttsnException {
        for (int i = 0; i < count; i++){
            queue(topicPath, "message " + (i + 1), false, qos);
            try {
                //the queue ordering is done using a natural order on
                //timestamp so ensure we are always 1 ms between
                Thread.sleep(1);
            } catch(Exception e){}
        }
    }

    protected void poke() throws MqttsnConnectorException {
        getRuntimeRegistry().getBackendService().pokeQueue();
    }

    protected void reinit() throws MqttsnConnectorException {
        getRuntimeRegistry().getBackendService().reinit();
    }

    protected void queue(String topicName, String payload, boolean retained, int qos)
            throws MqttsnException {

        message("Enqueued publish to all subscribed sessions: " + topicName);
        getRuntimeRegistry().getExpansionHandler().receiveToSessions(topicName, qos, retained, payload.getBytes(StandardCharsets.UTF_8));
    }

    protected void inflight(){
        MqttsnGatewayRuntimeRegistry gatewayRuntimeRegistry = getRuntimeRegistry();
        List<IClientIdentifierContext> m = ((MqttsnInMemoryMessageStateService)gatewayRuntimeRegistry.getMessageStateService()).getActiveInflights();
        Iterator<IClientIdentifierContext> itr = m.iterator();
        while (itr.hasNext()){
            IClientIdentifierContext c = itr.next();
            renderInflight(c,
                ((MqttsnInMemoryMessageStateService)gatewayRuntimeRegistry.getMessageStateService()).getInflightMessages(c, IMqttsnOriginatingMessageSource.LOCAL));
            renderInflight(c,
                    ((MqttsnInMemoryMessageStateService)gatewayRuntimeRegistry.getMessageStateService()).getInflightMessages(c, IMqttsnOriginatingMessageSource.REMOTE));
        }
    }

    private void renderInflight(IClientIdentifierContext context, Map<Integer, InflightMessage> msgs){
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

        Optional<IClientIdentifierContext> context =
                getRuntimeRegistry().getSessionRegistry().lookupClientIdSession(clientId);
        if(context.isPresent()) {
            IClientIdentifierContext c = context.get();
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
        MqttsnGatewayOptions opts = (MqttsnGatewayOptions) runtimeRegistry.getOptions();
        if(runtime != null) {
            boolean connected = getRuntimeRegistry().getBackendService().isConnected(null);
            int maxClients = opts.getMaxClientSessions();
            int advertiseTime = opts.getGatewayAdvertiseTime();

            //-- general stuff
            message(String.format("Gateway Id: %s", opts.getGatewayId()));
            message(String.format("Advertise Interval: %s", advertiseTime));
            message(String.format("Max connected clients: %s", maxClients));
            message(String.format("Protocol Version: %s", getRuntimeRegistry().getCodec().getProtocolVersion()));
            message(String.format("Max message size: %s", getRuntimeRegistry().getOptions().getMaxProtocolMessageSize()));
            message(String.format("Message registry size: %s", getRuntimeRegistry().getMessageRegistry().size()));

            List<ITransport> ts = getRuntimeRegistry().getTransports();
            for (ITransport t : ts){
                StringTable st = t.getTransportDetails();
                tabmessage(StringTableWriters.writeStringTableAsASCII(st));
            }


            if (runtimeRegistry.getOptions() != null) {
                Map<String, Integer> pTopics = runtimeRegistry.getOptions().getPredefinedTopics();
                if(pTopics != null){
                    message(String.format("Predefined topic count: %s", pTopics.size()));
                    Iterator<String> itr = pTopics.keySet().iterator();
                    while(itr.hasNext()){
                        String topic = itr.next();
                        tabmessage(String.format("%s = %s", topic, pTopics.get(topic)));
                    }
                }
            }

            Iterator<ISession> sessionItr = getRuntimeRegistry().getSessionRegistry().iterator();
            List<ISession> allState = new ArrayList<>();
            int queuedMessages = 0;
            while(sessionItr.hasNext()){
                ISession session = sessionItr.next();
                allState.add(session);
                queuedMessages += getRuntimeRegistry().getMessageQueue().queueSize(session);
            }

            message(String.format("Network registry count: %s", getRuntimeRegistry().getNetworkRegistry().size()));
            message(String.format("Current active/awake sessions: %s", allState.stream().filter(s -> MqttsnUtils.in(s.getClientState(), ClientState.ACTIVE, ClientState.AWAKE)).count()));
            message(String.format("Current sleeping sessions: %s", allState.stream().filter(s -> s.getClientState() == ClientState.ASLEEP).count()));
            message(String.format("Current disconnected sessions: %s", allState.stream().filter(s -> s.getClientState() == ClientState.DISCONNECTED).count()));
            message(String.format("Current lost sessions: %s", allState.stream().filter(s -> s.getClientState() == ClientState.LOST).count()));
            message(String.format("All queued session messages: %s", queuedMessages));

            //-- broker stuff
            message(String.format("Broker TCP/IP Connection State: %s", (connected ? cli_green("ESTABLISHED") : cli_red("UNESTABLISHED"))));

        } else {
            message( "Gateway status: awaiting connection..");
        }
    }

    @Override
    protected MqttsnOptions createOptions(IMqttsnStorageService storageService) {
        MqttsnGatewayOptions options = new MqttsnGatewayOptions();
        options.withMaxClientSessions(100).
                withGatewayId(101).
                withContextId(storageService.getStringPreference(GatewayConfig.CLIENTID, null)).
                withSessionExpiryInterval(60 * 60).
                withMinFlushTime(5);
        return options;
    }


    @Override
    protected IMqttsnTransport createTransport(IMqttsnStorageService storageService) {
        MqttsnUdpOptions udpOptions = new MqttsnUdpOptions().
                withPort(storageService.getIntegerPreference(GatewayConfig.LISTEN_PORT, null));
        return new MqttsnUdpBatchTransport(udpOptions, 2048);
    }

    @Override
    protected AbstractMqttsnRuntime createRuntime(AbstractMqttsnRuntimeRegistry registry, MqttsnOptions options) {
        MqttsnGateway gateway = new MqttsnGateway();
        return gateway;
    }

    @Override
    public void start(IMqttsnStorageService storageService) throws Exception {
        super.start(storageService);
        try {
            getRuntime().start(getRuntimeRegistry(), false);
            if(needsBroker){
                message("Successfully connected to broker, TCP/IP connection active.");
            }

            message(String.format("Gateway listening for datagram traffic on port %s",
                    getRuntimeRegistry().getDefaultTransport().getPort()));
        } catch(Exception e){
            message(cli_red("Unable to connect to broker"));
            message("Please check the connection details supplied");
            throw e;
        }
    }

    @Override
    protected void captureSettings() throws MqttsnException {
        super.captureSettings();
        getStorageService().setIntegerPreference(GatewayConfig.LISTEN_PORT,
                captureMandatoryInt(input, output, "Please enter the local listen port", null));
        if(needsBroker){
            //-- ensuring we're setting the connector stuff in the correct namespace
            getStorageService().
                    setStringPreference(GatewayConfig.USERNAME,
                    captureString(input, output, "(B) Please enter a valid username for your connector"));
            getStorageService().
                    setStringPreference(GatewayConfig.PASSWORD,
                    captureString(input, output,  "(B) Please enter a valid password for your connector"));
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
