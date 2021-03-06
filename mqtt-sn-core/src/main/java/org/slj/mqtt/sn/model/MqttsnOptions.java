/*
 * Copyright (c) 2020 Simon Johnson <simon622 AT gmail DOT com>
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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.slj.mqtt.sn.model;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;
import org.slj.mqtt.sn.utils.MqttsnUtils;
import org.slj.mqtt.sn.utils.TopicPath;

import java.util.HashMap;
import java.util.Map;

/**
 * The options class allows you to control aspects of the MQTT-SN engines lifecycle and functionality. The options
 * are generally applicable to both client and gateway runtimes, in limited cases, an option may only apply
 * to either client OR gateway.
 *
 * Default values have been specified to be sensible for use in most cases. It is advised that defaults are only
 * changed when you have a solid understanding of what you are changing, the imnpacts the changes could have.
 */
public class MqttsnOptions {

    /**
     * By default, contexts active message timeout will not be monitored
     */
    public static final int DEFAULT_ACTIVE_CONTEXT_TIMEOUT = 0;

    /**
     * By default, discover is NOT enabled on either the client or the gateway.
     */
    public static final boolean DEFAULT_WIRE_LOGGING_ENABLED = false;

    /**
     * By default, discover is NOT enabled on either the client or the gateway.
     */
    public static final boolean DEFAULT_DISCOVERY_ENABLED = false;

    /**
     * By default, thread hand off is enabled on the gateway, and disabled on the client
     */
    public static final boolean DEFAULT_THREAD_HANDOFF_ENABLED = true;

    /**
     * When thread hand off is enabled, the default number of processing threads is 1
     */
    public static final int DEFAULT_HANDOFF_THREAD_COUNT = 1;

    /**
     * By default, 128 topics can reside in any 1 client registry
     */
    public static final int DEFAULT_MAX_TOPICS_IN_REGISTRY = 128;

    /**
     * By default, message IDs will start at 1
     */
    public static final int DEFAULT_MSG_ID_STARTS_AT = 1;

    /**
     * By default, assigned aliases, handed out by the gateway will start at 1
     */
    public static final int DEFAULT_ALIAS_STARTS_AT = 1;

    /**
     * By default, in either direction a client may have 1 message inflight (this is imposed by the specification).
     */
    public static final int DEFAULT_MAX_MESSAGES_IN_FLIGHT = 1;

    /**
     * By default, 100 publish messages can reside in a client message queue
     */
    public static final int DEFAULT_MAX_MESSAGE_IN_QUEUE = 100;

    /**
     * By default, timedout messages will be requeued
     */
    public static final boolean DEFAULT_REQUEUE_ON_INFLIGHT_TIMEOUT = true;

    /**
     * By default, the ASLEEP state will assume topic registrations will need to be reestablished
     */
    public static final boolean DEFAULT_SLEEP_CLEARS_REGISTRATIONS = true;

    /**
     * The maximum size of a supplied topic is 1024 characters
     */
    public static final int DEFAULT_MAX_TOPIC_LENGTH = 1024;

    /**
     * The maximum number of entries in a network registry is 1024
     */
    public static final int DEFAULT_MAX_NETWORK_ADDRESS_ENTRIES = 1024;

    /**
     * By default, the max wait time for an acknowledgement is 10000 milliseconds
     */
    public static final int DEFAULT_MAX_WAIT = 10000;

    /**
     * By default, the max time a PUBLISH message will remain in flight is 30000 milliseconds
     */
    public static final int DEFAULT_MAX_TIME_INFLIGHT = 30000;

    /**
     * By default, the time to wait between activity (receiving and sending) is 1000 milliseconds
     */
    public static final int DEFAULT_MIN_FLUSH_TIME = 1000;

    /**
     * By default, the discovery search radius is 2 hops
     */
    public static final int DEFAULT_SEARCH_GATEWAY_RADIUS = 2;

    /**
     * By default, the time in seconds a client waits for a discovered gateway
     */
    public static final int DEFAULT_DISCOVERY_TIME_SECONDS = 60 * 60;

