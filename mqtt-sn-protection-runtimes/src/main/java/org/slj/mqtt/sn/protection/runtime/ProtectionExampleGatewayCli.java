package org.slj.mqtt.sn.protection.runtime;

import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.console.MqttsnConsoleOptions;
import org.slj.mqtt.sn.console.impl.MqttsnConsoleService;
import org.slj.mqtt.sn.gateway.cli.MqttsnInteractiveGateway;
import org.slj.mqtt.sn.gateway.cli.MqttsnInteractiveGatewayLauncher;
import org.slj.mqtt.sn.gateway.impl.MqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.gateway.impl.connector.LoopbackMqttsnConnector;
import org.slj.mqtt.sn.gateway.impl.gateway.type.MqttsnAggregatingGateway;
import org.slj.mqtt.sn.gateway.spi.connector.MqttsnConnectorOptions;
import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.impl.AbstractMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.protection.impl.InMemoryProtectedSenderRegistry;
import org.slj.mqtt.sn.protection.impl.MqttsnProtectionService;
import org.slj.mqtt.sn.protection.impl.ProtectionUtils;
import org.slj.mqtt.sn.protection.spi.IProtectedSenderRegistry;
import org.slj.mqtt.sn.protection.spi.MqttsnProtectionOptions;
import org.slj.mqtt.sn.protection.spi.ProtectedSender;
import org.slj.mqtt.sn.spi.IMqttsnStorageService;
import org.slj.mqtt.sn.spi.IMqttsnTransport;

import java.util.List;

/**
 * @author Simon L Johnson
 */
public class ProtectionExampleGatewayCli {
    public static void main(String[] args) throws Exception {
        MqttsnInteractiveGatewayLauncher.launch(new MqttsnInteractiveGateway() {
            protected AbstractMqttsnRuntimeRegistry createRuntimeRegistry(IMqttsnStorageService storageService, MqttsnOptions options, IMqttsnTransport transport) {

                //-- configure my protection details
                MqttsnProtectionOptions protectionOptions =
                        new MqttsnProtectionOptions().
                                withProtectionPacketFlags(new byte[] {(byte)0x03,(byte)0x00,(byte)0x00}).
                                withProtectionKey(
                                    ProtectionUtils.loadKey("gateway1", "hmac"));

                //-- configure the trusted sender
                ProtectedSender sender = new ProtectedSender("client1",
                        List.of(ProtectionUtils.loadKey("client1", "hmac"),
                                ProtectionUtils.loadKey("client1", "aes128"),
                                ProtectionUtils.loadKey("client1", "aes192"),
                                ProtectionUtils.loadKey("client1", "aes256")));
                IProtectedSenderRegistry protectedSenderProvider =
                        new InMemoryProtectedSenderRegistry(List.of(sender), protectionOptions);

                MqttsnConnectorOptions connectorOptions = new MqttsnConnectorOptions();
                IMqttsnStorageService namespacePreferences = storageService.getPreferenceNamespace(LoopbackMqttsnConnector.DESCRIPTOR);
                namespacePreferences.initializeFieldsFromStorage(connectorOptions);
                options.withAnonymousPublishAllowed(true);
                options.withWireLoggingEnabled(true);
                options.withSecurityOptions(protectionOptions);

                MqttsnConsoleOptions console = new MqttsnConsoleOptions().
                        withConsoleEnabled(true);
                console.withConsolePort(8080);



                return MqttsnGatewayRuntimeRegistry.defaultConfiguration(storageService, (MqttsnGatewayOptions)options).
                		withConnector(new LoopbackMqttsnConnector(LoopbackMqttsnConnector.DESCRIPTOR, connectorOptions)).
                        withBackendService(new MqttsnAggregatingGateway()).
                        //-- Davide this is the place to bootstrap the instance into the runtime
                        withSecurityService(new MqttsnProtectionService()).
                        withService(protectedSenderProvider).
                        withService(new MqttsnConsoleService(console)).
                        withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_2_0).
                        withTransport(createTransport(storageService));
            }
        }, false, "Welcome to the loopback gateway. This version does NOT use a backend broker, instead brokering MQTT messages itself as a loopback to connected devices.");
    }
}
