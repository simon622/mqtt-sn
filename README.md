
# MQTT For Small Things (SN)
MQTT-SN is an optimized version of the MQTT specification designed for use on small, low powered, sensor devices, often running on the edge of the network; typical of the IoT.

View the intial [MQTT-SN Version 1.2](http://www.mqtt.org/new/wp-content/uploads/2009/06/MQTT-SN_spec_v1.2.pdf) specification written by **Andy Stanford-Clark** and **Hong Linh Truong** from **IBM**.

### MQTT-SN Evolved
As of late 2020, the MQTT technical committee at OASIS (via a sub-committee led by **Ian Craggs** ([Ian's Blog](https://modelbasedtesting.co.uk))) are working on standardisation and changes to bring MQTT-SN more in line with MQTT version 5. 
This is an ongoing piece of work which we hope to formalise and conclude in 2021.

### Project Goals
Notable open-source works already exists for various MQTT and MQTT-SN components, the main ones of note are listed below; many fall under the eclipse PAHO project. The work by **Ian Craggs** et al on the MQTT-SN Java gateway set out the wire transport implementation and a reference transparent gateway. That is a gateway which connects a client to a broker side socket and mediates the access. My goal of this project and its work are that it should provide an open-source **aggregating gateway** implementation, and should implement the wire messages such that the next interation of MQTT-sn can be demonstrated using this project.

### MQTT / MQTT-SN differences
The SN variant of MQTT is an expression of the protocol using smaller messages, and an optimised message lifecycle. All the features of a core MQTT broker are available to the SN clients, with the gateway implementation hiding the complexities of the protocol using various multiplexing techniques. An SN client has no need of a TCP/IP connection to a broker, and can choose to any transport layer; for example UDP, BLE, Zigbee etc.

### Project modules
Module | Language & Build | Dependencies | Description
------------ | ------------- | ------------- | -------------
[mqtt-sn-codec](/mqtt-sn-codec) | Java 1.8, Maven | **Mandatory** | Pure java message parsers and writers. Includes interfaces and abstractions to support future versions of the protocol.
[mqtt-sn-core](/mqtt-sn-core) | Java 1.8, Maven | **Mandatory** | Shared interfaces and abstractions for use in the various MQTT-SN runtimes
[mqtt-sn-client](/mqtt-sn-client) | Java 1.8, Maven | Client | A lightweight client with example transport implementations. Exposes both a simple blocking API and an aysnc publish API to the application and hides the complexities of topic registrations and connection management.
[mqtt-sn-gateway](/mqtt-sn-gateway) | Java 1.8, Maven | Gateway | The core gateway runtime. The end goal is to provide all 3 variants of the gateway (Aggregating, Transparent & Forwarder) where possible. I have started with the aggregating gateway, since this is the most complex, and the most suitable for larger scale deployment.
[mqtt-sn-gateway-paho-connector](/mqtt-sn-gateway-paho-connector) | Java 1.8, Maven | Optional | Simple aggregating gateway using an out of the box PAHO connector to manage the TCP side

### Quick start - Gateway

Git checkout the repository. For a simple standalone jar execution, run the following maven deps.

```shell script
mvn -f mqtt-sn-codec clean install
mvn -f mqtt-sn-core clean install
mvn -f mqtt-sn-gateway clean install
mvn -f mqtt-sn-client clean install
mvn -f mqtt-sn-gateway-paho-connector clean package
```

This will yield a file in your mqtt-sn-gateway-paho-connector/target directory that will be called mqtt-sn-gateway-<version>.jar. You can then start a broker
from a command line using;

```shell script
java -jar <path-to>/mqtt-sn-gateway-<version>.jar
```

You can then follow the on screen instructions to get a gateway up and running.

### Quick start - Client

Ensure you have the requisite projects mounted per the table above. You can then run the main method for in the Example.java located in the project. You can see the configuration options for details
on how to customise your installation.

Click into [mqtt-sn-client](/mqtt-sn-client) for more details on the client.

### Configuration

The default client/gateway behaviour can be customised using configuration options. Sensible defaults have been specified which allow it to all work out of the box.
Many of the options below are applicable for both the client and gateway runtimes. 

Options | Default Value | Type | Description
------------ | ------------- | ------------- | -------------
contextId | NULL | String | This is used as either the clientId (when in a client runtime) or the gatewayId (when in a gateway runtime). **NB: This is a required field and must be set by the application.**
maxWait | 10000 | int | Time in milliseconds to wait for a confirmation message where required. When calling a blocking method, this is the time the method will block until either the confirmation is received OR the timeout expires.
maxTopicLength | 1024 | int | Maximum number of characters allowed in a topic including wildcard and separator characters.
threadHandoffFromTransport | true | boolean | Should the transport layer delegate to and from the handler layer using a thread hand-off. **NB: Depends on your transport implementation as to whether you should block.**
handoffThreadCount | 5 | int | How many threads are used to process messages received from the transport layer 
discoveryEnabled | false | boolean | When discovery is enabled the client will listen for broadcast messages from local gateways and add them to its network registry as it finds them.
maxTopicsInRegistry | 128 | int | Max number of topics which can reside in the CLIENT registry. This does NOT include predefined alias's.
msgIdStartAt | 1 | int (max. 65535) | Starting number for message Ids sent from the client to the gateways (each gateway has a unique count).
aliasStartAt | 1 | int (max. 65535) | Starting number for alias's used to store topic values (NB: only applicable to gateways).
maxMessagesInflight | 1 | int (max. 65535) | In theory, a gateway and broker can have multiple messages inflight concurrently. The spec suggests only 1 confirmation message is inflight at any given time. (NB: do NOT change this).
maxMessagesInQueue | 100 | int | Max number of messages allowed in a client's queue. When the max is reached any new messages will be discarded.
requeueOnInflightTimeout | true | boolean | When a publish message fails to confirm, should it be re-queued for DUP sending at a later point.
predefinedTopics | Config| Map | Where a client or gateway both know a topic alias in advance, any messages or subscriptions to the topic will be made using the predefined IDs. 
networkAddressEntries | Config | Map | You can prespecify known locations for gateways and clients in the network address registry. NB. The runtime will dynamically update the registry with new clients / gateways as they are discovered. In the case of clients, they are unable to connect or message until at least 1 gateway is defined in config OR discovered.
sleepClearsRegistrations  | true | boolean | When a client enters the ASLEEP state, should the NORMAL topic registered alias's be cleared down and reestablished during the next AWAKE or ACTIVE states.
minFlushTime  | 1000 | int | Time in milliseconds between a gateway device last receiving a message before it begins processing the client queue
discoveryTime  | 3600 | int | The time (in seconds) a client will wait for a broadcast during CONNECT before giving up
pingDivisor  | 4 | int | The divisor to use for the ping window, the dividend being the CONNECT keepAlive resulting in the quotient which is the time (since last sent message) each ping will be issued
maxProtocolMessageSize | 1024 | int | The max allowable size (in bytes) of protocol messages that will be sent or received by the system. **NB: this differs from transport level max sizes which will be determined and constrained by the MTU of the transport**

### Runtime Hooks

You can hook into the runtime and provide your own implementations of various components or bind in listeners to give you control or visibility onto aspects of the system.

#### Transport Implementations

You can very easily plug transport implementations into the runtime by hooking the transport layer

```java
    MqttsnClientRuntimeRegistry.defaultConfiguration(options).
        withTransport(new YourTransportLayerImplementation()).
        withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2);
```

#### Traffic Listeners

You can access all the data sent to and from the transport adapter by using traffic listeners.

```java
    MqttsnClientRuntimeRegistry.defaultConfiguration(options).
        withTransport(new MqttsnClientUdpTransport(udpOptions)).
        withTrafficListener(new IMqttsnTrafficListener() {
            @Override
            public void trafficSent(INetworkContext context, byte[] data, IMqttsnMessage message) {
                System.err.println(String.format("message [%s]", message));
            }

            @Override
            public void trafficReceived(INetworkContext context, byte[] data, IMqttsnMessage message) {
                System.err.println(String.format("message [%s]", message));
            }
        }).
        withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2);
```

### Related people & projects
Our goal on the [MQTT-SN technial committee](https://www.oasis-open.org/committees/tc_home.php?wg_abbrev=mqtt) is to drive and foster a thriving open-source community. Listed here are some related open-source projects with some comments.

Project | Author | Link | Description
------------ | ------------- | ------------- | -------------
Paho Mqtt C Client | Various | [GitHub Repository](https://github.com/eclipse/paho.mqtt.c) |Fully featured MQTT C client library
Paho Mqtt C Embedded | Various | [GitHub Repository](https://github.com/eclipse/paho.mqtt.embedded-c) | Fully featured embedded MQTT C client library
Paho Mqtt-Sn C Embedded | Various | [GitHub Repository](https://github.com/eclipse/paho.mqtt-sn.embedded-c) | C implementation of a transparent MQTT-SN gateway, client and codecs
Mqtt-sn Transparent Java Gateway | Ian Craggs & jsaak | [GitHub Repository](https://github.com/jsaak/mqtt-sn-gateway) | Java implementation of a transparent MQTT-SN gateway, c

### Aggregating gateway diagram

![MQTT-SN Aggregating Gateway Architecture](/images/mqttsn-arch.png)