    /**
     * By default, the divisor is 4
     */
    public static final int DEFAULT_PING_DIVISOR = 4;

    /**
     * By default instrumentation is switched off
     */
    public static final boolean DEFAULT_INSTRUMENTATION_ENABLED = false;

    /**
     * The default instrumentation interval is 60000 (60 seconds)
     */
    public static final int DEFAULT_INSTRUMENTATION_INTERVAL = 60000;

    /**
     * The default max protocol message size (including header and data) is 1024 bytes
     */
    public static final int DEFAULT_MAX_PROTOCOL_SIZE = 1024;

    private String contextId;
    private boolean threadHandoffFromTransport = DEFAULT_THREAD_HANDOFF_ENABLED;
    private boolean enableDiscovery = DEFAULT_DISCOVERY_ENABLED;
    private boolean sleepClearsRegistrations = DEFAULT_SLEEP_CLEARS_REGISTRATIONS;
    private int handoffThreadCount = DEFAULT_HANDOFF_THREAD_COUNT;
    private int minFlushTime = DEFAULT_MIN_FLUSH_TIME;
    private int maxTopicsInRegistry = DEFAULT_MAX_TOPICS_IN_REGISTRY;
    private int msgIdStartAt = DEFAULT_MSG_ID_STARTS_AT;
    private int aliasStartAt = DEFAULT_ALIAS_STARTS_AT;
    private int maxMessagesInflight = DEFAULT_MAX_MESSAGES_IN_FLIGHT;
    private int maxMessagesInQueue = DEFAULT_MAX_MESSAGE_IN_QUEUE;
    private boolean requeueOnInflightTimeout = DEFAULT_REQUEUE_ON_INFLIGHT_TIMEOUT;
    private int maxTopicLength = DEFAULT_MAX_TOPIC_LENGTH;
    private int maxNetworkAddressEntries = DEFAULT_MAX_NETWORK_ADDRESS_ENTRIES;
    private int maxWait = DEFAULT_MAX_WAIT;
    private int maxTimeInflight = DEFAULT_MAX_TIME_INFLIGHT;
    private int searchGatewayRadius = DEFAULT_SEARCH_GATEWAY_RADIUS;
    private int discoveryTime = DEFAULT_DISCOVERY_TIME_SECONDS;
    private int pingDivisor = DEFAULT_PING_DIVISOR;
    private boolean instrumentationEnabled = DEFAULT_INSTRUMENTATION_ENABLED;
    private int instrumentationInterval = DEFAULT_INSTRUMENTATION_INTERVAL;
    private int maxProtocolMessageSize = DEFAULT_MAX_PROTOCOL_SIZE;
    private boolean wireLoggingEnabled = DEFAULT_WIRE_LOGGING_ENABLED;
    private int activeContextTimeout = DEFAULT_ACTIVE_CONTEXT_TIMEOUT;

    private Map<String, Integer> predefinedTopics;
    private Map<String, NetworkAddress> networkAddressEntries;

    /**
     * When > 0, active context monitoring will notify the application when the context has not generated
     * any active messages (PUBLISH, CONNECT, SUBSCRIBE, UNSUBSCRIBE)
     *
     * @param activeContextTimeout - the time alllowed between last message SENT or RECEIVED from context before
     *                             notification to the connection listener
     *
     * @see {@link MqttsnOptions#DEFAULT_ACTIVE_CONTEXT_TIMEOUT}
     * @return this configuration
     */
    public MqttsnOptions withActiveContextTimeout(int activeContextTimeout){
        this.activeContextTimeout = activeContextTimeout;
        return this;
    }

    /**
     * When enabled, output binary representation of all bytes sent and received
     * from transport
     *
     * @param wireLoggingEnabled - output binary representation of all bytes sent and received from transport
     *
     * @see {@link MqttsnOptions#DEFAULT_WIRE_LOGGING_ENABLED}
     * @return this configuration
     */
    public MqttsnOptions withWireLoggingEnabled(boolean wireLoggingEnabled){
        this.wireLoggingEnabled = wireLoggingEnabled;
        return this;
    }

