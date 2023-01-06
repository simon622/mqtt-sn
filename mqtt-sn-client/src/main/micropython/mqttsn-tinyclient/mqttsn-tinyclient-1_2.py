import socket

# Set up the MQTT-SN message
msg_type = 0x0C  # MQTT-SN PUBLISH message type
msg_id = 0  # The message ID (16-bit unsigned integer)
payload = b"hello world"  # The payload to send
length = 7 + len(payload)
qos = 3

# a pre-defined topic id (set to “0b01”)
topic_id_type = 0b01
topic_id = 0  # The topic ID to publish to (16-bit unsigned integer)

# short topic name (set to “0b10”)
topic_id_type = 0b10
short_topic_name = b"ab"

# Set up the MQTT-SN message header and payload
# length, msgType, flags, TopicId, MsgId, Data
flags = (False << 7) | (qos << 5) | (False << 4) | (False << 3) | (False << 2) | topic_id_type
header = bytearray([length, msg_type, flags])
if topic_id_type == 0b01:
    data = bytearray(topic_id.to_bytes(2, 'big'))
else:
    data = short_topic_name

data += bytearray(msg_id.to_bytes(2, 'big'))
data += payload

# Set up the UDP socket and send the message
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.sendto(header + data, ("localhost", 2442))
