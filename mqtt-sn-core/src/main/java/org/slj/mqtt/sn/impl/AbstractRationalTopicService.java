package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnService;

public class AbstractRationalTopicService <T extends IMqttsnRuntimeRegistry>
        extends MqttsnService<T> {

    /**
     * Method called before any interactions into or out of the registry to allow for
     * hooking to manipulate topic formats
     * @param context
     * @param topicName
     * @return
     */
    protected String rationalizeTopic(IMqttsnContext context, String topicName) throws MqttsnException {
        return topicName;
    }
}