    /**
     * Should the transport layer hand off messages it receives to a processing thread pool so the protocol loop does
     * not become blocked by longing running operations. NB: it is advised that the transport loop is kept as quick as
     * possible, changing this to false could result is long pauses for concurrent clients.
     *
     * @see {@link MqttsnOptions#DEFAULT_THREAD_HANDOFF_ENABLED}
     *
     * @param threadHandoffFromTransport - Should the transport layer hand off messages it receives to a processing thread pool so the protocol loop does
     * not become blocked by longing running operations.
     * @return this configuration
     */
    public MqttsnOptions withThreadHandoffFromTransport(boolean threadHandoffFromTransport){
        this.threadHandoffFromTransport = threadHandoffFromTransport;
        return this;
    }

    /**
     * When threadHandoffFromTransport is set to true, how many threads should be made available in the
     * managed pool to handle processing.
     *
     * @see {@link MqttsnOptions#DEFAULT_HANDOFF_THREAD_COUNT}
     *
     * @param handoffThreadCount - When threadHandoffFromTransport is set to true, how many threads should be made available in the
     * managed pool to handle processing
     * @return this configuration
     */
    public MqttsnOptions withHandoffThreadCount(int handoffThreadCount){
        this.handoffThreadCount = handoffThreadCount;
        return this;
    }

    /**
     * The idle time between receiving a message and starting a new publish operation (where number of messages on a client queue > 0)
     *
     * @see {@link MqttsnOptions#DEFAULT_MIN_FLUSH_TIME}
     *
     * @param minFlushTime - The idle time between receiving a message and starting a new publish operation (where number of messages on a client queue > 0)
     * @return this configuration
     */
    public MqttsnOptions withMinFlushTime(int minFlushTime){
        this.minFlushTime = minFlushTime;
        return this;
    }

    /**
     * When a client enters the ASLEEP state, should the NORMAL topic registered alias's be cleared down and reestablished during the
     * next AWAKE | ACTIVE states. Setting this to false, will mean the gateway will resend REGISTER messages during an AWAKE ping.
     *
     * @see {@link MqttsnOptions#DEFAULT_SLEEP_CLEARS_REGISTRATIONS}
     *
     * @param sleepClearsRegistrations - When a client enters the ASLEEP state, should the NORMAL topic registered alias's be cleared down and reestablished during the next AWAKE | ACTIVE states.
     * @return this configuration
     */
    public MqttsnOptions withSleepClearsRegistrations(boolean sleepClearsRegistrations){
        this.sleepClearsRegistrations = sleepClearsRegistrations;
        return this;
    }

    /**
     * Number of hops to allow broadcast messages
     *
     * @see {@link MqttsnOptions#DEFAULT_SEARCH_GATEWAY_RADIUS}
     *
     * @param searchGatewayRadius - Number of hops to allow broadcast messages
     * @return this configuration
     */
    public MqttsnOptions withSearchGatewayRadius(int searchGatewayRadius){
        this.searchGatewayRadius = searchGatewayRadius;
        return this;
    }

    /**
     * How many messages should be allowed in a client's queue (either to send or buffer from the gateway).
     *
     * @see {@link MqttsnOptions#DEFAULT_MAX_MESSAGE_IN_QUEUE}
     *
     * @param maxMessagesInQueue - How many messages should be allowed in a client's queue.
     * @return this configuration
     */
    public MqttsnOptions withMaxMessagesInQueue(int maxMessagesInQueue){
        this.maxMessagesInQueue = maxMessagesInQueue;
        return this;
    }

