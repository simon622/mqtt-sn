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

package org.slj.mqtt.sn.client.spi;

import org.slj.mqtt.sn.client.MqttsnClientConnectException;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnQueueAcceptException;
import org.slj.mqtt.sn.model.MqttsnWaitToken;
import org.slj.mqtt.sn.model.MqttsnWillData;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;

import java.io.Closeable;
import java.util.Optional;

/**
 * An SN client allows you to talk to a DISCOVERED or PRECONFIGURED Sensor Network gateway.
 */
public interface IMqttsnClient extends Closeable {

    /**
     * Is the client in the CONNECTED state
     */
    boolean isConnected();

    /**
     * Is the client in the ASLEEP state
     */
    boolean isAsleep();

    /**
     * A blocking call to issue a CONNECT packet. On return your client will be considered in ACTIVE mode unless an exception
     * is thrown.
     *
     * @param keepAlive - Time in seconds to keep the session alive before the gateway times you out
     * @param cleanSession - Whether tidy up any existing session state on the gateway; including message queues, subscriptions and registrations
     * @throws MqttsnException - There was an internal error
     * @throws MqttsnClientConnectException - The connect handshake could not be completed
     */
    void connect(int keepAlive, boolean cleanSession) throws MqttsnException, MqttsnClientConnectException;

    /**
     * Add a new message onto the queue to send to the gateway at some point in the future.
     * The queue is processed in FIFO order in the background on a queue processing thread.
     * You can be notified of successful completion by registering a messageSent listener
     * onto the client.
     *
     * @param topicName - The path to which you wish to send the data
     * @param QoS - Quality of Service of the method, one of -1, 0 , 1, 2
     * @param data - The data you wish to send
     * @return token - a sending token that you can use to block on until the message has been sent
     * @throws MqttsnException - There was an internal error
     * @throws MqttsnQueueAcceptException - The queue could not accept the message, most likely full
     */
    MqttsnWaitToken publish(String topicName, int QoS, byte[] data) throws MqttsnException, MqttsnQueueAcceptException;


    /**
     * @see {@link IMqttsnMessageStateService#waitForCompletion}
     */
    Optional<IMqttsnMessage> waitForCompletion(MqttsnWaitToken token, int customWaitTime) throws MqttsnExpectationFailedException;


    /**
     * Subscribe the topic using the most appropriate topic scheme. This will automatically select the use of a PREDEFINED or SHORT
     * according to your configuration. If no PREDEFINED or SHORT topic is available, a new NORMAL registration will be placed in your
     * topic registry transparently.
     *
     * @param topicName - The path to subscribe to
     * @param QoS - The quality of service of the subscription
     * @throws MqttsnException - An error occurred
     */
    void subscribe(String topicName, int QoS) throws MqttsnException;

    /**
     * Unsubscribe the topic using the most appropriate topic scheme. This will automatically select th use of a PREDEFINED or SHORT
     * according to your configuration. If no PREDEFINED or SHORT topic is available, a new registration will be places in your
     * topic registry transparently for receiving the messages.
     *
     * @param topicName - The path to unsubscribe to
     * @throws MqttsnException -  An error occurred
     */
    void unsubscribe(String topicName) throws MqttsnException;

    /**
     * A long blocking call which will put your client into the SLEEP state for the {duration} specified in SECONDS, automatically
     * waking every {wakeAfterInterval} period of time in SECONDS to check for messages. NOTE: messages queued to be sent while
     * the device is SLEEPing will not be processed until the device is back in the ACTIVE status.
     *
     * @param duration - time in seconds for the sleep period to last
     * @param wakeAfterIntervalSeconds - time in seconds that the device will wake up to check for messages, before going back to sleep
     * @param maxWaitTimeMillis - time in millis during WAKING that the device will wait for a PINGRESP response from the gateway before erroring
     * @param connectOnFinish - when the DURATION period has elapsed, should the device transition into the ACTIVE mode by issuing a soft CONNECT or alternatively, DISCONNECT
     * @throws MqttsnException -  An error occurred
     */
    void supervisedSleepWithWake(int duration, int wakeAfterIntervalSeconds, int maxWaitTimeMillis, boolean connectOnFinish) throws MqttsnException, MqttsnClientConnectException;

    /**
     * Put the device into the SLEEP mode for the duration in seconds. NOTE: this is a non-supervized sleep, which means the application
     * is responsible for issuing PINGREQ and CONNECTS from this mode
     * @param sessionExpiryInterval - Time in seconds to put the device to sleep.
     * @throws MqttsnException -  An error occurred
     */
    void sleep(long sessionExpiryInterval) throws MqttsnException;

    /**
     * Unsupervised Wake the device up by issuing a PINGREQ from SLEEP state. The maxWait time will be taken from the core client configuration
     * supplied during setup.
     * @throws MqttsnException -  An error occurred
     */
    void wake()  throws MqttsnException;

    /**
     * Unsupervised Wake the device up by issuing a PINGREQ from SLEEP state.
     * @param waitTime - Time in MILLISECONDS to wait for a PINGRESP after the AWAKE period
     * @throws MqttsnException -  An error occurred
     */
    void wake(int waitTime)  throws MqttsnException;

    /**
     * Issue a PINGREQ during CONNECTED mode. This operation will not affect the state of the runtime, with the exception of
     * updating the last message sent time timestamp
     * @throws MqttsnException -  An error occurred
     */
    void ping() throws MqttsnException;

    /**
     * DISCONNECT from the gateway. Closes down any local queues and active processing
     */
    void disconnect() throws MqttsnException;

    /**
     * Registers a new Publish listener which will be notified when a PUBLISH message is successfully committed to the gateway
     * @param listener - The instance of the listener to notify
     */
    void registerPublishSentListener(IMqttsnPublishSentListener listener);

    /**
     * Registers a new Publish listener which will be notified when a PUBLISH message is successfully RECEIVED committed from the gateway
     * @param listener - The instance of the listener to notify
     */
    void registerPublishReceivedListener(IMqttsnPublishReceivedListener listener);

    /**
     * Return the clientId associated with this instance. The clientId is passed to the client from the configuration (contextId).
     * @return - Return the clientId associated with this instance
     */
    String getClientId();


    /**
     * Sets the will data on the runtime. If CONNECTED, the will data will be updated over the network, if NOT connected,
     * any subsequent CONNECT messages will contain the will workflow to set the will data on the gateway
     * @param willData - The will data you would like set
     */
    void setWillData(MqttsnWillData willData) throws MqttsnException ;

    /**
     * Clear any existing will data from the runtime.
     */
    void clearWillData() throws MqttsnException ;


    /**
     * Send a HELO packet to determine version of gateway
     */
    String helo()  throws MqttsnException;

}