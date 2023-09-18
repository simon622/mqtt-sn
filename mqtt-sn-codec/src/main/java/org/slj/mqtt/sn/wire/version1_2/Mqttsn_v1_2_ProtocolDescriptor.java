package org.slj.mqtt.sn.wire.version1_2;

import org.slj.mqtt.sn.descriptor.FieldDescriptor;
import org.slj.mqtt.sn.descriptor.FlagsDescriptor;
import org.slj.mqtt.sn.descriptor.PacketDescriptor;
import org.slj.mqtt.sn.descriptor.ProtocolDescriptor;

import java.nio.channels.NonReadableChannelException;

/**
 * @author Simon L Johnson
 */
public class Mqttsn_v1_2_ProtocolDescriptor extends ProtocolDescriptor  {

    public static final ProtocolDescriptor INSTANCE = new Mqttsn_v1_2_ProtocolDescriptor();

    private Mqttsn_v1_2_ProtocolDescriptor() {
        super("Mqtt-Sn Version 1.2", "An expression of MQTT designed for low-power, constrained devices.");
        initPacketDescriptors();
    }

    protected void initPacketDescriptors(){

        initAdvertise();
        initSearchGw();
        initGwInfo();
        initConnect();
        initConnack();
        initWillTopicReq();
        initWillTopic();
        initWillMsgReq();
        initWillMsg();
        initRegister();
        initRegack();
        initPublish();
        initPuback();
        initPubcomp();
        initPubrec();
        initPubrel();
        initSubscribe();
        initSuback();
        initUnsubscribe();
        initUnsuback();
        initPingreq();
        initPingresp();
        initDisconnect();
        initWilltopicUpd();
        initWilltopicResp();
        initWillmsgUpd();
        initWillmsgResp();
        initEncapsulatedMessage();
    }

    protected void initAdvertise(){
        PacketDescriptor descriptor = createPacketDescriptor("Advertise", "The ADVERTISE message is broadcasted periodically by a gateway to advertise its presence. The time interval until the next broadcast time is indicated in the Duration field of this message.", 0x00);
        descriptor.addFieldDescriptor(new FieldDescriptor("GwId","The id of the gateway which sends this message."));
        descriptor.addFieldDescriptor(new FieldDescriptor("Duration","Time interval until the next ADVERTISE is broadcast by this gateway"));
        addPacketDescriptor(descriptor);
    }

    protected void initSearchGw(){
        PacketDescriptor descriptor = createPacketDescriptor("SearchGw", "The SEARCHGW message is broadcast by a client when it searches for a GW. The broadcast radius of the SEARCHGW is limited and depends on the density of the clients deployment, e.g. only 1-hop broadcast in case of a very dense network in which every MQTT-SN client is reachable from each other within 1-hop transmission.", 0x01);
        descriptor.addFieldDescriptor(new FieldDescriptor("Radius","The broadcast radius of this message."));
        descriptor.setNotes("The broadcast radius is also indicated to the underlying network layer when MQTT-SN gives this message for transmission.");
        addPacketDescriptor(descriptor);
    }

    protected void initGwInfo(){
        PacketDescriptor descriptor = createPacketDescriptor("GwInfo", "The GWINFO message is sent as response to a SEARCHGW message using the broadcast service of the underlying layer, with the radius as indicated in the SEARCHGW message. If sent by a GW, it contains only the id of the sending GW; otherwise, if sent by a client, it also includes the address of the GW.", 0x02);
        descriptor.addFieldDescriptor(new FieldDescriptor("GwId","The id of a GW"));
        descriptor.addFieldDescriptor(new FieldDescriptor("GwAdd","Address of the indicated GW; optional, only included if message is sent by a client.").setLength(Integer.MAX_VALUE));
        descriptor.setNotes("Like the SEARCHGW message the broadcast radius for this message is also indicated to the underlying\n" +
                "network layer when MQTT-SN gives this message for transmission.");
        addPacketDescriptor(descriptor);
    }