    /**
     * When a PUBLISH QoS 1,2 message has been in an unconfirmed state for a period of time,
     * should it be requeued for a second DUP sending attempt or discarded.
     *
     * @see {@link MqttsnOptions#DEFAULT_REQUEUE_ON_INFLIGHT_TIMEOUT}
     *
     * @param requeueOnInflightTimeout - When a PUBLISH QoS 1,2 message has been in an unconfirmed state for a period of time,
     * should it be requeued for a second DUP sending attempt or discarded
     * @return this configuration
     */
    public MqttsnOptions withRequeueOnInflightTimeout(boolean requeueOnInflightTimeout){
        this.requeueOnInflightTimeout = requeueOnInflightTimeout;
        return this;
    }

    /**
     * Time in millis a PUBLISH message will reside in the INFLIGHT (unconfirmed) state before it is considered DUP (errord).
     *
     * @see {@link MqttsnOptions#DEFAULT_MAX_TIME_INFLIGHT}
     *
     * @param maxTimeInflight - Time in millis a PUBLISH message will reside in the INFLIGHT (unconfirmed) state before it is considered DUP (errord).
     * @return this configuration
     */
    public MqttsnOptions withMaxTimeInflight(int maxTimeInflight){
        this.maxTimeInflight = maxTimeInflight;
        return this;
    }

    /**
     * Maximum number of messages allowed INFLIGHT at any given point in time. NB: the specification allows for a single message in flight in either direction.
     * WARNING: changing this default value could lead to unpredictable behaviour depending on the gateway capability.
     *
     * @see {@link MqttsnOptions#DEFAULT_MAX_MESSAGES_IN_FLIGHT}
     *
     * @param maxMessagesInflight - Maximum number of messages allowed INFLIGHT at any given point in time. NB: the specification allows for a single message in flight in either direction.
     * @return this configuration
     */
    public MqttsnOptions withMaxMessagesInflight(int maxMessagesInflight){
        this.maxMessagesInflight = maxMessagesInflight;
        return this;
    }

    /**
     * The maximum time (in millis) that an acknowledged message will wait to be considered successfully confirmed
     * by the gateway.
     *
     * @see {@link MqttsnOptions#DEFAULT_MAX_WAIT}
     *
     * @param maxWait - Time in millis acknowledged message will wait before an error is thrown
     * @return this configuration
     */
    public MqttsnOptions withMaxWait(int maxWait){
        this.maxWait = maxWait;
        return this;
    }

    /**
     * Add a predefined topic alias to the registry to be used in all interactions.
     * NB: these should be known by both the client and the gateway to enable successful use of
     * the PREDEFINED alias types.
     *
     * @param topicPath - The topic path to register e.g. "foo/bar"
     * @param alias - The alias of the topic path to match
     * @return this configuration
     */
    public MqttsnOptions withPredefinedTopic(String topicPath, int alias){
        if(!TopicPath.isValidTopic(topicPath, Math.max(maxTopicLength, MqttsnConstants.USIGNED_MAX_16))){
            throw new MqttsnRuntimeException("invalid topic path " + topicPath);
        }

        if(!MqttsnUtils.validUInt16(alias)){
            throw new MqttsnRuntimeException("invalid topic alias " + alias);
        }

        if(predefinedTopics == null){
            synchronized (this) {
                if (predefinedTopics == null) {
                    predefinedTopics = new HashMap();
                }
            }
        }
        predefinedTopics.put(topicPath, alias);
        return this;
    }

    /**
     * The maximum length of a topic allowed, including all wildcard or separator characters.
     *
     * @see {@link MqttsnOptions#DEFAULT_MAX_TOPIC_LENGTH}
     *
     * @param maxTopicLength - The maximum length of a topic allowed, including all wildcard or separator characters.
     * @return this configuration
     */
    public MqttsnOptions withMaxTopicLength(int maxTopicLength){
        this.maxTopicLength = maxTopicLength;
        return this;
    }

    /**
     * The number at which messageIds start, typically this should be 1.
     *
     * @see {@link MqttsnOptions#DEFAULT_MSG_ID_STARTS_AT}
     *
     * @param msgIdStartAt - The number at which messageIds start, typically this should be 1.
     * @return this configuration
     */
    public MqttsnOptions withMsgIdsStartAt(int msgIdStartAt){
        this.msgIdStartAt = msgIdStartAt;
        return this;
    }

