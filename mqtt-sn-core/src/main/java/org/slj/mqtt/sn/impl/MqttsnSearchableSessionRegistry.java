package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.model.IClientIdentifierContext;
import org.slj.mqtt.sn.model.session.ISession;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;
import org.slj.mqtt.tree.radix.RadixTree;
import org.slj.mqtt.tree.radix.RadixTreeImpl;

import java.util.List;

/**
 * @author Simon L Johnson
 */
public class MqttsnSearchableSessionRegistry extends MqttsnSessionRegistry {
    protected RadixTree<String> searchTree;

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        searchTree = new RadixTreeImpl<>();
    }

    public List<String> prefixSearch(String prefix){
        return searchTree.searchPrefix(prefix, 100);
    }

    @Override
    public ISession createNewSession(IClientIdentifierContext context) {
        try {
            return super.createNewSession(context);
        } finally{
            try {
                if(searchTree != null) {
                    String clientId = context.getId();
                    searchTree.insert(clientId, clientId);
                }
            } catch(Exception e){
                throw new MqttsnRuntimeException(e);
            }
        }
    }

    @Override
    public void clear(ISession session, boolean clearNetworking) throws MqttsnException {
        super.clear(session, clearNetworking);
        try {
            if(searchTree != null) {
                searchTree.delete(session.getContext().getId());
            }
        } catch(Exception e){
            throw new MqttsnRuntimeException(e);
        }
    }
}
