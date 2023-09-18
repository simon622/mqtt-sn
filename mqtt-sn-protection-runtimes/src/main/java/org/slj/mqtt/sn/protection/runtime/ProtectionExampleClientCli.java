package org.slj.mqtt.sn.protection.runtime;

import org.slj.mqtt.sn.client.impl.MqttsnClientRuntimeRegistry;
import org.slj.mqtt.sn.client.impl.cli.MqttsnInteractiveClient;
import org.slj.mqtt.sn.client.impl.cli.MqttsnInteractiveClientLauncher;
import org.slj.mqtt.sn.codec.AbstractProtectionScheme;
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
public class ProtectionExampleClientCli {

    public static void main(String[] args) throws Exception {
        MqttsnInteractiveClientLauncher.launch(new MqttsnInteractiveClient() {
            protected AbstractMqttsnRuntimeRegistry createRuntimeRegistry(IMqttsnStorageService storageService, MqttsnOptions options, IMqttsnTransport transport) {
                options.withWireLoggingEnabled(true);

                //-- configure my protection details
//                MqttsnProtectionOptions protectionOptions =
//                        new MqttsnProtectionOptions().
//                                withProtectionScheme(AbstractProtectionScheme.HMAC_SHA256).
//                                withProtectionPacketFlags(new byte[] {(byte)0x03,(byte)0x00,(byte)0x00}).
//                                withProtectionKey(ProtectionUtils.loadKey("client1", "hmac"));

                MqttsnProtectionOptions protectionOptions =
                        new MqttsnProtectionOptions().
                                withProtectionScheme(AbstractProtectionScheme.AES_CCM_128_192).
                                withProtectionPacketFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02}).
                                withProtectionKey(ProtectionUtils.loadKey("client1", "aes192"));

                //-- configure the trusted sender
                ProtectedSender sender = new ProtectedSender("gateway1",
                        List.of(ProtectionUtils.loadKey("gateway1", "hmac")));
                IProtectedSenderRegistry protectedSenderRegistry =
                        new InMemoryProtectedSenderRegistry(List.of(sender), protectionOptions);

                options.withSecurityOptions(protectionOptions);

            	AbstractMqttsnRuntimeRegistry registry = MqttsnClientRuntimeRegistry.defaultConfiguration(storageService, options).
                        withTransport(transport).
                        withService(protectedSenderRegistry).
                        withSecurityService(new MqttsnProtectionService());
                return registry;
            }
        });
    }
}