    /**
     * The maximum number of NORMAL topics allowed in the topic registry.
     * NB: Realistically an application should not need many hundreds of topics in their hierarchy
     *
     * @see {@link MqttsnOptions#DEFAULT_MAX_TOPICS_IN_REGISTRY}
     *
     * @param maxTopicsInRegistry - The maximum number of NORMAL topics allowed in the topic registry.
     * @return this configuration
     */
    public MqttsnOptions withMaxTopicsInRegistry(int maxTopicsInRegistry){
        this.maxTopicsInRegistry = maxTopicsInRegistry;
        return this;
    }

    /**
     * Should discovery be enabled. When enabled the transport layer will run its broadcast threads and
     * allow dynamic gateway / client binding.
     *
     * @see {@link MqttsnOptions#DEFAULT_DISCOVERY_ENABLED}
     *
     * @param enableDiscovery - Should discovery be enabled.
     * @return this configuration
     */
    public MqttsnOptions withDiscoveryEnabled(boolean enableDiscovery){
        this.enableDiscovery = enableDiscovery;
        return this;
    }

    /**
     * The maximum number of addresses allowed in the network registry. An address is a network location mapped
     * to a clientId
     *
     * @see {@link MqttsnOptions#DEFAULT_MAX_NETWORK_ADDRESS_ENTRIES}
     *
     * @param maxNetworkAddressEntries - The maximum number of addresses allowed in the network registry
     * @return this configuration
     */
    public MqttsnOptions withMaxNetworkAddressEntries(int maxNetworkAddressEntries){
        this.maxNetworkAddressEntries = maxNetworkAddressEntries;
        return this;
    }

    /**
     * The starting value of assigned NORMAL topic aliases that the gateway hands out.
     *
     * @see {@link MqttsnOptions#DEFAULT_ALIAS_STARTS_AT}
     *
     * @param aliasStartAt - The starting value of assigned NORMAL topic aliases that the gateway hands out.
     * @return this configuration
     */
    public MqttsnOptions withAliasStartAt(int aliasStartAt){
        this.aliasStartAt = aliasStartAt;
        return this;
    }

    /**
     * The contextId is a general term for EITHER the clientId when running as a client or the gatewayId
     * when running as a gateway. It should conform to the specification. It is advised that it contains between
     * 1-23 alpha numeric characters from the ASCII character set.
     *
     * NB: When running in gateway mode, this is a mandatory item that should be set by the application.
     *
     * @param contextId - The contextId is a general term for EITHER the clientId when running as a client or the gatewayId
     * when running as a gateway.
     * @return this configuration
     */
    public MqttsnOptions withContextId(String contextId){
        this.contextId = contextId;
        return this;
    }

    /**
     * The time (in seconds) a client will wait for a broadcast during CONNECT before giving up
     *
     * NB: only applicable to client
     *
     * @see {@link MqttsnOptions#DEFAULT_DISCOVERY_TIME_SECONDS}
     *
     * @param discoveryTime - Time (in seconds) a client will wait for a broadcast during CONNECT before giving up
     * when running as a client.
     * @return this configuration
     */
    public MqttsnOptions withDiscoveryTime(int discoveryTime){
        this.discoveryTime = discoveryTime;
        return this;
    }

    /**
     * The divisor to use for the ping window, the dividend being the CONNECT keepAlive resulting
     * in the quotient which is the time (since last sent message) each ping will be issued
     *
     * For example a 60 seconds session with a divisor of 4 will yield 15 second pings between
     * activity
     *
     * NB: only applicable to client
     *
     * @see {@link MqttsnOptions#DEFAULT_PING_DIVISOR}
     *
     * @param pingDivisor - The divisor to use for the ping window
     * @return this configuration
     */
    public MqttsnOptions withPingDivisor(int pingDivisor){
        this.pingDivisor = pingDivisor;
        return this;
    }

