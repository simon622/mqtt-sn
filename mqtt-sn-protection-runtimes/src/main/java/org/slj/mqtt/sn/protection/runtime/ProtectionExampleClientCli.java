package org.slj.mqtt.sn.protection.runtime;

import org.slj.mqtt.sn.client.impl.MqttsnClientRuntimeRegistry;
import org.slj.mqtt.sn.client.impl.cli.MqttsnInteractiveClient;
import org.slj.mqtt.sn.client.impl.cli.MqttsnInteractiveClientLauncher;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.integrity.MqttsnProtectionService;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.spi.IMqttsnStorageService;
import org.slj.mqtt.sn.spi.IMqttsnTransport;

/**
 * @author Simon L Johnson
 */
public class ProtectionExampleClientCli {
    public static void main(String[] args) throws Exception {
        MqttsnInteractiveClientLauncher.launch(new MqttsnInteractiveClient() {
            protected AbstractMqttsnRuntimeRegistry createRuntimeRegistry(IMqttsnStorageService storageService, MqttsnOptions options, IMqttsnTransport transport) {
                AbstractMqttsnRuntimeRegistry registry = MqttsnClientRuntimeRegistry.defaultConfiguration(storageService, options).
                        withTransport(transport).
                        //-- Davide this is the place to bootstrap the instance into the runtime
                        withSecurityService(new MqttsnProtectionService());
                return registry;
            }
        });
    }
}