    protected void initConnect(){
        PacketDescriptor descriptor = createPacketDescriptor("Connect", "The CONNECT message is sent by a client to setup a connection.", 0x04);
        descriptor.addFieldDescriptor(createFlagsField("Flags"));
        descriptor.addFieldDescriptor(new FieldDescriptor("ProtocolId","Corresponds to the “ProtocolName” and “ProtocolVersion” of the MQTT CONNECT message"));
        descriptor.addFieldDescriptor(new FieldDescriptor("Duration","Same as with MQTT, contains the value of the Keep Alive timer."));
        descriptor.addFieldDescriptor(new FieldDescriptor("ClientId","Same as with MQTT, contains the client id which is a 1-23 character long string which uniquely identifies the client to the server.").setLength(Integer.MAX_VALUE));
        addPacketDescriptor(descriptor);
    }

    protected void initConnack(){
        PacketDescriptor descriptor = createReturnCodePacketDescriptor("Connack", "The CONNACK message is sent by the server in response to a connection request from a client.", 0x05);
        addPacketDescriptor(descriptor);
    }

    protected void initWillTopicReq(){
        PacketDescriptor descriptor = createPacketDescriptor("WillTopicReq", "The WILLTOPICREQ message is sent by the GW to request a client for sending the Will topic name.", 0x06);
        addPacketDescriptor(descriptor);
    }

    protected void initWillTopic(){
        PacketDescriptor descriptor = createPacketDescriptor("WillTopic", "The WILLTOPIC message is sent by a client as response to the WILLTOPICREQ message for transferring its Will topic name to the GW.", 0x07);
        descriptor.addFieldDescriptor(createFlagsField("Flags"));
        descriptor.addFieldDescriptor(new FieldDescriptor("WillTopic","Contains the Will topic name.").setLength(Integer.MAX_VALUE));
        descriptor.setNotes("An empty WILLTOPIC message is a WILLTOPIC message without Flags and WillTopic field (i.e. it is exactly 2 octets long). It is used by a client to delete the Will topic and the Will message stored in the server.");
        addPacketDescriptor(descriptor);
    }

    protected void initWillMsgReq(){
        PacketDescriptor descriptor = createPacketDescriptor("WillMsgReq", "The WILLMSGREQ message is sent by the GW to request a client for sending the Will message.", 0x08);
        addPacketDescriptor(descriptor);
    }

    protected void initWillMsg(){
        PacketDescriptor descriptor = createPacketDescriptor("WillMsg", "The WILLMSG message is sent by a client as response to a WILLMSGREQ for transferring its Will message to the GW.", 0x09);
        descriptor.addFieldDescriptor(new FieldDescriptor("WillMsg","Contains the Will message.").setLength(Integer.MAX_VALUE));
        addPacketDescriptor(descriptor);
    }

    protected void initRegister(){
        PacketDescriptor descriptor = createPacketDescriptor("Register", "The REGISTER message is sent by a client to a GW for requesting a topic id value for the included topic name. It is also sent by a GW to inform a client about the topic id value it has assigned to the included topic name.", 0x0A);
        descriptor.addFieldDescriptor(new FieldDescriptor("TopicId","If sent by a client, it is coded 0x0000 and is not relevant; if sent by a GW, it contains the topic id value assigned to the topic name included in the TopicName field.").setLength(2));
        descriptor.addFieldDescriptor(new FieldDescriptor("MsgId","Should be coded such that it can be used to identify the corresponding REGACK message").setLength(2));
        descriptor.addFieldDescriptor(new FieldDescriptor("TopicName","Contains the topic name").setLength(Integer.MAX_VALUE));
        addPacketDescriptor(descriptor);
    }

    protected void initRegack(){
        PacketDescriptor descriptor = createPacketDescriptor("Regack", "The REGACK message is sent by a client or by a GW as an acknowledgment to the receipt and processing of a REGISTER message.", 0x0B);
        descriptor.addFieldDescriptor(new FieldDescriptor("TopicId","The value that shall be used as topic id in the PUBLISH messages").setLength(2));
        descriptor.addFieldDescriptor(new FieldDescriptor("MsgId","Same value as the one contained in the corresponding REGISTER message").setLength(2));
        descriptor.addFieldDescriptor(new FieldDescriptor("ReturnCode","“Accepted”, or rejection reason"));
        addPacketDescriptor(descriptor);
    }

