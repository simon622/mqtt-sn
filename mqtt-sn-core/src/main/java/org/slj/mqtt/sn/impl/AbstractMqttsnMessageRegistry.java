package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.*;

import java.util.Date;
import java.util.UUID;

public abstract class AbstractMqttsnMessageRegistry  <T extends IMqttsnRuntimeRegistry>
        extends MqttsnService<T> implements IMqttsnMessageRegistry<T> {

    @Override
    public UUID add(byte[] data, boolean removeAfterRead) throws MqttsnException {
        UUID messageId = UUID.randomUUID();
        MessageImpl impl = new MessageImpl(messageId, data, removeAfterRead);
        return storeInternal(impl);
    }

    @Override
    public UUID add(byte[] data, Date expires) throws MqttsnException {
        UUID messageId = UUID.randomUUID();
        MessageImpl impl = new MessageImpl(messageId, data, expires);
        return storeInternal(impl);
    }

    @Override
    public byte[] get(UUID messageId) throws MqttsnException {

        MessageImpl impl = readInternal(messageId);
        if(impl != null){
            Date expires = impl.getExpires();
            if(expires != null && expires.before(new Date())){
                remove(messageId);
                impl = null;
            }
        }
        if(impl == null) throw new MqttsnExpectationFailedException("unable to read message by id, message not found in registry");
        if(impl.isRemoveAfterRead()){
            remove(messageId);
        }
        return impl.getData();
    }



    @Override
    public void clear(IMqttsnContext context) throws MqttsnException {
        throw new UnsupportedOperationException("message registry is global");
    }

    protected abstract boolean remove(UUID messageId) throws MqttsnException;

    protected abstract UUID storeInternal(MessageImpl message) throws MqttsnException;

    protected abstract MessageImpl readInternal(UUID messageId) throws MqttsnException;

    protected static class MessageImpl {

        Date created;
        Date expires;
        UUID messageId;
        byte[] data;
        boolean removeAfterRead = false;

        public MessageImpl(UUID messageId, byte[] data, boolean removeAfterRead) {
            this(messageId, data, null);
            this.removeAfterRead = removeAfterRead;
        }

        public MessageImpl(UUID messageId, byte[] data, Date expires) {
            this.created = new Date();
            this.expires = expires;
            this.messageId = messageId;
            this.data = data;
        }

        public boolean isRemoveAfterRead() {
            return removeAfterRead;
        }

        public void setRemoveAfterRead(boolean removeAfterRead) {
            this.removeAfterRead = removeAfterRead;
        }

        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        public Date getExpires() {
            return expires;
        }

        public void setExpires(Date expires) {
            this.expires = expires;
        }

        public UUID getMessageId() {
            return messageId;
        }

        public void setMessageId(UUID messageId) {
            this.messageId = messageId;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
    }
}