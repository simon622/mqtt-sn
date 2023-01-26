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

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.codec.MqttsnCodecException;

/**
 * Use to obtain instances of the codec messages for use in a client or gateway runtime.
 * The lightweight abstraction allows message implementations & versions to be pluggable
 * into 3rd party systems.
 *
 * @author Simon Johnson <simon622 AT gmail DOT com>
 * See mqttsn specification at http://www.mqtt.org/new/wp-content/uploads/2009/06/MQTT-SN_spec_v1.2.pdf
 */
public interface IMqttsnMessageFactory {

    /**
     * The ADVERTISE message is broadcasted periodically by a gateway to advertise its presence.
     * The time interval until the next broadcast time is indicated in the Duration field of this message.
     * Its format is illustrated in Table 6 of the specification.
     *
     * @Param gatewayId the id of the gateway which sends this message.
     * @Param duration time interval until the next ADVERTISE is broadcasted by this gateway.
     */
    IMqttsnMessage createAdvertise(int gatewayId, int duration)
            throws MqttsnCodecException;

    /**
     * The SEARCHGW message is broadcasted by a client when it searches for a GW. The broadcast radius
     * of the SEARCHGW is limited and depends on the density of the clients deployment, e.g. only 1-hop
     * broadcast in case of a very dense network in which every MQTT-SN client is reachable from each
     * other within 1-hop transmission.
     *
     * @param radius: the broadcast radius of this message.
     *                The broadcast radius is also indicated to the underlying network layer when MQTT-SN gives this message for transmission.
     */
    IMqttsnMessage createSearchGw(int radius)
            throws MqttsnCodecException;

    /**
     * The GWINFO message is sent as response to a SEARCHGW message using the broadcast service of the underlying layer,
     * with the radius as indicated in the SEARCHGW message. If sent by a GW, it contains only the id of the sending GW;
     * otherwise, if sent by a client, it also includes the address of the GW.
     *
     * @param gatewayId:      the id of a GW.
     * @param gatewayAddress: address of the indicated GW; optional, only included if message is sent by a client.
     *                        Like the SEARCHGW message the broadcast radius for this message is also indicated to the underlying
     *                        network layer when MQTT-SN gives this message for transmission.
     */
    IMqttsnMessage createGwinfo(int gatewayId, String gatewayAddress)
            throws MqttsnCodecException;

    /**
     * The CONNECT message is sent by a client to setup a connection.
     *
     * @param clientId:     same as with MQTT, contains the client id which is a 1-23 character long string which uniquely identifies the client to the server.
     * @param keepAlive:    same as with MQTT, contains the value of the Keep Alive timer.
     * @param willPrompt:   if set, indicates that client is requesting for Will topic and Will message prompting.
     * @param cleanSession: same meaning as with MQTT, however extended for Will topic and Will message.
     * @param defaultAwakeMessages: the default max number of messages transmitted per awake cycle.
     */
    IMqttsnMessage createConnect(String clientId, int keepAlive, boolean willPrompt, boolean cleanSession, int maxPacketSize, int defaultAwakeMessages, long sessionExpiryInterval)
            throws MqttsnCodecException;

    /**
     * The CONNACK message is sent by the server in response to a connection request from a client.
     * <p>
     * Allowed Return code(s):
     * 0x00 - Accepted
     * 0x01 - Rejected: congestion
     * 0x02 - Rejected: invalid topic ID
     * 0x03 - Rejected: not supported reserved
     * 0x04 - 0xFF: reserved
     *
     * @param returnCode: see above
     */
    IMqttsnMessage createConnack(int returnCode)
            throws MqttsnCodecException;


    /**
     * The CONNACK message is sent by the server in response to a connection request from a client.
     * @param returnCode: see above
     */
    IMqttsnMessage createConnack(int returnCode, boolean sessionExists, String assignedClientId, long sessionExpiryInterval)
            throws MqttsnCodecException;


    /**
     * The WILLTOPICREQ message is sent by the GW to request a client for sending the Will topic name.
     */
    IMqttsnMessage createWillTopicReq()
            throws MqttsnCodecException;

