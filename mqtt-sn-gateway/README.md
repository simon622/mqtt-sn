# MQTT-SN Java Gateway
Full, dependency free java implementation of the MQTT-SN protocol specification for an aggregating gateway. 
Uses the mqtt-sn-codecs for wire transport and comes equip with UDP network transport by default. 
NOTE: As with all the modules in this project, the persistence, transport and wire traffic layer is entirely pluggable.

## Quick start
Configure your details using the code below and run Example.

```java
public class Example {
    public static void main(String[] args) throws Exception {
        if(args.length < 6)
            throw new IllegalArgumentException("you must specify 6 arguments; <localPort>, <clientId>, <host>, <port>, <username> and <password>");

        //-- the local port on which to listen
        int localPort = Integer.valueOf(args[0].trim());

        //-- the clientId of the MQTT broker you are connecting to
        String clientId = args[1].trim();

        //-- the host of the MQTT broker you are connecting to
        String host = args[2].trim();

        //-- the port of the (remote) MQTT broker you are connecting to
        int port = Integer.valueOf(args[3].trim());

        //-- the username of the MQTT broker you are connecting to
        String username = args[4].trim();

        //-- the password of the MQTT broker you are connecting to
        String password = args[5].trim();

        MqttsnBackendOptions brokerOptions = new MqttsnBackendOptions().
                withHost(host).
                withPort(port).
                withUsername(username).
                withPassword(password);

        //-- configure your gateway runtime
        MqttsnOptions gatewayOptions = new MqttsnGatewayOptions().
                withGatewayId(1).
                withMaxConnectedClients(100).
                withContextId(clientId).
                withPredefinedTopic("/my/example/topic/1", 1);

        //-- construct the registry of controllers and config
        AbstractMqttsnRuntimeRegistry registry = MqttsnGatewayRuntimeRegistry.defaultConfiguration(gatewayOptions).
                withBrokerConnectionFactory(new <Your-Connection-Type>MqttsnBrokerConnectionFactory()).
                withBrokerService(new MqttsnAggregatingGateway(brokerOptions)).
                withTransport(new MqttsnUdpTransport(new MqttsnUdpOptions().withPort(localPort))).
                withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2);

        MqttsnGateway gateway = new MqttsnGateway();

        //-- start the gateway and specify if you wish to join the main gateway thread (blocking) or
        //-- specify false to run async if you are embedding
        gateway.start(registry, true);
    }
}
```