    protected void initPublish(){
        PacketDescriptor descriptor = createPacketDescriptor("Publish", "This message is used by both clients and gateways to publish data for a certain topic.", 0x0C);
        descriptor.addFieldDescriptor(createFlagsField("Flags"));
        descriptor.addFieldDescriptor(new FieldDescriptor("TopicId","Contains the topic id value or the short topic name for which the data is published.").setLength(2));
        descriptor.addFieldDescriptor(new FieldDescriptor("MsgId","MsgId: same meaning as the MQTT “Message ID”; only relevant in case of QoS levels 1 and 2, otherwise coded 0x0000.").setLength(2));
        descriptor.addFieldDescriptor(new FieldDescriptor("Data","The published data").setLength(Integer.MAX_VALUE).setOptional(true));
        addPacketDescriptor(descriptor);
    }

    protected void initPuback(){
        PacketDescriptor descriptor = createPacketDescriptor("Puback", "The PUBACK message is sent by a gateway or a client as an acknowledgment to the receipt and processing of a PUBLISH message in case of QoS levels 1 or 2. It can also be sent as response to a PUBLISH message in case of an error; the error reason is then indicated in the ReturnCode field.", 0x0D);
        descriptor.addFieldDescriptor(new FieldDescriptor("TopicId","Same value the one contained in the corresponding PUBLISH message.").setLength(2));
        descriptor.addFieldDescriptor(new FieldDescriptor("MsgId","Same value as the one contained in the corresponding PUBLISH message.").setLength(2));
        descriptor.addFieldDescriptor(new FieldDescriptor("ReturnCode","“Accepted”, or rejection reason"));
        addPacketDescriptor(descriptor);
    }

    protected void initPubcomp(){
        PacketDescriptor descriptor = createPacketDescriptor("Pubcomp", "As with MQTT, the PUBREC, PUBREL, and PUBCOMP messages are used in conjunction with a PUBLISH message with QoS level 2.", 0x0E);
        descriptor.addFieldDescriptor(new FieldDescriptor("MsgId","Same value as the one contained in the corresponding PUBLISH message.").setLength(2));
        addPacketDescriptor(descriptor);
    }

    protected void initPubrec(){
        PacketDescriptor descriptor = createPacketDescriptor("Pubrec", "As with MQTT, the PUBREC, PUBREL, and PUBCOMP messages are used in conjunction with a PUBLISH message with QoS level 2.", 0x0F);
        descriptor.addFieldDescriptor(new FieldDescriptor("MsgId","Same value as the one contained in the corresponding PUBLISH message.").setLength(2));
        addPacketDescriptor(descriptor);
    }

    protected void initPubrel(){
        PacketDescriptor descriptor = createPacketDescriptor("Pubrel", "As with MQTT, the PUBREC, PUBREL, and PUBCOMP messages are used in conjunction with a PUBLISH message with QoS level 2.", 0x10);
        descriptor.addFieldDescriptor(new FieldDescriptor("MsgId","Same value as the one contained in the corresponding PUBLISH message.").setLength(2));
        addPacketDescriptor(descriptor);
    }

    protected void initSubscribe(){
        PacketDescriptor descriptor = createPacketDescriptor("Subscribe", "The SUBSCRIBE message is used by a client to subscribe to a certain topic name.", 0x12);
        descriptor.addFieldDescriptor(createFlagsField("Flags"));
        descriptor.addFieldDescriptor(new FieldDescriptor("MsgId","Should be coded such that it can be used to identify the corresponding SUBACK message").setLength(2));
        descriptor.addFieldDescriptor(new FieldDescriptor("TopicId or TopicName","Contains topic name, topic id, or short topic name as indicated in the TopicIdType field.").setLength(2));
        addPacketDescriptor(descriptor);
    }

    protected void initSuback(){
        PacketDescriptor descriptor = createPacketDescriptor("Suback", "The SUBACK message is sent by a gateway to a client as an acknowledgment to the receipt and processing of a SUBSCRIBE message.", 0x13);
        descriptor.addFieldDescriptor(createFlagsField("Flags"));
        descriptor.addFieldDescriptor(new FieldDescriptor("TopicId","In case of “accepted” the value that will be used as topicid by the gateway when sending PUBLISH messages to the client (not relevant in case of subscriptions to a short topic name or to a topic name which contains wildcard characters)").setLength(2));
        descriptor.addFieldDescriptor(new FieldDescriptor("MsgId","Should be coded such that it can be used to identify the corresponding SUBACK message").setLength(2));
        descriptor.addFieldDescriptor(new FieldDescriptor("ReturnCode","“Accepted”, or rejection reason"));
        addPacketDescriptor(descriptor);
    }

