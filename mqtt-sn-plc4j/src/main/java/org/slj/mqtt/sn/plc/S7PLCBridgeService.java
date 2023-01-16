package org.slj.mqtt.sn.plc;

import org.slj.mqtt.sn.spi.AbstractMqttsnService;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnService;

@MqttsnService
public class S7PLCBridgeService extends AbstractMqttsnService {

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);


    }

    @Override
    public void stop() throws MqttsnException {
        super.stop();
    }
}
