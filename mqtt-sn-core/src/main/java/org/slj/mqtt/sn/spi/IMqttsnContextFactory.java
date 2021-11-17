package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.MqttsnContext;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.net.NetworkContext;

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
 */
public interface IMqttsnContextFactory <T extends IMqttsnRuntimeRegistry> {

    /**
     * When no network context can be found in the registry from the associated {@link NetworkAddress},
     * the factrory is called to create a new instance from the address supplied.
     * @param address - the source address from which traffic has been received.
     * @return - the new instance of a network context bound to the address supplied
     * @throws MqttsnException - an error has occurred
     */
    INetworkContext createInitialNetworkContext(NetworkAddress address) throws MqttsnException;

    /**
     * No application existed for the network context OR a new clientId was detected, so we
     * should create a new application context pinned to the network context supplied.
     * @param context - The source network context
     * @param clientId - The clientId which was supplied by the CONNECT packet
     * @return the new instance of the application context coupled to the network context
     * @throws MqttsnSecurityException - The supplied clientId was not allowed on the gateway
     */
    IMqttsnContext createInitialApplicationContext(INetworkContext context, String clientId) throws MqttsnSecurityException;

    /**
     * No application existed for the network context OR a new clientId was detected, so we
     * should create a new application context pinned to the network context supplied.
     * @param context - The source network context
     * @return the new instance of the application context coupled to the network context
     * @throws MqttsnSecurityException - The supplied clientId was not allowed on the gateway
     */
    IMqttsnContext createTemporaryApplicationContext(INetworkContext context) throws MqttsnSecurityException;

}