    protected void initUnsubscribe(){
        PacketDescriptor descriptor = createPacketDescriptor("Unsubscribe", "An UNSUBSCRIBE message is sent by the client to the GW to unsubscribe from named topics.", 0x14);
        descriptor.addFieldDescriptor(createFlagsField("Flags"));
        descriptor.addFieldDescriptor(new FieldDescriptor("MsgId","Should be coded such that it can be used to identify the corresponding UNSUBACK message").setLength(2));
        descriptor.addFieldDescriptor(new FieldDescriptor("TopicId or TopicName","Contains topic name, topic id, or short topic name as indicated in the TopicIdType field.").setLength(2));
        addPacketDescriptor(descriptor);
    }

    protected void initUnsuback(){
        PacketDescriptor descriptor = createPacketDescriptor("Unsuback", "An UNSUBACK message is sent by a GW to acknowledge the receipt and processing of an UNSUBSCRIBE message.", 0x15);
        descriptor.addFieldDescriptor(new FieldDescriptor("MsgId","Same value as the one contained in the corresponding UNSUBSCRIBE message.").setLength(2));
        addPacketDescriptor(descriptor);
    }

    protected void initPingreq(){
        PacketDescriptor descriptor = createPacketDescriptor("Pingreq", "As with MQTT, the PINGREQ message is an ”are you alive” message that is sent from or received by a connected client.", 0x16);
        descriptor.addFieldDescriptor(new FieldDescriptor("ClientId","Contains the client id; this field is optional and is included by a “sleeping” client when it goes to the “awake” state and is waiting for messages sent by the server/gateway.").setLength(Integer.MAX_VALUE).setOptional(true));
        addPacketDescriptor(descriptor);
    }

    protected void initPingresp(){
        PacketDescriptor descriptor = createPacketDescriptor("Pingresp", "As with MQTT, a PINGRESP message is the response to a PINGREQ message and means ”yes I am alive”. Keep Alive messages flow in either direction, sent either by a connected client or the gateway.", 0x17);
        descriptor.setNotes("Moreover, a PINGRESP message is sent by a gateway to inform a sleeping client that it has no more buffered messages for that client.");
        addPacketDescriptor(descriptor);
    }

    protected void initDisconnect(){
        PacketDescriptor descriptor = createPacketDescriptor("Disconnect", "As with MQTT, the PINGREQ message is an ”are you alive” message that is sent from or received by a connected client.", 0x18);
        descriptor.addFieldDescriptor(new FieldDescriptor("Duration","Contains the value of the sleep timer; this field is optional and is included by a “sleeping” client that wants to go the “asleep” state,").setLength(2).setOptional(true));
        descriptor.setNotes("As with MQTT, the DISCONNECT message is sent by a client to indicate that it wants to close the connection. The gateway will acknowledge the receipt of that message by returning a DISCONNECT to the client. A server or gateway may also sends a DISCONNECT to a client, e.g. in case a gateway, due to an error, cannot map a received message to a client. Upon receiving such a DISCONNECT message, a client should try to setup the connection again by sending a CONNECT message to the gateway or server. In all these cases the DISCONNECT message does not contain the Duration field. A DISCONNECT message with a Duration field is sent by a client when it wants to go to the “asleep” state. The receipt of this message is also acknowledged by the gateway by means of a DISCONNECT message (without a duration field).");
        addPacketDescriptor(descriptor);
    }

    protected void initWilltopicUpd(){
        PacketDescriptor descriptor = createPacketDescriptor("WillTopicUpd", "The WILLTOPICUPD message is sent by a client to update its Will topic name stored in the GW/server.", 0x1A);
        descriptor.addFieldDescriptor(createFlagsField("Flags"));
        descriptor.addFieldDescriptor(new FieldDescriptor("WillTopic","Contains the Will topic name").setLength(Integer.MAX_VALUE));
        descriptor.setNotes("An empty WILLTOPICUPD message is a WILLTOPICUPD message without Flags and WillTopic field (i.e.it is exactly 2 octets long). It is used by a client to delete its Will topic and Will message stored in the GW/server.");
        addPacketDescriptor(descriptor);
    }

    protected void initWilltopicResp(){
        PacketDescriptor descriptor = createReturnCodePacketDescriptor("WillTopicResp", "The WILLTOPICRESP message is sent by a GW to acknowledge the receipt and processing of an WILL- TOPICUPD message.", 0x1B);
        addPacketDescriptor(descriptor);
    }

