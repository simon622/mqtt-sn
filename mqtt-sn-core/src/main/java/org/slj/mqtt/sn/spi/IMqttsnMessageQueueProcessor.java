package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.model.IMqttsnContext;

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
 */
public interface IMqttsnMessageQueueProcessor<T extends IMqttsnRuntimeRegistry>
            extends IMqttsnService<T> {

    enum RESULT {
        REMOVE_PROCESS,
        BACKOFF_PROCESS,
        REPROCESS
    }

    RESULT process(IMqttsnContext context) throws MqttsnException ;
}
