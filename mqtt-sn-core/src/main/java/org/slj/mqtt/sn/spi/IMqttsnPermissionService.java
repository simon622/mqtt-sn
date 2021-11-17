package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.model.IMqttsnContext;

/**
 * Optional - when installed it will be consulted to determine whether a remote context can perform certain
 * operations;
 *
 * CONNECT with the given clientId
 * SUBSCRIBE to a given topicPath
 * Granted Maximum Subscription Levels
 * Eligibility to publish to a given path & size
 */
public interface IMqttsnPermissionService {

    /**
     * Is a client allowed to CONNECT successfully.
     * @param context - the context who would like to CONNECT
     * @param clientId - the client Id they provided in the CONNECT dialog
     * @return true if the client is allowed to CONNECT (yielding CONNACK ok) or false if not allowed (yielding CONNACK error)
     * @throws MqttsnException an error occurred
     */
    boolean allowConnect(IMqttsnContext context, String clientId) throws MqttsnException;

    /**
     * Is a client allowed to SUBSCRIBE to a given topic path
     * @param context - the context who would like to SUBSCRIBE
     * @param topicPath - the topic path to subscribe to
     * @return true if the client is allowed to SUBSCRIBE (yielding SUBACK ok) or false if not allowed (yielding SUBACK error)
     * @throws MqttsnException an error occurred
     */
    boolean allowedToSubscribe(IMqttsnContext context, String topicPath) throws MqttsnException;

    /**
     * What is the maximum granted QoS allowed on a given topicPath.
     * @param context - the context who would like to SUBSCRIBE
     * @param topicPath - the topic path to subscribe to
     * @return one of 0,1,2 matching the maximum QoS that can be granted for this subscription
     * @throws MqttsnException an error occurred
     */
    int allowedMaximumQoS(IMqttsnContext context, String topicPath) throws MqttsnException;

    /**
     * Is the context allowed to publish to the given topicPath.
     * @param context - the context who would like to PUBLISH
     * @param topicPath - the topic path to publish to
     * @param size - size of the data to be published
     * @return true if the client is allowed to PUBLISH (yielding PUBLISH normal lifecycle) or false if not allowed (yielding PUBACK error)
     * @throws MqttsnException an error occurred
     */
    boolean allowedToPublish(IMqttsnContext context, String topicPath, int size, int QoS) throws MqttsnException;
}
