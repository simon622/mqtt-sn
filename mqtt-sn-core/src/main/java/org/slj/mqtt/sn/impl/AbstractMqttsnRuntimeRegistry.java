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

package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.model.MqttsnContext;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.net.NetworkContext;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.spi.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The base runtime registry provides support for simple fluent construction and encapsulates
 * both the controllers and the configuration for a runtime. Each controller has access to the
 * registry and uses it to access other parts of the application and config.
 *
 * During startup, the registry will be validated to ensure all required components are available.
 * Extending implementations should provide convenience methods for out-of-box runtimes.
 */
public abstract class AbstractMqttsnRuntimeRegistry implements IMqttsnRuntimeRegistry {

    protected MqttsnOptions options;

    //-- obtained lazily from the codec --//
    protected IMqttsnMessageFactory factory;

    protected IMqttsnCodec codec;
    protected IMqttsnMessageHandler messageHandler;
    protected IMqttsnMessageQueue messageQueue;
    protected IMqttsnTransport transport;
    protected AbstractMqttsnRuntime runtime;
    protected INetworkAddressRegistry networkAddressRegistry;
    protected IMqttsnTopicRegistry topicRegistry;
    protected IMqttsnSubscriptionRegistry subscriptionRegistry;
    protected IMqttsnMessageStateService messageStateService;
    protected IMqttsnContextFactory contextFactory;
    protected IMqttsnMessageQueueProcessor queueProcessor;
    protected IMqttsnQueueProcessorStateService queueProcessorStateCheckService;
    protected IMqttsnMessageRegistry messageRegistry;
    protected IMqttsnPermissionService permissionService;
    protected List<IMqttsnTrafficListener> trafficListeners;

    public AbstractMqttsnRuntimeRegistry(MqttsnOptions options){
        this.options = options;
    }

    @Override
    public void init() {
        validateOnStartup();
        initNetworkRegistry();
        factory = codec.createMessageFactory();
    }

    protected void initNetworkRegistry(){
        //-- ensure initial definitions exist in the network registry
        if(options.getNetworkAddressEntries() != null && !options.getNetworkAddressEntries().isEmpty()){
            Iterator<String> itr = options.getNetworkAddressEntries().keySet().iterator();
            while(itr.hasNext()){
                String key = itr.next();
                NetworkAddress address = options.getNetworkAddressEntries().get(key);
                NetworkContext networkContext = new NetworkContext(address);
                MqttsnContext sessionContext = new MqttsnContext(key);
                networkAddressRegistry.bindContexts(networkContext, sessionContext);
            }
        }
    }