    /**
     * Should instrumentation be enabled. When enabled the runtime will call registered instrumentation
     * providers on the {@link MqttsnOptions#getInstrumentationInterval} period and output the data
     * to the standard logging.
     *
     * @param instrumentationEnabled - Should instrumentation be enabled
     * @return this configuration
     */
    public MqttsnOptions withInstrumentationEnabled(boolean instrumentationEnabled){
        this.instrumentationEnabled = instrumentationEnabled;
        return this;
    }

    /**
     * The interval between instrumentation sampling when it is enabled.
     *
     * @param instrumentationInterval The interval between instrumentation sampling when it is enabled.
     * @return this configuration
     */
    public MqttsnOptions withInstrumentationInterval(int instrumentationInterval){
        this.instrumentationInterval = instrumentationInterval;
        return this;
    }

    /**
     * The max allowable size of protocol messages that will be sent or received by the system.
     * NOTE: this differs from transport level max sizes which will be deterimed and constrained by the
     * MTU of the transport
     *
     * @param maxProtocolMessageSize - The max allowable size of protocol messages.
     * @return this configuration
     */
    public MqttsnOptions withMaxProtocolMessageSize(int maxProtocolMessageSize){
        this.maxProtocolMessageSize = maxProtocolMessageSize;
        return this;
    }

    /**
     * Sets the locations of known clients or gateways on the network. When running as a client and discovery is not enabled,
     * it is mandatory that at least 1 gateway entry be supplied, which will be the gateway the client talks to. In gateway
     * mode, the registry is populated dynamically.
     * @param contextId - the contextId of the known remote location
     * @param address - the network address of the known remote location
     * @return this config
     */
    public MqttsnOptions withNetworkAddressEntry(String contextId, NetworkAddress address){
        if(networkAddressEntries == null){
            synchronized (this) {
                if (networkAddressEntries == null) {
                    networkAddressEntries = new HashMap();
                }
            }
        }
        networkAddressEntries.put(contextId, address);
        return this;
    }

    public Map<String, NetworkAddress> getNetworkAddressEntries() {
        return networkAddressEntries;
    }

    public int getAliasStartAt() {
        return aliasStartAt;
    }

    public int getMsgIdStartAt() {
        return msgIdStartAt;
    }

    public int getMaxTopicsInRegistry() {
        return maxTopicsInRegistry;
    }

    public boolean isEnableDiscovery() {
        return enableDiscovery;
    }

    public int getMaxTopicLength() {
        return maxTopicLength;
    }

    public String getContextId() {
        return contextId;
    }

    public int getMaxTimeInflight() {
        return maxTimeInflight;
    }

    public int getMaxNetworkAddressEntries() {
        return maxNetworkAddressEntries;
    }

    public Map<String, Integer> getPredefinedTopics() {
        return predefinedTopics;
    }

    public int getMaxMessagesInflight() {
        return maxMessagesInflight;
    }

    public int getMaxMessagesInQueue() {
        return maxMessagesInQueue;
    }

    public int getMaxWait() {
        return maxWait;
    }

    public int getHandoffThreadCount() {
        return handoffThreadCount;
    }

    public int getSearchGatewayRadius() {
        return searchGatewayRadius;
    }

    public int getMinFlushTime() {
        return minFlushTime;
    }

    public boolean isSleepClearsRegistrations() {
        return sleepClearsRegistrations;
    }

    public int getDiscoveryTime() {
        return discoveryTime;
    }

    public int getPingDivisor() {
        return pingDivisor;
    }

    public boolean isInstrumentationEnabled() {
        return instrumentationEnabled;
    }

    public int getInstrumentationInterval() {
        return instrumentationInterval;
    }

    public boolean isThreadHandoffFromTransport() {
        return threadHandoffFromTransport;
    }

    public boolean isRequeueOnInflightTimeout() {
        return requeueOnInflightTimeout;
    }

    public int getMaxProtocolMessageSize() {
        return maxProtocolMessageSize;
    }

    public boolean isWireLoggingEnabled() {
        return wireLoggingEnabled;
    }

    public int getActiveContextTimeout() {
        return activeContextTimeout;
    }
}