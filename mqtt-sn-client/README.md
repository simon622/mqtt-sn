# MQTT-SN Java Client
Full, dependency free java implementation of the MQTT-SN protocol specification for a client. 
Uses the mqtt-sn-codecs for wire transport and comes equip with UDP network transport by default. 
NOTE: As with all the modules in this project, the persistence, transport and wire traffic layer is entirely pluggable.

## Quick start
Configure your details using the code below and run Example.

```java
public class Example {
    public static void main(String[] args) throws Exception {

        //-- use the client transport options, which will use random unallocated local ports
        MqttsnUdpOptions udpOptions = new MqttsnClientUdpOptions();

        //-- runtimes options can be used to tune the behaviour of the client
        MqttsnOptions options = new MqttsnOptions().
                //-- specify the address of any static gateway nominating a context id for it
                        withNetworkAddressEntry("gatewayId", NetworkAddress.localhost(MqttsnUdpOptions.DEFAULT_LOCAL_PORT)).
                //-- configure your clientId
                        withContextId("clientId1").
                //-- specify and predefined topic Ids that the gateway will know about
                        withPredefinedTopic("my/predefined/example/topic/1", 1);

        //-- using a default configuration for the controllers will just work out of the box, alternatively
        //-- you can supply your own implementations to change underlying storage or business logic as is required
        AbstractMqttsnRuntimeRegistry registry = MqttsnClientRuntimeRegistry.defaultConfiguration(options).
                withTransport(new MqttsnUdpTransport(udpOptions)).
                //-- select the codec you wish to use, support for SN 1.2 is standard or you can nominate your own
                        withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2);

        AtomicInteger receiveCounter = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);

        //-- the client is Closeable and so use a try with resource
        try (MqttsnClient client = new MqttsnClient()) {

            //-- the client needs to be started using the configuration you constructed above
            client.start(registry);

            //-- register any publish receive listeners you require
            client.registerReceivedListener((IMqttsnContext context, String topic, int qos, byte[] data) -> {
                receiveCounter.incrementAndGet();
                System.err.println(String.format("received message [%s] [%s]",
                        receiveCounter.get(), new String(data, MqttsnConstants.CHARSET)));
                latch.countDown();
            });

            //-- register any publish sent listeners you require
            client.registerSentListener((IMqttsnContext context, UUID messageId, String topic, int qos, byte[] data) -> {
                System.err.println(String.format("sent message [%s]",
                        new String(data, MqttsnConstants.CHARSET)));
            });

            //-- issue a connect command - the method will block until completion
            client.connect(360, true);

            //-- issue a subscribe command - the method will block until completion
            client.subscribe("my/example/topic/1", 2);

            //-- issue a publish command - the method will queue the message for sending and return immediately
            client.publish("my/example/topic/1", 1,  "hello world".getBytes());

            //-- wait for the sent message to be looped back before closing
            latch.await(30, TimeUnit.SECONDS);

            //-- issue a disconnect command - the method will block until completion
            client.disconnect();
        }
    }
}
```