    protected void initWillmsgUpd(){
        PacketDescriptor descriptor = createPacketDescriptor("WillMsgUpd", "The WILLMSGUPD message is sent by a client to update its Will message stored in the GW/server.", 0x1C);
        descriptor.addFieldDescriptor(new FieldDescriptor("WillMsg","Contains the Will message").setLength(Integer.MAX_VALUE).setOptional(true));
        descriptor.setNotes("An empty WILLTOPICUPD message is a WILLTOPICUPD message without Flags and WillTopic field (i.e.it is exactly 2 octets long). It is used by a client to delete its Will topic and Will message stored in the GW/server.");
        addPacketDescriptor(descriptor);
    }

    protected void initWillmsgResp(){
        PacketDescriptor descriptor = createReturnCodePacketDescriptor("WillMsgResp", "The WILLTOPICRESP message is sent by a GW to acknowledge the receipt and processing of an WILL- TOPICUPD message.", 0x1D);
        addPacketDescriptor(descriptor);
    }

    protected void initEncapsulatedMessage(){
        PacketDescriptor descriptor = createPacketDescriptor("EncapsulatedMsg", "The forwarder simply encapsulates the MQTT-SN frames it receives on the wireless side and forwards them unchanged to the GW; in the opposite direction, it decapsulates the frames it receives from the gateway and sends them to the clients, unchanged too.", 0xFE);
        descriptor.addFieldDescriptor(new FieldDescriptor("Ctrl","The Ctrl octet contains control information exchanged between the GW and the forwarder.").
                addFlagsDescriptor(new FlagsDescriptor("Reserved","All remaining bits are reserved",7,2)).
                addFlagsDescriptor(new FlagsDescriptor("Radius","Broadcast radius (only relevant in direction GW to forwarder)",1,0)));
        descriptor.addFieldDescriptor(new FieldDescriptor("WirelessNodeId","Identifies the wireless node which has sent or should receive the encapsulated MQTT-SN message. The mapping between this Id and the address of the wireless node is implemented by the for- warder, if needed.").setLength(2));
        descriptor.addFieldDescriptor(new FieldDescriptor("MQTT-SN-Msg","the MQTT-SN message, encoded.").setLength(Integer.MAX_VALUE));
        addPacketDescriptor(descriptor);
    }

    static FieldDescriptor createFlagsField(String name){
        return new FieldDescriptor(name).
                addFlagsDescriptor(new FlagsDescriptor("DUP", "Set to '0' if message is sent for the first time; set to '1' if retransmitted", 7)).
                addFlagsDescriptor(new FlagsDescriptor("QoS", "For QoS level 0, 1, and 2; set to '0b00' for QoS level 0, '0b01' for QoS level 1, '0b10' for QoS level 2, and “0b11” for new QoS level -1", 6, 5)).
                addFlagsDescriptor(new FlagsDescriptor("Retain", "Same meaning as with MQTT", 4)).
                addFlagsDescriptor(new FlagsDescriptor("Will", "If set, indicates that client is asking for Will topic and Will message prompting", 3)).
                addFlagsDescriptor(new FlagsDescriptor("CleanSession", "Same meaning as with MQTT, however extended for Will topic and Will message", 2)).
                addFlagsDescriptor(new FlagsDescriptor("TopicIdType", "Indicates whether the field TopicId or TopicName included in this message contains a normal topic id (set to “0b00”), a pre-defined topic id (set to “0b01”), or a short topic name (set to “0b10”). The value “0b11” is reserved.", 1, 0));
    }

    static PacketDescriptor createPacketDescriptor(String name, String description, int packetType){
        PacketDescriptor descriptor = new PacketDescriptor(name, description, packetType);
        descriptor.addFieldDescriptor(new FieldDescriptor("Length","The Length field is either 1 or 3 octet(s) long and specifies the total number of octets contained in the message (including the Length field itself)"));
        descriptor.addFieldDescriptor(new FieldDescriptor("MsgType","The MsgType field is 1-octet long and specifies the message type"));
        return descriptor;
    }

    static PacketDescriptor createReturnCodePacketDescriptor(String name, String description, int packetType){
        PacketDescriptor descriptor = createPacketDescriptor(name, description, packetType);
        descriptor.addFieldDescriptor(new FieldDescriptor("ReturnCode","Encoded per the allowed ReturnCode values"));
        return descriptor;
    }
}
