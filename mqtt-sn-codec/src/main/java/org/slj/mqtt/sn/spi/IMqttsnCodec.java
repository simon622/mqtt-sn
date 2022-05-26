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

import org.slj.mqtt.sn.PublishData;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.codec.MqttsnUnsupportedVersionException;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnPublish;

/**
 * A codec contains all the functionality to marshall and unmarshall
 * wire traffic in the format specified by the implementation. Further,
 * it also provides a message factory instance which allows construction
 * of wire messages hiding the underlying transport format. This allows versioned
 * protocol support.
 *
 * @author Simon Johnson <simon622 AT gmail DOT com>
 */
public interface IMqttsnCodec {

    /**
     * @return - Does the message represent a PUBLISH
     */
    PublishData getData(IMqttsnMessage message);

    /**
     * @return - Get the is retained from the supplied messages
     */
    boolean isRetainedPublish(IMqttsnMessage message);

    /**
     * @return - Get the QoS from the supplied messages
     * If convert is used, the -1 will be upgraded to 0 for onward publishing
     */
    int getQoS(IMqttsnMessage message, boolean convertMinus1);

    /**
     * @return the client from a CONNECT message
     */
    String getClientId(IMqttsnMessage message);

    /**
     * @return the cleanSession flag
     */
    boolean isCleanSession(IMqttsnMessage message);

    /**
     * @return the keepAlive flag
     */
    long getKeepAlive(IMqttsnMessage message);

    /**
     * @return the duration flag
     */
    long getDuration(IMqttsnMessage message);

    /**
     * @return - Does the message represent a CONNECT
     */
    boolean isConnect(IMqttsnMessage message);

    /**
     * @return - Does the message represent a PUBLISH
     */
    boolean isPublish(IMqttsnMessage message);

    /**
     * @return - Does the message represent a PUBACK
     */
    boolean isPuback(IMqttsnMessage message);

    /**
     * @return - Does the message represent a PUBREL
     */
    boolean isPubRel(IMqttsnMessage message);

    /**
     * @return - Does the message represent a PUBREC
     */
    boolean isPubRec(IMqttsnMessage message);

    /**
     * @return - Does the message represent a DISCONNECT
     */
    boolean isDisconnect(IMqttsnMessage message);

    /**
     * Is the message considered an ACTIVE message, that is a message actively sent by an application
     * (anything other than a PINGREQ, PINGRESP, DISCONNECT)
     * @param message - the message to consider
     * @return Is the message considered an ACTIVE message
     */
    boolean isActiveMessage(IMqttsnMessage message);

    /**
     * To help with debugging, this method will return a binary or hex
     * representation of the encoded message
     */
    String print(IMqttsnMessage message) throws MqttsnCodecException;

    /**
     * Given data of the wire, will convert to the message model which can be used
     * in a given runtime
     *
     * @throws MqttsnCodecException - something went wrong when decoding the data
     */
    IMqttsnMessage decode(byte[] data) throws MqttsnCodecException, MqttsnUnsupportedVersionException;

    /**
     * When supplied with messages constructed from an associated message factory,
     * will encode them into data that can be sent on the wire
     *
     * @throws MqttsnCodecException - something went wrong when encoding the data
     */
    byte[] encode(IMqttsnMessage message) throws MqttsnCodecException;

    /**
     * A message factory will contruct messages using convenience methods
     * that hide the complexity of the underlying wire format
     */
    IMqttsnMessageFactory createMessageFactory();

    /**
     * Using the first few bytes of a message, determine the length of the full message,
     * for use with stream reading
     */
    int readMessageSize(byte[] arr) throws MqttsnCodecException;


    /**
     * Runs validation on the mqttsn message fields
     * @param message
     * @throws MqttsnCodecException
     */
    void validate(IMqttsnMessage message) throws MqttsnCodecException;

    /**
     * Determine if the codec supports the protocol version presented by the client
     * @param protocolVersion
     */
    boolean supportsVersion(int protocolVersion) throws MqttsnCodecException;


    /**
     * Returns the protocolVersion that this codec supports. Where is supports multiple, the highest should be chosen
     */
    int getProtocolVersion() throws MqttsnCodecException;
}