    /**
     * The WILLTOPIC message is sent by a client as response to the WILLTOPICREQ message for transferring
     * its Will topic name to the GW.
     * <p>
     * An empty WILLTOPIC message is a WILLTOPIC message without Flags and WillTopic field (i.e. it is exactly 2 octets long).
     * It is used by a client to delete the Will topic and the Will message stored in the server, see Section 6.4.
     *
     * @param QoS:       same as MQTT, contains the Will QoS
     * @param retain:    same as MQTT, contains the Will Retain flag – Will: not used
     * @param topicPath: contains the Will topic name.
     */
    IMqttsnMessage createWillTopic(int QoS, boolean retain, String topicPath)
            throws MqttsnCodecException;

    /**
     * The WILLTOPICRESP message is sent by a GW to acknowledge the receipt and processing of an WILLTOPICUPD message.
     * <p>
     * Allowed Return code(s):
     * 0x00 - Accepted
     * 0x01 - Rejected: congestion
     * 0x02 - Rejected: invalid topic ID
     * 0x03 - Rejected: not supported reserved
     * 0x04 - 0xFF: reserved
     *
     * @param returnCode: see above
     */
    IMqttsnMessage createWillTopicResp(int returnCode)
            throws MqttsnCodecException;

    /**
     * The WILLTOPICUPD message is sent by a client to update its Will topic name stored in the GW/server.
     * An empty WILLTOPICUPD message is a WILLTOPICUPD message without Flags and WillTopic field (i.e.
     * it is exactly 2 octets long). It is used by a client to delete its Will topic and Will message stored in the GW/server.
     *
     * @param QoS       same as MQTT, contains the Will QoS
     * @param retain    same as MQTT, contains the Will Retain flag – Will: not used
     * @param topicPath contains the Will topic name.
     */
    IMqttsnMessage createWillTopicupd(int QoS, boolean retain, String topicPath)
            throws MqttsnCodecException;

    /**
     * The WILLMSGUPD message is sent by a client to update its Will message stored in the GW/server.
     *
     * @param payload contains the Will message.
     */
    IMqttsnMessage createWillMsgupd(byte[] payload)
            throws MqttsnCodecException;

    /**
     * The WILLMSGREQ message is sent by the GW to request a client for sending the Will message.
     */
    IMqttsnMessage createWillMsgReq()
            throws MqttsnCodecException;

    /**
     * The WILLMSG message is sent by a client as response to a WILLMSGREQ for transferring its Will message to the GW.
     *
     * @param payload: contains the Will message.
     */
    IMqttsnMessage createWillMsg(byte[] payload)
            throws MqttsnCodecException;

    /**
     * The WILLMSGRESP message is sent by a GW to acknowledge the receipt and processing of an WILLMSGUPD message.
     * <p>
     * Allowed Return code(s):
     * 0x00 - Accepted
     * 0x01 - Rejected: congestion
     * 0x02 - Rejected: invalid topic ID
     * 0x03 - Rejected: not supported reserved
     * 0x04 - 0xFF: reserved
     *
     * @param returnCode: see above
     */
    IMqttsnMessage createWillMsgResp(int returnCode)
            throws MqttsnCodecException;

    /**
     * The REGISTER message is sent by a client to a GW for requesting a topic id value for the included topic name.
     * It is also sent by a GW to inform a client about the topic id value it has assigned to the included topic name.
     *
     * @param topicPath: contains the topic name.
     */
    IMqttsnMessage createRegister(String topicPath)
            throws MqttsnCodecException;

    /**
     * The REGISTER message is sent by a client to a GW for requesting a topic id value for the included topic name.
     * It is also sent by a GW to inform a client about the topic id value it has assigned to the included topic name.
     *
     * @param topicAlias: if sent by a client, it is coded 0x0000 and is not relevant; if sent by a GW, it contains the topic id
     *                    value assigned to the topic name included in the TopicName field;
     * @param topicPath:  contains the topic name.
     */
    IMqttsnMessage createRegister(int topicAlias, String topicPath)
            throws MqttsnCodecException;

    /**
     * The REGACK message is sent by a client or by a GW as an acknowledgment to the receipt and processing of a REGISTER message.
     * <p>
     * Allowed Return code(s):
     * 0x00 - Accepted
     * 0x01 - Rejected: congestion
     * 0x02 - Rejected: invalid topic ID
     * 0x03 - Rejected: not supported reserved
     * 0x04 - 0xFF: reserved
     *
     * @param topicAlias: the value that shall be used as topic id in the PUBLISH messages;
     * @param returnCode
     */
    IMqttsnMessage createRegack(int topicAliasTypeId, int topicAlias, int returnCode)
            throws MqttsnCodecException;


