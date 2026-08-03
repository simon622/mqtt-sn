# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

MQTT-SN ("MQTT for Small Things") is a Java implementation of the OASIS MQTT-SN protocol: a pure-Java, dependency-free codec plus pluggable client and aggregating-gateway runtimes. It supports both wire protocol v1.2 and v2.0. Java 8, Maven multi-module build.

## Build & test

```shell script
mvn clean install               # build all modules, from repo root (cascades to all modules)
mvn -pl <module> -am install    # build a single module and its dependencies, e.g. -pl mqtt-sn-core -am
mvn test -pl <module>           # run tests for one module only
mvn test -pl <module> -Dtest=ClassName          # run a single test class
mvn test -pl <module> -Dtest=ClassName#method   # run a single test method
```

Standard JUnit 4 tests live under each module's `src/test/java`. Notable ones:
- `mqtt-sn-codec`: `Mqttsn1_2WireTests`, `Mqttsn2_0WireTests` — wire encode/decode round-trip tests per protocol version.
- `mqtt-sn-core`: `InflightMessageStateRaceConditionTest`, `IntegrityTests`, `SubscriptionTests` — use `MqttsnTestRuntime`/`MqttsnTestRuntimeRegistry` as an in-VM test harness that wires up client+gateway runtimes without real network transport.
- `mqtt-sn-client`: `ClientConnectionTest`.

There are also untracked scratch runtime folders under `mqtt-sn-core/mqtt-sn-runtimes/mqtt-sn-race-test-*` used for manually reproducing/verifying concurrency races (connect race conditions, inflight message state) outside of the JUnit suite — these are runtime config/working directories, not modules with their own `pom.xml`.

`mvn clean install` on the root `pom.xml` produces runnable shaded jars:
- `mqtt-sn-gateway-console/target/mqtt-sn-gateway-console-<version>.jar` — run with `java -jar ... <listenPort> <gatewayId>`, admin console on `:8080` (admin/password by default).
- `mqtt-sn-client/target/mqtt-sn-client-<version>.jar` — interactive client CLI (`ClientInteractiveMain`).

## Module map

- `mqtt-sn-codec` — **mandatory**. Pure-Java wire parsers/writers for both protocol versions (`wire/version1_2`, `wire/version2_0`), the `IMqttsnMessage`/`IMqttsnCodec`/`IMqttsnMessageFactory` abstractions, and `MqttsnConstants` (packet type IDs, QoS values, etc). No dependency on `mqtt-sn-core`.
- `mqtt-sn-core` — **mandatory**. Shared runtime engine used by both client and gateway: the service/registry framework (`spi/IMqttsnRuntimeRegistry`, `impl/AbstractMqttsnRuntime`, `impl/AbstractMqttsnRuntimeRegistry`), transport abstraction (`spi/IMqttsnTransport`, `impl/AbstractMqttsnTransport`, `impl/AbstractMqttsnUdpTransport`), session/topic/subscription/message-queue/will registries and their in-memory (`impl/ram`) implementations, message state machine (`IMqttsnMessageStateService`), security service, metrics, and the `org.slj.mqtt.tree` topic-matching trie used for subscription matching.
- `mqtt-sn-client` — lightweight client with example UDP transport. Exposes both a blocking API and an async publish API; hides topic registration/connection-management complexity. `MqttsnClientRuntimeRegistry` is the fluent entry point (`.withTransport(...).withCodec(...)`).
- `mqtt-sn-gateway` — the aggregating gateway runtime (`MqttsnGatewayRuntimeRegistry`). Backend/connector/bridge abstractions live under `gateway/spi` and `gateway/impl`; concrete backend connectors are separate modules.
- `mqtt-sn-gateway-connector-aws-iotcore`, `mqtt-sn-gateway-connector-paho` — optional backend connector implementations bound into the gateway (AWS IoT Core via X.509, and a plain PAHO/TCP MQTT connector respectively).
- `mqtt-sn-gateway-console` — packages the gateway with a bootstrap web admin console; produces the standalone runnable jar.
- `mqtt-sn-cloud-client` — HTTP client used to pull cloud-hosted config/modules.
- `mqtt-sn-load-test` — spins up N clients against a gateway to test throughput/concurrency.
- `mqtt-sn-protection` / `mqtt-sn-protection-runtimes` — message integrity/protection scheme (HMAC/CHECKSUM) implementations and their runtime tests.
- `site` — project website content (not application code).

## Architecture notes

- **Runtime = registry of services.** Both the client and the gateway are assembled the same way: a `*RuntimeRegistry` (fluent builder, e.g. `MqttsnClientRuntimeRegistry.defaultConfiguration(options).withTransport(...).withCodec(...)`) wires together an `AbstractMqttsnRuntime`, a set of `IMqttsnService` implementations (registries, state services, security, metrics), and a transport. Custom behavior (auth, transport, payload modification, listeners) is injected by supplying alternative service implementations to the registry rather than subclassing the runtime.
- **Codec is version-pluggable.** `mqtt-sn-codec` defines version-agnostic interfaces (`IMqttsnMessage`, `IMqttsnCodec`) with concrete `wire/version1_2` and `wire/version2_0` implementations selected via `MqttsnCodecs`. When touching protocol packet handling, check whether behavior needs to differ between v1.2 and v2.0 semantics (see the "Version 2.0" changelog in `README.md` for what changed and why).
- **Protocol invariant:** only a single message may be in flight per direction per client at any time (`maxMessagesInflight`, do not change) — this shapes the message state machine (`IMqttsnMessageStateService`) and is a deliberate constraint of the MQTT-SN spec, not an oversight.
- **In-memory reference implementations live under `impl/ram`** in `mqtt-sn-core` (message queue, registries, dead letter queue, will registry) — these back the default configuration and are what the JUnit test harness (`MqttsnTestRuntimeRegistry`) uses.
- **Gateway backend is pluggable via connectors.** The gateway itself is transport/broker-agnostic; `gateway/spi/connector` defines the contract that `mqtt-sn-gateway-connector-aws-iotcore` and `mqtt-sn-gateway-connector-paho` implement to bridge to a real MQTT broker/cloud service.
- **Configuration** is done via the options objects (e.g. `MqttsnOptions`) passed into the runtime registry, and every option can be overridden with a matching `-D<optionName>=...` system property at runtime (see the Configuration table in `README.md`).
- Java source/target level is 1.8 across all modules — do not use newer language features without checking `maven.compiler.source`/`target` in the relevant `pom.xml`.
