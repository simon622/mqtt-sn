# mqtt-sn-codec
Full, dependency free java implementation of the MQTT-SN protocol specification. Abstraction of the underlying model to support pluggable wire protocols.

```java
  public class ExampleUsage {
      public static void main(String[] args) throws Exception {
  
          //-- select the version of the protocol you wish to use. The versions
          //-- registered on the interface are thread-safe, pre-constructed singletons.
          //-- you can also manually construct your codecs and manage their lifecycle if you so desire.
          IMqttsnCodec mqttsnVersion1_2 = MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2;
  
          //-- each codec supplies a message factory allowing convenient construction of messages for use
          //-- in your application.
          IMqttsnMessageFactory factory = mqttsnVersion1_2.createMessageFactory();
  
          //-- construct a connect message with your required configuration
          IMqttsnMessage connect =
                  factory.createConnect("testClientId", 60, false, true);
  
          //-- encode the connect message for wire transport...
          byte[] arr = mqttsnVersion1_2.encode(connect);
  
          //-- then on the other side of the wire..
          connect = mqttsnVersion1_2.decode(arr);
      }
  }
```