    /**
     * Traffic listeners can contributed to the runtime to be notified of any traffic processed by
     * the transport layer. Listeners are not able to affect the traffic in transit or the business
     * logic executed during the course of the traffic, they are merely observers.
     *
     * The listeners ~may be notified asynchronously from the application, and thus they cannot be relied
     * upon to give an absolute timeline of traffic.
     *
     * @param trafficListener - The traffic listener instance which will be notified upon traffic being processed.
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withTrafficListener(IMqttsnTrafficListener trafficListener){
        if(trafficListeners == null){
            synchronized (this){
                if(trafficListeners == null){
                    trafficListeners = new ArrayList<>();
                }
            }
        }
        trafficListeners.add(trafficListener);
        return this;
    }

    /**
     * NOTE: this is optional
     * When contributed, this controller is used by the queue processor to check if the application is in a fit state to
     * offload messages to a remote (gateway or client), and is called back by the queue processor to be notified of
     * the queue being empty after having been flushed.
     *
     * @param queueProcessorStateCheckService - The instance
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withQueueProcessorStateCheck(IMqttsnQueueProcessorStateService queueProcessorStateCheckService){
        this.queueProcessorStateCheckService = queueProcessorStateCheckService;
        return this;
    }

    /**
     * The job of the queue processor is to (when requested) interact with a remote contexts' queue, processing
     * the next message from the HEAD of the queue, handling any topic registration, session state, marking
     * messages inflight and finally returning an indicator as to what should happen when the processing of
     * the next message is complete. Upon dealing with the next message, whether successful or not, the processor
     * needs to return an indiction;
     *
     *  REMOVE_PROCESS - The queue is empty and the context no longer needs further processing
     *  BACKOFF_PROCESS - The queue is not empty, come back after a backend to try again. Repeating this return type for the same context
     *                      will yield an exponential backoff
     *  REPROCESS (continue) - The queue is not empty, (where possible) call me back immediately to process again
     *
     * @param queueProcessor - The instance
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withQueueProcessor(IMqttsnMessageQueueProcessor queueProcessor){
        this.queueProcessor = queueProcessor;
        return this;
    }

    /**
     * A context factory deals with the initial construction of the context objects which identity
     * the remote connection to the application. There are 2 types of context; a {@link NetworkContext}
     * and a {@link MqttsnContext}. The network context identifies where (the network location) the identity
     * resides and the mqttsn-context identifies who the context is (generally this is the CliendId or GatewayId of
     * the connected resource).
     *
     * A {@link NetworkContext} can exist in isolation without an associated {@link MqttsnContext}, during a CONNECT attempt
     *  (when the context has yet to be established), or during a failed CONNECTion. An application context cannot exist without
     * a network context.
     *
     * You can provide your own implementation, if you wish to wrap or provide your own extending context implementation
     * to wrap custom User objects, for example.
     *
     * @param contextFactory - The instance
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withContextFactory(IMqttsnContextFactory contextFactory){
        this.contextFactory = contextFactory;
        return this;
    }

    /**
     * The message registry is a normalised view of transiting messages, it context the raw payload of publish operations
     * so light weight references to the payload can exist in multiple storage systems without duplication of data.
     * For example, when running in gateway mode, the same message my reside in queues for numerous devices which are
     * in different connection states. We should not store payloads N times in this case.
     *
     * The lookup is a simple UUID -> byte[] relationship. It is up to the registry implementation to decide how to store
     * this data.
     *
     * @param messageRegistry The instance
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withMessageRegistry(IMqttsnMessageRegistry messageRegistry){
        this.messageRegistry = messageRegistry;
        return this;
    }

    /**
     * The state service is responsible for sending messages and processing received messages. It maintains state
     * and tracks messages in and out and their successful acknowledgement (or not).
     *
     * The message handling layer will call into the state service with messages it has received, and the queue processor
     * will use the state service to dispatch new outbound publish messages.
     *
     * @param messageStateService The instance
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withMessageStateService(IMqttsnMessageStateService messageStateService){
        this.messageStateService = messageStateService;
        return this;
    }

    /**
     * The subscription registry maintains a list of subscriptions against the remote context. On the gateway this
     * is used to determine which clients are subscribed to which topics to enable outbound delivery. In client
     * mode it tracks the subscriptions a client presently holds.
     *
     * @param subscriptionRegistry The instance
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withSubscriptionRegistry(IMqttsnSubscriptionRegistry subscriptionRegistry){
        this.subscriptionRegistry = subscriptionRegistry;
        return this;
    }

    /**
     * The topic registry is responsible for tracking, storing and determining the correct alias
     * to use for a given remote context and topic combination. The topic registry will be cleared
     * according to session lifecycle rules.
     *
     * @param topicRegistry The instance
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withTopicRegistry(IMqttsnTopicRegistry topicRegistry){
        this.topicRegistry = topicRegistry;
        return this;
    }

    /**
     * Queue implementation to store messages destined to and from gateways and clients. Queues will be flushed acccording
     * to the session semantics defined during CONNECT.
     *
     * Ideally the queue should be implemented to support FIFO where possible.
     *
     * @param messageQueue The instance
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withMessageQueue(IMqttsnMessageQueue messageQueue){
        this.messageQueue = messageQueue;
        return this;
    }

    /**
     * A codec contains all the functionality to marshall and unmarshall
     * wire traffic in the format specified by the implementation. Further,
     * it also provides a message factory instance which allows construction
     * of wire messages hiding the underlying transport format. This allows versioned
     * protocol support.
     *
     * @param codec The instance
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withCodec(IMqttsnCodec codec){
        this.codec = codec;
        return this;
    }

    /**
     * The message handler is delegated to by the transport layer and its job is to process
     * inbound messages and marshall into other controllers to manage state lifecycle, authentication, permission
     * etc. It is directly responsible for creating response messages and sending them back to the transport layer.
     *
     * @param handler The instance
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withMessageHandler(IMqttsnMessageHandler handler){
        this.messageHandler = handler;
        return this;
    }

    /**
     * The transport layer is responsible for managing the receiving and sending of messages over some connection.
     * No session is assumed by the application and the connection is considered stateless at this point.
     * It is envisaged implementations will include UDP (with and without DTLS), TCP-IP (with and without TLS),
     * BLE and ZigBee.
     *
     * Please refer to {@link org.slj.mqtt.sn.impl.AbstractMqttsnTransport} and sub-class your own implementations
     * or choose an existing implementation out of the box.
     *
     * @see {@link org.slj.mqtt.sn.net.MqttsnUdpTransport} for an example of an out of the box implementation.
     *
     * @param transport The instance
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withTransport(IMqttsnTransport transport){
        this.transport = transport;
        return this;
    }

    /**
     * The network registry maintains a list of known network contexts against a remote address ({@link NetworkAddress}).
     * It exposes functionality to wait for discovered contexts as well as returning a list of valid broadcast addresses.
     *
     * @param networkAddressRegistry The instance
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withNetworkAddressRegistry(INetworkAddressRegistry networkAddressRegistry){
        this.networkAddressRegistry = networkAddressRegistry;
        return this;
    }

    /**
     * Optional - when installed it will be consulted to determine whether a remote context can perform certain
     * operations;
     *
     * CONNECT with the given clientId
     * SUBSCRIBE to a given topicPath
     * Granted Maximum Subscription Levels
     * Eligibility to publish to a given path & size
     *
     * @param permissionService The instance
     * @return This runtime registry
     */
    public AbstractMqttsnRuntimeRegistry withPermissionService(IMqttsnPermissionService permissionService){
        this.permissionService = permissionService;
        return this;
    }

