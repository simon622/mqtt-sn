
# MQTT For Small Things (SN)
MQTT-SN is an optimized version of the MQTT specification designed for use on small, low powered, sensor devices, often running on the edge of the network; typical of the IoT.

View the intial [MQTT-SN Version 1.2](http://www.mqtt.org/new/wp-content/uploads/2009/06/MQTT-SN_spec_v1.2.pdf) specification written by **Andy Stanford-Clark** and **Hong Linh Truong** from **IBM**.

## About

### MQTT-SN Evolved
As of late 2020, the MQTT technical committee at OASIS (via a sub-committee led by **Ian Craggs** ([Ian's Blog](https://modelbasedtesting.co.uk))) are working on standardisation and changes to bring MQTT-SN more in line with MQTT version 5. 
This is an ongoing piece of work which we hope to formalise and conclude in 2022.

### MQTT / MQTT-SN differences
The SN variant of MQTT is an expression of the protocol using smaller messages, and an optimised message lifecycle. All the features of a core MQTT broker are available to the SN clients, with the gateway implementation hiding the complexities of the protocol using various multiplexing techniques. An SN client has no need of a TCP/IP connection to a broker, and can choose to any transport layer; for example UDP, BLE, Zigbee etc.

### Project Goals
Notable open-source works already exists for various MQTT and MQTT-SN components, the main ones of note are listed below; many fall under the eclipse PAHO project. The work by **Ian Craggs** et al on the MQTT-SN Java gateway set out the wire transport implementation and a transparent gateway. That is a gateway which connects a client to a broker side socket and mediates the access. The goal of this project and its work are that it should provide an open-source **aggregating gateway** implementation to prove out **version 2** of the specification.

### Gateway system diagram
The system was built to be pluggable, to allow implementations to provide their own functionality and implementations where needed. Some aspects of the system are mandatory, others (for example AAA, Backend broker implementation etc) can be plugged in as required by a deployment.
![System Overview](/images/MQTT-SN-Aggregating-Gateway-Sys.png)

## Project

### Modules
Module | Language & Build | Dependencies | Description
------------ | ------------- | ------------- | -------------
[mqtt-sn-codec](/mqtt-sn-codec) | Java 1.8, Maven | **Mandatory** | Pure java message parsers and writers. Includes interfaces and abstractions to support future versions of the protocol.
[mqtt-sn-core](/mqtt-sn-core) | Java 1.8, Maven | **Mandatory** | Shared interfaces and abstractions for use in the various MQTT-SN runtimes
[mqtt-sn-client](/mqtt-sn-client) | Java 1.8, Maven | Client | A lightweight client with example transport implementations. Exposes both a simple blocking API and an aysnc publish API to the application and hides the complexities of topic registrations and connection management.
[mqtt-sn-gateway](/mqtt-sn-gateway) | Java 1.8, Maven | Gateway | The core gateway runtime. The end goal is to provide all 3 variants of the gateway (Aggregating, Transparent & Forwarder) where possible. I have started with the aggregating gateway, since this is the most complex, and the most suitable for larger scale deployment.
[mqtt-sn-gateway-connector-aws-iotcore](/mqtt-sn-gateway-connector-aws-iotcore) | Java 1.8, Maven | Optional | Connector to bind into AWS IoT Core using X.509 certs
[mqtt-sn-gateway-connector-google-iotcore](/mqtt-sn-gateway-connector-google-iotcore) | Java 1.8, Maven | Optional | Connector to bind to a Google IoT Core Gateway using pkcs8 (RS256) 
[mqtt-sn-gateway-connector-paho](/mqtt-sn-gateway-connector-paho) | Java 1.8, Maven | Optional | Simple aggregating gateway using an out of the box PAHO connector to manage the TCP side
[mqtt-sn-load-test](/mqtt-sn-load-test) | Java 1.8, Maven | Tools | Provides a runtime to spin up N clients and connect to a gateway instance and test concurrency and message throughput

### Quick Start Guide
I have created simple interactive command lines for both client and gateway components to allow simple testing and use. The interactive client and gateway both use preconfigured default runtimes which
can be used to evaluate / test the software. For more complex use, please refer to the source build and configuration.

#### Client CLI
The latest interactive client build can be obtained from the releases section. Download the mqtt-sn-client-VERSION.jar and run locally using;

```shell script
java -jar <path-to>/mqtt-sn-client-VERSION.jar
```

![Client CLI](/images/client-cli.png)

#### Gateway CLI
The latest interactive gateway build can be obtained from the releases section. Download the mqtt-sn-gateway-VERSION.jar and run locally using;

```shell script
java -jar <path-to>/mqtt-sn-gateway-VERSION.jar
```
![Gateway CLI](/images/gateway-cli.png)

### Build

#### Gateway Build

Git checkout the repository. For a simple standalone jar execution, run the following maven deps.

```shell script
mvn -f mqtt-sn-codec clean install
mvn -f mqtt-sn-core clean install
mvn -f mqtt-sn-gateway clean install
mvn -f mqtt-sn-client clean install
mvn -f mqtt-sn-gateway-connector-paho clean package
```

This will yield a file in your mqtt-sn-gateway-connector-paho/target directory that will be called mqtt-sn-gateway-<version>.jar. You can then start a broker
from a command line using;

```shell script
java -jar <path-to>/mqtt-sn-gateway-VERSION.jar
```
You can then follow the on screen instructions to get a gateway up and running.

#### Client Build

Git checkout the repository. For a simple standalone jar execution, run the following maven deps.

```shell script
mvn -f mqtt-sn-codec clean install
mvn -f mqtt-sn-core clean install
mvn -f mqtt-sn-client clean package
```

This will yield a file in your mqtt-sn-client/target directory that will be called mqtt-sn-gateway-<version>.jar. You can then start a broker
from a command line using;

```shell script
java -jar <path-to>/mqtt-sn-client-VERSION.jar
```
You can then follow the on screen instructions to get a client up and running.

## Runtime Hooks (Gateway & Client)

You can hook into the runtime and provide your own implementations of various components or bind in listeners to give you control or visibility onto different aspects of the system.

### Listeners

#### Publish Listeners

Application messages sent and received by the application can be accessed by registering publish listeners against
the runtime.

```java
    client.registerPublishReceivedListener((IMqttsnContext context, String topic, int qos, byte[] data, boolean retained) -> {
        // some custom code that will be called for every confirmed messages received
    });

    client.registerPublishSentListener((IMqttsnContext context, UUID messageId, String topic, int qos, byte[] data) -> {
        // some custom code that will be called for every confirmed messages sent
    });
```

#### Traffic Listeners

All traffic to and from the transport layer can be monitored by the application by registering traffic listeners. These will
get called back after the traffic has been sent / received to the transport adapter top enable you to see what is going over
the wire.

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

### Transport Implementations

You can very easily plug transport implementations into the runtime by hooking the transport layer

```java
    MqttsnClientRuntimeRegistry.defaultConfiguration(options).
        withTransport(new YourTransportLayerImplementation()).
        withCodec(MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2);
```

### Message Integrity

You can optionally configure the gateway and client to require and produce message verification on either all packets or payload data. If enabled you
must choose from HMAC (suggested) or CHECKSUM integrity checks. When enabled, data packets or payload will be prefixed with integrity fields
according to the chosen specification which will then be validated by the receiver. The available integrity options are listed below with
their respective lengths.


Type | Name | Field Size
------------ | ------------- | -------------
HMAC | MD5 | 16 Bytes
HMAC | SHA-1 | 20 Bytes
HMAC | SHA-224 | 28 Bytes
HMAC | SHA-256 | 32 Bytes
HMAC | SHA-384 | 48 Bytes
HMAC | SHA-512 | 64 Bytes
CHECKSUM | CRC-32 | 4 Bytes
CHECKSUM | Adler-32 | 4 Bytes

Use the following code to change the configuration on your runtime options.

```java
    MqttsnSecurityOptions securityOptions = new MqttsnSecurityOptions().
        withIntegrityType(MqttsnSecurityOptions.INTEGRITY_TYPE.hmac).               //hmac or checksum
        withIntegrityPoint(MqttsnSecurityOptions.INTEGRITY_POINT.protocol_messages). //should each message be integrity prefixed or just payload
        withIntegrityKey("my-pre-shared-key") //only used in HMAC

    options.withSecurityOptions(securityOptions);
```

### Benchmarking

I have run a limited set of benchmarks using the [mqtt-sn-load-test](/mqtt-sn-load-test) project. Benchmarking MQTT-SN is a little different than MQTT due to the constraint of only a single message
being inflight for a given client at any point in time, therefore running some of the scenarios that are used to benchmark MQTT is not comparable since the message inflight rule provides
an artificial bottleneck; further the round-trip latency is coupled to the latency of the backend broker. 

In a simple load-test environment on a single gateway instance running in loopback mode (no backend broker)[^1] on a local[^2] instance with 10,000 client connections I was able 
to achieve (after a round of performance tuning) over 400,000 [^3] outbound publish messages being delivered per second (at the same time as 5,500 inbound publish operations). See the grab below for results.

This was an informal load test, and I would encourage anyone who would like to take this further to share their results.

[^1]: Setting the gateway into loopback mode will short-circuit delivery out to a broker on the backand and treat the inbound messages as if they came back from subscriptions on the broker (essentially acting like a broker).
[^2]: Running the gateway on a local virtual machine would mean network latency is not a factor in these tests
[^3]: Achieved using a tuned configuration (120 delivery threads, 60 protocol threads, 2 general purpose threads, 2 queue processing threads) to match the expectation of the test profile (that is a large degree of message expansion cause by many clients subscribing to a single topic).

![Load Test Results](/images/peak-message-count.png)

This test should be re-run when time allows against a remote EC2 host.

### Clustering

The gateway runtime can be clustered. During connection establishment; the clustering service is notified of the connecting device. At this point, the implementation
is responsible for synchronising the state of previous sessions onto the local gateway. For more information about clustering support please contact me to discuss the
available options as the environment onto which the gateway is deployed impacts how clustering is achieved.

### Configuration

The default client/gateway behaviour can be customised using configuration options. Sensible defaults have been specified which allow it to all work out of the box.
Many of the options below are applicable for both the client and gateway runtimes.

Options | Default Value | Type | Description
------------ | ------------- | ------------- | -------------
contextId | NULL | String | This is used as either the clientId (when in a client runtime) or the gatewayId (when in a gateway runtime). **NB: This is a required field and must be set by the application.**
maxWait | 10000 | int | Time in milliseconds to wait for a confirmation message where required. When calling a blocking method, this is the time the method will block until either the confirmation is received OR the timeout expires.
transportHandoffThreadCount | 1 | int | How many threads are used to process messages received from the transport layer
queueProcessorThreadCount | 2 | int | How many threads should be used to process connected context message queues (should scale with the number of expected connected clients and the level of concurrency)
discoveryEnabled | false | boolean | When discovery is enabled the client will listen for broadcast messages from local gateways and add them to its network registry as it finds them.
maxTopicsInRegistry | 128 | int | Max number of topics which can reside in the CLIENT registry. This does NOT include predefined alias's.
msgIdStartAt | 1 | int (max. 65535) | Starting number for message Ids sent from the client to the gateways (each gateway has a unique count).
aliasStartAt | 1 | int (max. 65535) | Starting number for alias's used to store topic values (NB: only applicable to gateways).
maxMessagesInflight | 1 | int (max. 65535) | In theory, a gateway and broker can have multiple messages inflight concurrently. The spec suggests only 1 confirmation message is inflight at any given time. (NB: do NOT change this).
maxMessagesInQueue | 100 | int | Max number of messages allowed in a client's queue. When the max is reached any new messages will be discarded.
requeueOnInflightTimeout | true | boolean | When a publish message fails to confirm, should it be re-queued for DUP sending at a later point.
predefinedTopics | Config| Map | Where a client or gateway both know a topic alias in advance, any messages or subscriptions to the topic will be made using the predefined IDs.
networkAddressEntries | Config | Map | You can pre-specify known locations for gateways and clients in the network address registry. NB. The runtime will dynamically update the registry with new clients / gateways as they are discovered. In the case of clients, they are unable to connect or message until at least 1 gateway is defined in config OR discovered.
sleepClearsRegistrations  | true | boolean | When a client enters the ASLEEP state, should the NORMAL topic registered alias's be cleared down and reestablished during the next AWAKE or ACTIVE states.
minFlushTime  | 1000 | int | Time in milliseconds between a gateway device last receiving a message before it begins processing the client queue
discoveryTime  | 3600 | int | The time (in seconds) a client will wait for a broadcast during CONNECT before giving up
pingDivisor  | 4 | int | The divisor to use for the ping window, the dividend being the CONNECT keepAlive resulting in the quotient which is the time (since last sent message) each ping will be issued
maxProtocolMessageSize | 1024 | int | The max allowable size (in bytes) of protocol messages that will be sent or received by the system. **NB: this differs from transport level max sizes which will be determined and constrained by the MTU of the transport**

## Version 2.0
There were a number of changes considered for the standardisation process into V2.0. It is also worth noting a number of issues were discussed but NOT included, a breakdown of these can be found in the OASIS ticket system. My intention is to support both version 1.2 and version 2.0 on both the gateway and the client side. Below lists the changes between versions and the status of each change relating to its function in this repository.

#### Changelog ####
Below is a listing of the changes adopted for the standardisation work. There were also more general changes to the document text to add clarity to parts that may have been considered to be ambiguous or to draw more alignment to MQTT version 5.0. 

Ref | Title | Description | Change Type | Implemented 
------------ | ------------- | ------------- | ------------- | ------------ 
539 | Remove topicId from PUBACK packet | **topicId** being named and present in PUBACK was causing confusion and was not needed. | Packet | :heavy_check_mark:
540 | Clarify max size of small message types | Wording change to indicate that the size of small messages is 0-255 (< 256) octets inclusive. | Descriptive | :heavy_check_mark: 
541 | Clean session should act like MQTT 5.0 | **cleanSession** changed to **cleanStart** and **sessionExpiryInterval** introduced to CONNECT packet. | Functional | :heavy_check_mark:
542 | Allow zero length clientId in CONNECT | The client should be able to pass a zero length clientId in the CONNECT packet. This indicates it should be assigned a clientId by the gateway. This **assignedClientId** should be sent back by the gateway as part of the CONNACK packet | Functional & Packet | :heavy_check_mark:
543 | Allow long topic names in PUBLISH for all QoS | The client should be able to pass a full topic name into the PUBLISH packet. Add a new **topicIdType** 0b11 to identify it. | Functional & Packet  | 
544 | Add **returnCode** to UNSUBACK | Specify **returnCode** on the UNSUBACK packet | Functional & Packet | :heavy_check_mark:
545 | Ensure zero byte retained PUBLISH clears RETAINED messages | Align functional description of zero byte retained PUBLISH messages with MQTT 5.0. | Functional & Descriptive | :heavy_check_mark:
546 | Improve description of sleeping client packet workflow | Clarity added to sleeping client description and sequence diagrams. | Descriptive | :heavy_check_mark:
550 | Mandate the use of separate namespaces for device normal topic alias | Each device should have its own normal topicId space, distinct from other device's normal topicId space. | Functional & Descriptive | :heavy_check_mark:
552 | Clarify that gateways should accept QoS -1 messages from sleeping clients | A device should be able to publish at QoS -1 from any state, including sleeping. This should be made clearer in the specification. | Descriptive | :heavy_check_mark:
553 | Clarify the lifecycle of normal topicIds | Topic Alias mappings exist only while a client is active and last for the entire duration of the active state.  A receiver MUST NOT carry forward any Topic Alias mappings from one active state to another. | Descriptive & Functional | :heavy_check_mark:
554 | Sleeping state message buffering | Clarify that the gateway can choose to buffer QoS 0 messages during sleep state | Descriptive | :heavy_check_mark:
555 | Clarify gateway behaviour when REGISTER called for a PREDEFINED topic | Adding the **topicIdType** type to a REGACK to allow the gateway to inform clients of topic for which a PREDEFINED mapping already exists | Descriptive & Functional | :heavy_check_mark:
558 | Clarify small topic name format | Need an explicit definition of 2 octet (16 bit) ints | Descriptive | :heavy_check_mark:
559 | Increase duration on DISCONNECT | Increase the duration a client can sleep for from 16 bit to 32 bit. Change from **duration** to **sessionExpiryInterval**  | Descriptive & Functional & Packet | :heavy_check_mark:
560 | Align error codes with MQTT 5.0 where appropriate | Increase the number of error codes in use and provide reason strings where appropriate  | Descriptive & Packet |
561 | Improve ping flush operation | Add **maxMessages** field to PINGREQ and **messagesRemaining** to PINGRESP to allow client to only retrieve some of their buffered messages before returning to sleep | Descriptive & Functional & Packet |
568 | Authentication | Add **auth** field to CONNECT and create SASL based authentication workflow | Functional & Packet & Descriptive |
570 | Align terminology with MQTT 5.0 | Message becomes application message or packet, topic name become topic name or topic filter, error code becomes reason code | Descriptive | :heavy_check_mark:
572 | Constrain MQTT-SN to only 1 inflight message in each direction | Update text to mandate that a client may only have a single message inflight in each direction at any given time | Descriptive & Functional | :heavy_check_mark:
573 | Subscription Options | Add **noLocal**, **retainAsPublished** and **retainHandling** to SUBSCRIBE packet | Descriptive & Functional & Packet |
574 | Add reason string to DISCONNECT | In conjunction to 560, add a textual representation to the DISCONNECT packet | Functional & Packet | :heavy_check_mark:
575 | New protocol version | Add a new protocol version | Functional & Descriptive | :heavy_check_mark:
576 | Separate the flags field for CONNECT, SUBSCRIBE & PUBLISH | The flags field is no longer aligned across packet types so each packet needs its own description | Descriptive & Packet | :heavy_check_mark:
581 | Add **sessionExpiryInterval** to CONNACK | The gateway can choose to change the value of the session expiry by sending the value | Functional & Packet | :heavy_check_mark:
582 | Add **maxPacketSize** to CONNECT | The client should be able to specify a max packet size they will be able to receive | Functional & Packet | :heavy_check_mark:
585 | Add **sessionPresent** indicator to CONNACK | The gateway should be able to communicate back to the client whether a session was held locally during non cleanStart | Functional & Packet | :heavy_check_mark:

#### Packet Types Affected By v2.0 Changes ####
Packet Name | Change Type
------------ | -------------
AUTH | NEW
CONNACK | MODIFIED
CONNECT | MODIFIED
PINGREQ | MODIFIED
PINGRESP | MODIFIED
PUBACK | MODIFIED
PUBLISH | MODIFIED
REGACK | MODIFIED
SUBACK | MODIFIED
SUBSCRIBE | MODIFIED
UNSUBSCRIBE | MODIFIED
UNSUBACK | MODIFIED

### Related people & projects
Our goal on the [MQTT-SN technical committee](https://www.oasis-open.org/committees/tc_home.php?wg_abbrev=mqtt) is to drive and foster a thriving open-source community. Listed here are some related open-source projects with some comments. If you would like me to include your repository below, please issue a Pull Request and add it.

Project | Author | Link | Description | Client | Gateway | Version 1.2 | Version 2.0
------------ | ------------- | ------------- | ------------- | ------------ | ------------- | ------------- | -------------
Org SLJ Mqtt-Sn Java Gateway & Client  | Simon Johnson | [GitHub Repository](https://github.com/simon622/mqtt-sn) | Dependency free, pluggable, lightweight pure Java Mqtt-Sn. Includes client and gateway implementations with both 1.2 and 2.0 support. Contains codecs, client, gateway, cloud connectors & load testing utilities | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:
Paho Mqtt-Sn C Embedded | Various | [GitHub Repository](https://github.com/eclipse/paho.mqtt-sn.embedded-c) | C implementation of a transparent MQTT-SN gateway, client and codecs | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_multiplication_x:
Mqtt-sn Transparent Java Gateway | Ian Craggs & jsaak | [GitHub Repository](https://github.com/jsaak/mqtt-sn-gateway) | Java implementation of a transparent MQTT-SN gateway | :heavy_multiplication_x: | :heavy_check_mark: | :heavy_check_mark: | :heavy_multiplication_x:
Paho Mqtt Java Client | Various | [GitHub Repository](https://github.com/eclipse/paho.mqtt.java) | Fully featured MQTT Java client library | N/A | N/A | N/A | N/A
Paho Mqtt C Client | Various | [GitHub Repository](https://github.com/eclipse/paho.mqtt.c) | Fully featured MQTT C client library | N/A | N/A | N/A | N/A
Paho Mqtt C Embedded | Various | [GitHub Repository](https://github.com/eclipse/paho.mqtt.embedded-c) | Fully featured embedded MQTT C client library | N/A | N/A | N/A | N/A