    /**
     * This message is used by both clients and gateways to publish data for a certain topic.
     *
     * @param DUP:     same as MQTT, indicates whether message is sent for the first time or not.
     * @param QoS:     same as MQTT, contains the QoS level for this PUBLISH message.
     * @param retain:  same as MQTT, contains the Retain flag.
     * @param type:    indicates the type of the topic id contained in the TopicId field.
     * @param topicId: contains the topic id value or the short topic name for which the data is published.
     * @param payload: the published data
     */
    IMqttsnMessage createPublish(int QoS, boolean DUP, boolean retain, MqttsnConstants.TOPIC_TYPE type, int topicId, byte[] payload)
            throws MqttsnCodecException;

    /**
     * This message is used by both clients and gateways to publish data for topics with short names
     *
     * @param DUP:       same as MQTT, indicates whether message is sent for the first time or not.
     * @param QoS:       same as MQTT, contains the QoS level for this PUBLISH message.
     * @param retain:    same as MQTT, contains the Retain flag.
     * @param topicPath: the SHORT topic path - version 1.2 does not support full topicNames in the publish
     * @param payload:   the published data
     */
    IMqttsnMessage createPublish(int QoS, boolean DUP, boolean retain, String topicPath, byte[] payload) throws MqttsnCodecException;

    /**
     * The PUBACK message is sent by a gateway or a client as an acknowledgment to the receipt and processing
     * of a PUBLISH message in case of QoS levels 1 or 2. It can also be sent as response to a PUBLISH message
     * in case of an error; the error reason is then indicated in the ReturnCode field.
     * <p>
     * Allowed Return code(s):
     * 0x00 - Accepted
     * 0x01 - Rejected: congestion
     * 0x02 - Rejected: invalid topic ID
     * 0x03 - Rejected: not supported reserved
     * 0x04 - 0xFF: reserved
     *
     * @param topicId:   same value the one contained in the corresponding PUBLISH message.
     * @param returnCode - see table
     */
    IMqttsnMessage createPuback(int topicId, int returnCode)
            throws MqttsnCodecException;

    /**
     * As with MQTT, the PUBREC, PUBREL, and PUBCOMP messages are used in conjunction with a PUBLISH message with QoS level 2.
     */
    IMqttsnMessage createPubrec()
            throws MqttsnCodecException;

    /**
     * As with MQTT, the PUBREC, PUBREL, and PUBCOMP messages are used in conjunction with a PUBLISH message with QoS level 2.
     */
    IMqttsnMessage createPubrel()
            throws MqttsnCodecException;

    /**
     * As with MQTT, the PUBREC, PUBREL, and PUBCOMP messages are used in conjunction with a PUBLISH message with QoS level 2.
     */
    IMqttsnMessage createPubcomp()
            throws MqttsnCodecException;

    /**
     * The SUBSCRIBE message is used by a client to subscribe to a certain topic name.
     * NB: this method should ONLY be used to set topicAlias to either PREDEFINED or NORMAL
     *
     * @param QoS:     same as MQTT, contains the requested QoS level for this topic.
     * @param topicId: topic id
     */
    IMqttsnMessage createSubscribe(int QoS, MqttsnConstants.TOPIC_TYPE type, int topicId)
            throws MqttsnCodecException;

    /**
     * The SUBSCRIBE message is used by a client to subscribe to a certain topic name.
     *
     * @param QoS:       same as MQTT, contains the requested QoS level for this topic.
     * @param topicName: topic path to subscribe to
     */
    IMqttsnMessage createSubscribe(int QoS, String topicName)
            throws MqttsnCodecException;

