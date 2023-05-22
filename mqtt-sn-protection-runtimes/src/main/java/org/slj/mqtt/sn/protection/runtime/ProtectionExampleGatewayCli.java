package org.slj.mqtt.sn.protection.runtime;

//import org.slj.mqtt.sn.client.impl.MqttsnClientRuntimeRegistry;
//import org.slj.mqtt.sn.client.impl.cli.MqttsnInteractiveClient;
//import org.slj.mqtt.sn.client.impl.cli.MqttsnInteractiveClientLauncher;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.gateway.cli.MqttsnInteractiveGateway;
import org.slj.mqtt.sn.gateway.cli.MqttsnInteractiveGatewayLauncher;
import org.slj.mqtt.sn.gateway.impl.MqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.gateway.impl.connector.LoopbackMqttsnConnector;
import org.slj.mqtt.sn.gateway.impl.gateway.type.MqttsnAggregatingGateway;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
//import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayPerformanceProfile;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.protection.MqttsnProtectionService;
//import org.slj.mqtt.sn.model.MqttsnSecurityOptions;
import org.slj.mqtt.sn.spi.IMqttsnStorageService;
import org.slj.mqtt.sn.spi.IMqttsnTransport;

/**
 * @author Simon L Johnson
 */
public class ProtectionExampleGatewayCli {
    public static void main(String[] args) throws Exception {
        MqttsnInteractiveGatewayLauncher.launch(new MqttsnInteractiveGateway() {
            protected AbstractMqttsnRuntimeRegistry createRuntimeRegistry(IMqttsnStorageService storageService, MqttsnOptions options, IMqttsnTransport transport) {

                MqttsnConnectorOptions connectorOptions = new MqttsnConnectorOptions();
                IMqttsnStorageService namespacePreferences = storageService.getPreferenceNamespace(LoopbackMqttsnConnector.DESCRIPTOR);
                namespacePreferences.initializeFieldsFromStorage(connectorOptions);
                options.withAnonymousPublishAllowed(true);
                options.withWireLoggingEnabled(true);
                return MqttsnGatewayRuntimeRegistry.defaultConfiguration(storageService, (MqttsnGatewayOptions)options).
                		withConnector(new LoopbackMqttsnConnector(LoopbackMqttsnConnector.DESCRIPTOR, connectorOptions)).
                        withBackendService(new MqttsnAggregatingGateway()).
                        //-- Davide this is the place to bootstrap the instance into the runtime
                        withSecurityService(new MqttsnProtectionService()).
                        withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_2_0).
                        withTransport(createTransport(storageService));

            }
        }, false, "Welcome to the loopback gateway. This version does NOT use a backend broker, instead brokering MQTT messages itself as a loopback to connected devices.");
    }
}
