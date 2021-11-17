package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.model.IMqttsnContext;

/**
 * NOTE: this is optional
 * When contributed, this controller is used by the queue processor to check if the application is in a fit state to
 * offload messages to a remote (gateway or client), and is called back by the queue processor to be notified of
 * the queue being empty after having been flushed.
 */
public interface IMqttsnQueueProcessorStateService {

    /**
     * The application can determine if its in a position to send publish messages to the remote context
     * @param context - The context who is to receive messages
     * @return - Is the context in a position to receive message
     * @throws MqttsnException - An error has occurred
     */
    boolean canReceive(IMqttsnContext context) throws MqttsnException;

    /**
     * Having flushed >= 1 messages to a context, this method will be notified to allow any final
     * session related actions to be taken
     * @return - Is the context in a position to receive message
     * @throws MqttsnException - An error has occurred
     */
    void queueEmpty(IMqttsnContext context) throws MqttsnException;
}
