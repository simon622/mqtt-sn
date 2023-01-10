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

package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.impl.AbstractMqttsnRuntime;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.model.MqttsnOptions;

import java.util.List;
import java.util.Optional;

public interface IMqttsnRuntimeRegistry {

    void setRuntime(AbstractMqttsnRuntime runtime);
    void init();
    MqttsnOptions getOptions();
    AbstractMqttsnRuntime getRuntime();

    /**
     * Add a service to the managed service lifecycle. Once bound,
     * services will be started and stopped inline with their
     * optional order parameter.
     */
    AbstractMqttsnRuntimeRegistry withService(IMqttsnService service);

    /**
     * Add a service to the managed service lifecycle. Once bound,
     * services will be started and stopped inline with their
     * optional order parameter. This version of the method will replace a service found
     * with the existing supplied interface
     */
    <T extends IMqttsnService> AbstractMqttsnRuntimeRegistry withServiceReplaceIfExists(Class<T> serviceInterface, IMqttsnService serviceInstance);

    /**
     * Obtain an immutable list of the service instances installed in the runtime.
     */
    List<IMqttsnService> getServices();

    /**
     * Obtain the service instance installed in the runtime. If it exists,
     * the instance of the supplied class (assignable from)
     * will be returned, else you will receive an unchecked runtime exception.
     *
     * If more than one matching instance of the class is found, a runtime exception will
     * be thrown
     *
     * NOTE: This method should only be used when the service is mandatory
     */
    <T extends IMqttsnService> T getService(Class<T> clz);

    /**
     * Obtain the service instances matching the class supplied on the runtime.
     * Will return [0..n] matching instances (incl.)
     */
    <T extends IMqttsnService> List<T> getServices(Class<T> clz);

    /**
     * Obtain the service instance installed in the runtime. If it exists,
     * the instance of the supplied class (assignable from)
     * will be returned, else the optional will be empty.
     *
     * @return Optional wrapping the service requested
     */
    <T extends IMqttsnService> Optional<T> getOptionalService(Class<T> clz);

    /**
     * @see IMqttsnTransport
     */
    List<IMqttsnTransport> getTransports();

    /**
     * @see IMqttsnCodec
     */
    IMqttsnCodec getCodec();

    /**
     * @see IMqttsnMessageQueue
     */
    IMqttsnMessageQueue getMessageQueue();

    /**
     * @see IMqttsnMessageFactory
     */
    IMqttsnMessageFactory getMessageFactory();

    /**
     * @see IMqttsnMessageHandler
     */
    IMqttsnMessageHandler getMessageHandler();

    /**
     * @see IMqttsnMessageStateService
     */
    IMqttsnMessageStateService getMessageStateService();

    /**
     * @see INetworkAddressRegistry
     */
    INetworkAddressRegistry getNetworkRegistry();

    /**
     * @see IMqttsnTopicRegistry
     */
    IMqttsnTopicRegistry getTopicRegistry();

    /**
     * @see IMqttsnSubscriptionRegistry
     */
    IMqttsnSubscriptionRegistry getSubscriptionRegistry();

    /**
     * @see IMqttsnContextFactory
     */
    IMqttsnContextFactory getContextFactory();

    /**
     * @see IMqttsnSessionRegistry
     */
    IMqttsnSessionRegistry getSessionRegistry();

    /**
     * @see IMqttsnMessageQueueProcessor
     */
    IMqttsnMessageQueueProcessor getQueueProcessor();

    /**
     * @see IMqttsnMessageQueueProcessor
     */
    IMqttsnQueueProcessorStateService getQueueProcessorStateCheckService();

    /**
     * @see IMqttsnMessageRegistry
     */
    IMqttsnMessageRegistry getMessageRegistry();

    /**
     * @see IMqttsnWillRegistry
     */
    IMqttsnWillRegistry getWillRegistry();

    /**
     * @see IMqttsnAuthenticationService
     */
    IMqttsnAuthenticationService getAuthenticationService();

    /**
     * @see IMqttsnAuthorizationService
     */
    IMqttsnAuthorizationService getAuthorizationService();

    /**
     * @see IMqttsnSecurityService
     */
    IMqttsnSecurityService getSecurityService();

    /**
     * @see IMqttsnTopicModifier
     */
    IMqttsnTopicModifier getTopicModifier();

    /**
     * @see IMqttsnMetricsService
     */
    IMqttsnMetricsService getMetrics();

    /**
     * @see IMqttsnStorageService
     */
    IMqttsnStorageService getStorageService();

    /**
     * @see IMqttsnClientIdFactory
     */
    IMqttsnClientIdFactory getClientIdFactory();

    /**
     * @see IMqttsnDeadLetterQueue
     */
    IMqttsnDeadLetterQueue getDeadLetterQueue() ;
}