    /**
     * The SUBACK message is sent by a gateway to a client as an acknowledgment to the receipt and processing of a SUBSCRIBE message.
     * <p>
     * Allowed Return code(s):
     * 0x00 - Accepted
     * 0x01 - Rejected: congestion
     * 0x02 - Rejected: invalid topic ID
     * 0x03 - Rejected: not supported reserved
     * 0x04 - 0xFF: reserved
     *
     * @param grantedQoS: same as MQTT, contains the granted QoS level.
     * @param topicId:    in case of "accepted" the value that will be used as topicid by the gateway when sending PUBLISH messages to the client
     *                    (not relevant in case of subscriptions to a short topic name or to a topic name which contains wildcard characters)
     * @param returnCode  - see table
     */
    IMqttsnMessage createSuback(int grantedQoS, int topicId, int returnCode)
            throws MqttsnCodecException;

    /**
     * An UNSUBSCRIBE message is sent by the client to the GW to unsubscribe from named topics.
     *
     * @param type:   indicates the type of the topic id contained in the TopicId field.
     * @param topicId the topicAlias
     */
    IMqttsnMessage createUnsubscribe(MqttsnConstants.TOPIC_TYPE type, int topicId)
            throws MqttsnCodecException;

    /**
     * An UNSUBSCRIBE message is sent by the client to the GW to unsubscribe from named topics.
     *
     * @param topicName the topicName
     */
    IMqttsnMessage createUnsubscribe(String topicName)
            throws MqttsnCodecException;

    /**
     * An UNSUBACK message is sent by a GW to acknowledge the receipt and processing of an UNSUBSCRIBE
     */
    IMqttsnMessage createUnsuback(int reasonCode)
            throws MqttsnCodecException;

    /**
     * As with MQTT, the PINGREQ message is an ”are you alive” message that is sent from or received by a connected client.
     *
     * @param clientId: contains the client id; this field is optional and is included by a "sleeping" client when
     *                  it goes to the "awake" state and is waiting for messages sent by the server/gateway.
     */
    IMqttsnMessage createPingreq(String clientId)
            throws MqttsnCodecException;

    /**
     * As with MQTT, a PINGRESP message is the response to a PINGREQ message and means ”yes I am alive”. Keep Alive messages
     * flow in either direction, sent either by a connected client or the gateway.
     * Moreover, a PINGRESP message is sent by a gateway to inform a sleeping client that it has no more buffered messages
     * for that client.
     */
    IMqttsnMessage createPingresp()
            throws MqttsnCodecException;

    /**
     * As with MQTT, the DISCONNECT message is sent by a client to indicate that it wants to close the connection.
     * The gateway will acknowledge the receipt of that message by returning a DISCONNECT to the client.
     * A server or gateway may also sends a DISCONNECT to a client, e.g. in case a gateway, due to an
     * error, cannot map a received message to a client. Upon receiving such a DISCONNECT message,
     * a client should try to setup the connection again by sending a CONNECT message to the gateway or server.
     * In all these cases the DISCONNECT message does not contain the Duration field.
     */
    IMqttsnMessage createDisconnect()
            throws MqttsnCodecException;

    /**
     * A DISCONNECT message with a Duration field is sent by a client when it wants to go to the "asleep" state.
     * The receipt of this message is also acknowledged by the gateway by means of a DISCONNECT message
     * (without a duration field).
     *
     * @param sessionExpiryInterval - length of sleeping session
     */
    IMqttsnMessage createDisconnect(long sessionExpiryInterval, boolean retainRegistrations)
            throws MqttsnCodecException;

    /**
     * A DISCONNECT message for use in error conditions including the returnCode and reasonString
     *
     * @param returnCode - return code to send
     */
    IMqttsnMessage createDisconnect(int returnCode, String reasonString)
            throws MqttsnCodecException;

    /**
     * A NON STANDARD HELO message to allow a client or gateway to ascertain which version of software a gateway or client is running
     *
     * @param userAgent - the userAgent of the client or gateway
     */
    IMqttsnMessage createHelo(String userAgent)
            throws MqttsnCodecException;

    /**
     * Encapsulate message for use on forwarders.
     *
     * @param wirelessNodeId - Wireless Node Id: identifies the wireless node which has sent or should receive the encapsulated MQTT-SN message.
     *                       The mapping between this Id and the address of the wireless node is implemented by the forwarder, if needed.
     * @param radius         - broadcast radius (only relevant in direction GW to forwarder)
     * @param messageData    - the MQTT-SN message to forward
     */
    IMqttsnMessage createEncapsulatedMessage(String wirelessNodeId, int radius, byte[] messageData)
            throws MqttsnCodecException;
}