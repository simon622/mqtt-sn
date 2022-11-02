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

package org.slj.mqtt.sn.model;

import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.net.NetworkAddress;

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
    public static final String DEFAULT_SIMPLE_LOG_PATTERN = "[%1$tc] %4$s %2$s - %5$s %6$s%n";

    /**
     * By default the  runtime will NOT remove receiving messages from the state service
     */
    public static final boolean DEFAULT_REAP_RECEIVING_MESSAGES = false;

    /**
     * The number of retries that shall be attempted before disconnecting the context
     */
    public static final int DEFAULT_MAX_ERROR_RETRIES = 3;

    /**
     * The interval (in milliseconds) between error retries
     */
    public static final int DEFAULT_MAX_ERROR_RETRY_TIME = 10000;

    /**
     * By default, contexts active message timeout will not be monitored
     */
    public static final int DEFAULT_ACTIVE_CONTEXT_TIMEOUT = 20000;

    /**
     * By default, the maximum time the state loop will sleep (unless notified)
     */
    public static final int DEFAULT_STATE_LOOP_TIMEOUT = 15000;

    /**
     * Wire logging can be stopped entirely
     */
    public static final boolean DEFAULT_WIRE_LOGGING_ENABLED = false;

    /**
     * By default, discovery is NOT enabled on either the client or the gateway.
     */
    public static final boolean DEFAULT_DISCOVERY_ENABLED = false;

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
     * By default, 1000 messages can reside in the DLQ
     */
    public static final int DEFAULT_MAX_MESSAGE_IN_DLQ = 1000;

    /**
     * By default, timedout messages will be requeued
     */
    public static final boolean DEFAULT_REQUEUE_ON_INFLIGHT_TIMEOUT = true;

    /**
     * By default, the ASLEEP state will assume topic registrations will need to be reestablished
     */
    public static final boolean DEFAULT_SLEEP_CLEARS_REGISTRATIONS = true;

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
     * By default, the time to wait between activity (receiving and sending publish messages) is 50 milliseconds
     */
    public static final int DEFAULT_MIN_FLUSH_TIME = 25;

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
     * The default max protocol message size (including header and data) is 1024 bytes
     */
    public static final int DEFAULT_MAX_PROTOCOL_SIZE = 1024;

    /**
     * The default T-WAIT for congestion (in seconds)
     */
    public static final int DEFAULT_CONGESTION_WAIT = 5;

    /**
     * The default time (in seconds) after which a disconnected session becomes eligible
     * for removal
     */
    public static final int DEFAULT_REMOVE_DISCONNECTED_SESSIONS_SECONDS = 60 * 60 * 24 * 7;

    /**
     * How many threads will process general protocol messages (from inbound and outbound lifecycle)
     */
    public static final int DEFAULT_TRANSPORT_PROTOCOL_HANDOFF_THREAD_COUNT = 1;

    /**
     * How many threads will process outbound PUBLISH messages
     */
    public static final int DEFAULT_TRANSPORT_PUBLISH_HANDOFF_THREAD_COUNT = 1;

    /**
     * Used to handle the outbound queue processing layer, when running as a gateway this should
     * scale with the number of expected connected clients
     */
    public static final int DEFAULT_QUEUE_PROCESSOR_THREAD_COUNT = 1;

    /**
     * General purpose thead pool which should not be blocked by traffic
     */
    public static final int DEFAULT_GENERAL_PURPOSE_THREAD_COUNT = 1;

    /**
     * Number of queues jobs allowed as backpressure in the workers
     */
    public static final int DEFAULT_WORK_QUEUE_BACKPRESSURE = 50000;

    /**
     * Whether metrics will be collected at runtime
     */
    public static final boolean DEFAULT_METRICS_ENABLED = true;

    /**
     * Percentage of message queue size that will overflow onto the disk if enabled
     */
    public static final int DEFAULT_MESSAGE_QUEUE_DISK_STORAGE_THRESHOLD = 5;

    private String contextId;
    private int transportProtocolHandoffThreadCount = DEFAULT_TRANSPORT_PROTOCOL_HANDOFF_THREAD_COUNT;
    private int transportPublishHandoffThreadCount = DEFAULT_TRANSPORT_PUBLISH_HANDOFF_THREAD_COUNT;
    private int queueProcessorThreadCount = DEFAULT_QUEUE_PROCESSOR_THREAD_COUNT;
    private int generalPurposeThreadCount = DEFAULT_GENERAL_PURPOSE_THREAD_COUNT;
    private int queueBackPressure = DEFAULT_WORK_QUEUE_BACKPRESSURE;
    private boolean enableDiscovery = DEFAULT_DISCOVERY_ENABLED;
    private boolean sleepClearsRegistrations = DEFAULT_SLEEP_CLEARS_REGISTRATIONS;
    private int minFlushTime = DEFAULT_MIN_FLUSH_TIME;
    private int maxTopicsInRegistry = DEFAULT_MAX_TOPICS_IN_REGISTRY;
    private int msgIdStartAt = DEFAULT_MSG_ID_STARTS_AT;
    private int aliasStartAt = DEFAULT_ALIAS_STARTS_AT;
    private int maxMessagesInflight = DEFAULT_MAX_MESSAGES_IN_FLIGHT;
    private int maxMessagesInQueue = DEFAULT_MAX_MESSAGE_IN_QUEUE;
    private boolean requeueOnInflightTimeout = DEFAULT_REQUEUE_ON_INFLIGHT_TIMEOUT;
    private int maxMessagesInDeadLetterQueue = DEFAULT_MAX_MESSAGE_IN_DLQ;
    private int maxNetworkAddressEntries = DEFAULT_MAX_NETWORK_ADDRESS_ENTRIES;
    private int maxWait = DEFAULT_MAX_WAIT;
    private int maxTimeInflight = DEFAULT_MAX_TIME_INFLIGHT;
    private int searchGatewayRadius = DEFAULT_SEARCH_GATEWAY_RADIUS;
    private int discoveryTime = DEFAULT_DISCOVERY_TIME_SECONDS;
    private int pingDivisor = DEFAULT_PING_DIVISOR;
    private int maxProtocolMessageSize = DEFAULT_MAX_PROTOCOL_SIZE;
    private boolean wireLoggingEnabled = DEFAULT_WIRE_LOGGING_ENABLED;
    private int activeContextTimeout = DEFAULT_ACTIVE_CONTEXT_TIMEOUT;
    private int stateLoopTimeout = DEFAULT_STATE_LOOP_TIMEOUT;
    private String logPattern = DEFAULT_SIMPLE_LOG_PATTERN;
    private int maxErrorRetries = DEFAULT_MAX_ERROR_RETRIES;
    private int maxErrorRetryTime = DEFAULT_MAX_ERROR_RETRY_TIME;
    private int congestionWait = DEFAULT_CONGESTION_WAIT;
    private int removeDisconnectedSessionsSeconds = DEFAULT_REMOVE_DISCONNECTED_SESSIONS_SECONDS;
    private boolean reapReceivingMessages = DEFAULT_REAP_RECEIVING_MESSAGES;
    private boolean metricsEnabled = DEFAULT_METRICS_ENABLED;

    private int messageQueueDiskStorageThreshold = DEFAULT_MESSAGE_QUEUE_DISK_STORAGE_THRESHOLD;
    private MqttsnSecurityOptions securityOptions;
    private Map<String, Integer> predefinedTopics = new HashMap<>();
    private volatile Map<String, NetworkAddress> networkAddressEntries;
    private MqttsnClientCredentials clientCredentials =
            new MqttsnClientCredentials(true);

    /**
     * When enabled, at what percentage of fill do the message queues start
     * to overflow onto disk
     *
     * @param messageQueueDiskStorageThreshold
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_MESSAGE_QUEUE_DISK_STORAGE_THRESHOLD}
     */
    public MqttsnOptions withMessageQueueDiskStorageThreshold(int messageQueueDiskStorageThreshold) {
        this.messageQueueDiskStorageThreshold = messageQueueDiskStorageThreshold;
        return this;
    }

    /**
     * Should the metrics system be enabled
     *
     * @param metricsEnabled
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_METRICS_ENABLED}
     */
    public MqttsnOptions withMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
        return this;
    }

    /**
     * How many seconds after a client is last seen should disconnected sessions be removed from
     * the session state service
     *
     * @param removeDisconnectedSessionsSeconds - Number of threads to use to service outbound queue processing
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_REMOVE_DISCONNECTED_SESSIONS_SECONDS}
     */
    public MqttsnOptions withRemoveDisconnectedSessionsSeconds(int removeDisconnectedSessionsSeconds) {
        this.removeDisconnectedSessionsSeconds = removeDisconnectedSessionsSeconds;
        return this;
    }

    /**
     * This number caps the allowed number of messages in the DLQ. When this number is exceeded, m
     * message will be discarded in a FIFO manner.
     *
     * @param maxMessagesInDeadLetterQueue - Max messages to reside in the dead letter queue
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_MAX_MESSAGE_IN_DLQ}
     */
    public MqttsnOptions withMaxMessageInDeadLetterQueue(int maxMessagesInDeadLetterQueue) {
        this.maxMessagesInDeadLetterQueue = maxMessagesInDeadLetterQueue;
        return this;
    }

    /**
     * Should the state service reap messages being recieved?
     *
     * @param reapReceivingMessages - Reap inbound messages
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_REAP_RECEIVING_MESSAGES}
     */
    public MqttsnOptions withReapReceivingMessages(boolean reapReceivingMessages) {
        this.reapReceivingMessages = reapReceivingMessages;
        return this;
    }

    /**
     * Configure the log pattern on the environment
     *
     * @param logPattern - the log pattern applied to the SIMPLE log formatter
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_SIMPLE_LOG_PATTERN}
     */
    public MqttsnOptions withLogPattern(String logPattern) {
        this.logPattern = logPattern;
        return this;
    }

    /**
     * Configure the behaviour or error retransmissions
     *
     * @param maxErrorRetries - The max number of retries attempted without a valid response before disconnecting the context
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_MAX_ERROR_RETRIES}
     */
    public MqttsnOptions withMaxErrorRetries(int maxErrorRetries) {
        this.maxErrorRetries = maxErrorRetries;
        return this;
    }

    /**
     * Configure the behaviour or error retransmissions
     *
     * @param maxErrorRetryTime - The time between retries when a response is not received
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_MAX_ERROR_RETRY_TIME}
     */
    public MqttsnOptions withMaxErrorRetryTime(int maxErrorRetryTime) {
        this.maxErrorRetryTime = maxErrorRetryTime;
        return this;
    }

    /**
     * When > 0, active context monitoring will notify the application when the context has not generated
     * any active messages (PUBLISH, CONNECT, SUBSCRIBE, UNSUBSCRIBE)
     *
     * @param activeContextTimeout - the time allowed between last message SENT or RECEIVED from context before
     *                             notification to the connection listener
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_ACTIVE_CONTEXT_TIMEOUT}
     */
    public MqttsnOptions withActiveContextTimeout(int activeContextTimeout) {
        this.activeContextTimeout = activeContextTimeout;
        return this;
    }

    /**
     * The max time between invocations of the state loop which monitors activity timeout
     *
     * @param stateLoopTimeout - the max time between invocations of the state loop which monitors activity timeout
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_STATE_LOOP_TIMEOUT}
     */
    public MqttsnOptions withStateLoopTimeout(int stateLoopTimeout) {
        this.stateLoopTimeout = stateLoopTimeout;
        return this;
    }

    /**
     * When enabled, output binary representation of all bytes sent and received
     * from transport
     *
     * @param wireLoggingEnabled - output binary representation of all bytes sent and received from transport
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_WIRE_LOGGING_ENABLED}
     */
    public MqttsnOptions withWireLoggingEnabled(boolean wireLoggingEnabled) {
        this.wireLoggingEnabled = wireLoggingEnabled;
        return this;
    }


    /**
     * How many threads should be used to process connected context message queues
     * (should scale with the number of expected connected clients and the level
     * of concurrency)
     *
     * @param queueProcessorThreadCount - Number of threads to use to service outbound queue processing
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_QUEUE_PROCESSOR_THREAD_COUNT}
     */
    public MqttsnOptions withQueueProcessorThreadCount(int queueProcessorThreadCount) {
        this.queueProcessorThreadCount = queueProcessorThreadCount;
        return this;
    }

    /**
     * When threadHandoffFromTransport is set to true, how many threads should be made available in the
     * managed pool to handle processing.
     *
     * @param transportProtocolHandoffThreadCount - When transportProtocolHandoffThreadCount is set to true, how many threads should be made available in the
     *                                            managed pool to handle processing
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_TRANSPORT_PROTOCOL_HANDOFF_THREAD_COUNT}
     */
    public MqttsnOptions withTransportProtocolHandoffThreadCount(int transportProtocolHandoffThreadCount) {
        this.transportProtocolHandoffThreadCount = transportProtocolHandoffThreadCount;
        return this;
    }

    /**
     * How many threads should be made available in the managed pool to handle egress publish processing.
     *
     * @param transportPublishHandoffThreadCount - How many threads should be made available in the
     *                                        managed pool to handle processing
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_TRANSPORT_PUBLISH_HANDOFF_THREAD_COUNT}
     */
    public MqttsnOptions withTransportPublishHandoffThreadCount(int transportPublishHandoffThreadCount) {
        this.transportPublishHandoffThreadCount = transportPublishHandoffThreadCount;
        return this;
    }

    /**
     * When generalPurposeThreadCount is set to true, how many threads should be made available in the
     * managed pool to handle processing.
     *
     * @param generalPurposeThreadCount - When generalPurposeThreadCount is set to true, how many threads should be made available in the
     *                                  managed pool to handle processing
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_GENERAL_PURPOSE_THREAD_COUNT}
     */
    public MqttsnOptions withGeneralPurposeThreadCount(int generalPurposeThreadCount) {
        this.generalPurposeThreadCount = generalPurposeThreadCount;
        return this;
    }

    /**
     * The max. size of the workers queues
     *
     * @param queueBackPressure - The size of the queues backing the workers
     *                          managed pool to handle processing
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_WORK_QUEUE_BACKPRESSURE}
     */
    public MqttsnOptions withQueueBackPressure(int queueBackPressure) {
        this.queueBackPressure = queueBackPressure;
        return this;
    }

    /**
     * The idle time between receiving a message and starting a new publish operation (where number of messages on a client queue > 0)
     *
     * @param minFlushTime - The idle time between receiving a message and starting a new publish operation (where number of messages on a client queue > 0)
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_MIN_FLUSH_TIME}
     */
    public MqttsnOptions withMinFlushTime(int minFlushTime) {
        this.minFlushTime = minFlushTime;
        return this;
    }

    /**
     * When a client enters the ASLEEP state, should the NORMAL topic registered alias's be cleared down and reestablished during the
     * next AWAKE | ACTIVE states. Setting this to false, will mean the gateway will resend REGISTER messages during an AWAKE ping.
     *
     * @param sleepClearsRegistrations - When a client enters the ASLEEP state, should the NORMAL topic registered alias's be cleared down and reestablished during the next AWAKE | ACTIVE states.
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_SLEEP_CLEARS_REGISTRATIONS}
     */
    public MqttsnOptions withSleepClearsRegistrations(boolean sleepClearsRegistrations) {
        this.sleepClearsRegistrations = sleepClearsRegistrations;
        return this;
    }

    /**
     * Number of hops to allow broadcast messages
     *
     * @param searchGatewayRadius - Number of hops to allow broadcast messages
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_SEARCH_GATEWAY_RADIUS}
     */
    public MqttsnOptions withSearchGatewayRadius(int searchGatewayRadius) {
        this.searchGatewayRadius = searchGatewayRadius;
        return this;
    }

    /**
     * How many messages should be allowed in a client's queue (either to send or buffer from the gateway).
     *
     * @param maxMessagesInQueue - How many messages should be allowed in a client's queue.
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_MAX_MESSAGE_IN_QUEUE}
     */
    public MqttsnOptions withMaxMessagesInQueue(int maxMessagesInQueue) {
        this.maxMessagesInQueue = maxMessagesInQueue;
        return this;
    }

    /**
     * When a PUBLISH QoS 1,2 message has been in an unconfirmed state for a period of time,
     * should it be requeued for a second DUP sending attempt or discarded.
     * <p>
     * NB - The spec says that messages should be resent on next CONNECT clean false, this setting
     * allows the messages to be moved back to the queue immediately
     *
     * @param requeueOnInflightTimeout - When a PUBLISH QoS 1,2 message has been in an unconfirmed state for a period of time,
     *                                 should it be requeued for a second DUP sending attempt or discarded
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_REQUEUE_ON_INFLIGHT_TIMEOUT}
     */
    public MqttsnOptions withRequeueOnInflightTimeout(boolean requeueOnInflightTimeout) {
        this.requeueOnInflightTimeout = requeueOnInflightTimeout;
        return this;
    }

    /**
     * Time in millis a PUBLISH message will reside in the INFLIGHT (unconfirmed) state before it is considered DUP (errord).
     *
     * @param maxTimeInflight - Time in millis a PUBLISH message will reside in the INFLIGHT (unconfirmed) state before it is considered DUP (errord).
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_MAX_TIME_INFLIGHT}
     */
    public MqttsnOptions withMaxTimeInflight(int maxTimeInflight) {
        this.maxTimeInflight = maxTimeInflight;
        return this;
    }

    /**
     * Maximum number of messages allowed INFLIGHT at any given point in time. NB: the specification allows for a single message in flight in either direction.
     * WARNING: changing this default value could lead to unpredictable behaviour depending on the gateway capability.
     *
     * @param maxMessagesInflight - Maximum number of messages allowed INFLIGHT at any given point in time. NB: the specification allows for a single message in flight in either direction.
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_MAX_MESSAGES_IN_FLIGHT}
     */
    public MqttsnOptions withMaxMessagesInflight(int maxMessagesInflight) {
        this.maxMessagesInflight = maxMessagesInflight;
        return this;
    }

    /**
     * The maximum time (in millis) that an acknowledged message will wait to be considered successfully confirmed
     * by the gateway.
     *
     * @param maxWait - Time in millis acknowledged message will wait before an error is thrown
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_MAX_WAIT}
     */
    public MqttsnOptions withMaxWait(int maxWait) {
        this.maxWait = maxWait;
        return this;
    }

    /**
     * Add a predefined topic alias to the registry to be used in all interactions.
     * NB: these should be known by both the client and the gateway to enable successful use of
     * the PREDEFINED alias types.
     *
     * @param topicPath - The topic path to register e.g. "foo/bar"
     * @param alias     - The alias of the topic path to match
     * @return this configuration
     */
    public MqttsnOptions withPredefinedTopic(String topicPath, int alias) {

        MqttsnSpecificationValidator.validatePublishPath(topicPath);
        MqttsnSpecificationValidator.validateTopicAlias(alias);

        predefinedTopics.put(topicPath, alias);
        return this;
    }


    /**
     * The number at which messageIds start, typically this should be 1.
     *
     * @param msgIdStartAt - The number at which messageIds start, typically this should be 1.
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_MSG_ID_STARTS_AT}
     */
    public MqttsnOptions withMsgIdsStartAt(int msgIdStartAt) {
        this.msgIdStartAt = msgIdStartAt;
        return this;
    }

    /**
     * The maximum number of NORMAL topics allowed in the topic registry.
     * NB: Realistically an application should not need many hundreds of topics in their hierarchy
     *
     * @param maxTopicsInRegistry - The maximum number of NORMAL topics allowed in the topic registry.
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_MAX_TOPICS_IN_REGISTRY}
     */
    public MqttsnOptions withMaxTopicsInRegistry(int maxTopicsInRegistry) {
        this.maxTopicsInRegistry = maxTopicsInRegistry;
        return this;
    }

    /**
     * Should discovery be enabled. When enabled the transport layer will run its broadcast threads and
     * allow dynamic gateway / client binding.
     *
     * @param enableDiscovery - Should discovery be enabled.
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_DISCOVERY_ENABLED}
     */
    public MqttsnOptions withDiscoveryEnabled(boolean enableDiscovery) {
        this.enableDiscovery = enableDiscovery;
        return this;
    }

    /**
     * The maximum number of addresses allowed in the network registry. An address is a network location mapped
     * to a clientId
     *
     * @param maxNetworkAddressEntries - The maximum number of addresses allowed in the network registry
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_MAX_NETWORK_ADDRESS_ENTRIES}
     */
    public MqttsnOptions withMaxNetworkAddressEntries(int maxNetworkAddressEntries) {
        this.maxNetworkAddressEntries = maxNetworkAddressEntries;
        return this;
    }

    /**
     * The starting value of assigned NORMAL topic aliases that the gateway hands out.
     *
     * @param aliasStartAt - The starting value of assigned NORMAL topic aliases that the gateway hands out.
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_ALIAS_STARTS_AT}
     */
    public MqttsnOptions withAliasStartAt(int aliasStartAt) {
        this.aliasStartAt = aliasStartAt;
        return this;
    }

    /**
     * The contextId is a general term for EITHER the clientId when running as a client or the gatewayId
     * when running as a gateway. It should conform to the specification. It is advised that it contains between
     * 1-23 alpha numeric characters from the ASCII character set.
     * <p>
     * NB: When running in gateway mode, this is a mandatory item that should be set by the application.
     *
     * @param contextId - The contextId is a general term for EITHER the clientId when running as a client or the gatewayId
     *                  when running as a gateway.
     * @return this configuration
     */
    public MqttsnOptions withContextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    /**
     * The time (in seconds) a client will wait for a broadcast during CONNECT before giving up
     * <p>
     * NB: only applicable to client
     *
     * @param discoveryTime - Time (in seconds) a client will wait for a broadcast during CONNECT before giving up
     *                      when running as a client.
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_DISCOVERY_TIME_SECONDS}
     */
    public MqttsnOptions withDiscoveryTime(int discoveryTime) {
        this.discoveryTime = discoveryTime;
        return this;
    }

    /**
     * The divisor to use for the ping window, the dividend being the CONNECT keepAlive resulting
     * in the quotient which is the time (since last sent message) each ping will be issued
     * <p>
     * For example a 60 seconds session with a divisor of 4 will yield 15 second pings between
     * activity
     * <p>
     * NB: only applicable to client
     *
     * @param pingDivisor - The divisor to use for the ping window
     * @return this configuration
     * @see {@link MqttsnOptions#DEFAULT_PING_DIVISOR}
     */
    public MqttsnOptions withPingDivisor(int pingDivisor) {
        this.pingDivisor = pingDivisor;
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
    public MqttsnOptions withMaxProtocolMessageSize(int maxProtocolMessageSize) {
        this.maxProtocolMessageSize = maxProtocolMessageSize;
        return this;
    }

    /**
     * When a client received a congestion returnCode from the gateway, they should back from publish, registration & subscription
     * operations for this time period
     *
     * @param congestionWait - The time (in seconds) clients should backoff when a congestion message is recieved
     * @return this configuration
     */
    public MqttsnOptions withCongestionWait(int congestionWait) {
        this.congestionWait = congestionWait;
        return this;
    }

    /**
     * Sets the locations of known clients or gateways on the network. When running as a client and discovery is not enabled,
     * it is mandatory that at least 1 gateway entry be supplied, which will be the gateway the client talks to. In gateway
     * mode, the registry is populated dynamically.
     *
     * @param contextId - the contextId of the known remote location
     * @param address   - the network address of the known remote location
     * @return this config
     */
    public MqttsnOptions withNetworkAddressEntry(String contextId, NetworkAddress address) {
        if (networkAddressEntries == null) {
            synchronized (this) {
                if (networkAddressEntries == null) {
                    networkAddressEntries = new HashMap();
                }
            }
        }
        networkAddressEntries.put(contextId, address);
        return this;
    }

    /**
     * Set the security options which will be considered by the various components of the system
     *
     * @param securityOptions - the security options to use in the runtime
     * @return this config
     */
    public MqttsnOptions withSecurityOptions(MqttsnSecurityOptions securityOptions) {
        this.securityOptions = securityOptions;
        return this;
    }

    public MqttsnOptions withClientCredentials(MqttsnClientCredentials clientCredentials){
        this.clientCredentials = clientCredentials;
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

    public String getLogPattern() {
        return logPattern;
    }

    public int getMaxErrorRetries() {
        return maxErrorRetries;
    }

    public int getMaxErrorRetryTime() {
        return maxErrorRetryTime;
    }

    public boolean isReapReceivingMessages() {
        return reapReceivingMessages;
    }

    public int getQueueProcessorThreadCount() {
        return queueProcessorThreadCount;
    }

    public int getTransportProtocolHandoffThreadCount() {
        return transportProtocolHandoffThreadCount;
    }

    public int getTransportPublishHandoffThreadCount() {
        return transportPublishHandoffThreadCount;
    }

    public int getGeneralPurposeThreadCount() {
        return generalPurposeThreadCount;
    }

    public int getCongestionWait() {
        return congestionWait;
    }

    public int getRemoveDisconnectedSessionsSeconds() {
        return removeDisconnectedSessionsSeconds;
    }

    public MqttsnSecurityOptions getSecurityOptions() {
        return securityOptions;
    }

    public void setSecurityOptions(MqttsnSecurityOptions securityOptions) {
        this.securityOptions = securityOptions;
    }

    public int getQueueBackPressure() {
        return queueBackPressure;
    }

    public int getStateLoopTimeout() {
        return stateLoopTimeout;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public int getMaxMessagesInDeadLetterQueue() {
        return maxMessagesInDeadLetterQueue;
    }

    public int getMessageQueueDiskStorageThreshold() {
        return messageQueueDiskStorageThreshold;
    }

    public MqttsnClientCredentials getClientCredentials() {
        return clientCredentials;
    }
}