    @Override
    public MqttsnOptions getOptions() {
        return options;
    }

    @Override
    public INetworkAddressRegistry getNetworkRegistry() {
        return networkAddressRegistry;
    }

    @Override
    public void setRuntime(AbstractMqttsnRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public AbstractMqttsnRuntime getRuntime() {
        return runtime;
    }

    @Override
    public IMqttsnPermissionService getPermissionService() {
        return permissionService;
    }

    public IMqttsnQueueProcessorStateService getQueueProcessorStateCheckService() {
        return queueProcessorStateCheckService;
    }

    @Override
    public IMqttsnCodec getCodec() {
        return codec;
    }

    @Override
    public IMqttsnMessageHandler getMessageHandler() {
        return messageHandler;
    }

    @Override
    public IMqttsnTransport getTransport() {
        return transport;
    }

    @Override
    public IMqttsnMessageFactory getMessageFactory(){
        return factory;
    }

    @Override
    public IMqttsnMessageQueue getMessageQueue() {
        return messageQueue;
    }

    @Override
    public IMqttsnTopicRegistry getTopicRegistry() {
        return topicRegistry;
    }

    @Override
    public IMqttsnSubscriptionRegistry getSubscriptionRegistry() {
        return subscriptionRegistry;
    }

    @Override
    public IMqttsnMessageStateService getMessageStateService() {
        return messageStateService;
    }

    @Override
    public IMqttsnMessageRegistry getMessageRegistry(){
        return messageRegistry;
    }

    @Override
    public List<IMqttsnTrafficListener> getTrafficListeners() {
        return trafficListeners;
    }

    @Override
    public IMqttsnContextFactory getContextFactory() {
        return contextFactory;
    }

    @Override
    public IMqttsnMessageQueueProcessor getQueueProcessor() {
        return queueProcessor;
    }

    protected void validateOnStartup() throws MqttsnRuntimeException {
        if(networkAddressRegistry == null) throw new MqttsnRuntimeException("network-registry must be bound for valid runtime");
        if(messageStateService == null) throw new MqttsnRuntimeException("message state service must be bound for valid runtime");
        if(transport == null) throw new MqttsnRuntimeException("transport must be bound for valid runtime");
        if(topicRegistry == null) throw new MqttsnRuntimeException("topic registry must be bound for valid runtime");
        if(codec == null) throw new MqttsnRuntimeException("codec must be bound for valid runtime");
        if(messageHandler == null) throw new MqttsnRuntimeException("message handler must be bound for valid runtime");
        if(messageQueue == null) throw new MqttsnRuntimeException("message queue must be bound for valid runtime");
        if(contextFactory == null) throw new MqttsnRuntimeException("context factory must be bound for valid runtime");
        if(queueProcessor == null) throw new MqttsnRuntimeException("queue processor must be bound for valid runtime");
        if(messageRegistry == null) throw new MqttsnRuntimeException("message registry must be bound for valid runtime");
    }
}