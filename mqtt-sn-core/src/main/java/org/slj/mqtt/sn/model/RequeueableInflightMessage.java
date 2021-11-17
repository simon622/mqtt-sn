package org.slj.mqtt.sn.model;

import org.slj.mqtt.sn.spi.IMqttsnMessage;

public class RequeueableInflightMessage extends InflightMessage {

    QueuedPublishMessage queuedPublishMessage;

    public RequeueableInflightMessage(QueuedPublishMessage queuedPublishMessage, IMqttsnMessage message) {
        super(message, DIRECTION.SENDING, queuedPublishMessage.getToken());
        if(queuedPublishMessage.getToken() != null)
            queuedPublishMessage.getToken().setMessage(message);

        this.queuedPublishMessage = queuedPublishMessage;
    }

    public QueuedPublishMessage getQueuedPublishMessage() {
        return queuedPublishMessage;
    }

    public void setQueuedPublishMessage(QueuedPublishMessage queuedPublishMessage) {
        this.queuedPublishMessage = queuedPublishMessage;
    }

    @Override
    public String toString() {
        return "RequeueableInflightMessage{" +
                "token=" + token +
                ", message=" + message +
                ", time=" + time +
                ", queuedPublishMessage=" + queuedPublishMessage +
                '}';
    }
}
