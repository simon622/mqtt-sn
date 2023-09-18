package org.slj.mqtt.tree;

import org.slj.mqtt.tree.radix.RadixTreeImpl;

import java.util.List;
import java.util.Set;

public class SearchableMqttTree<T>
        implements ISearchableMqttTree<T>, IMqttTree<T> {

    private final RadixTreeImpl<MqttTreeNode<T>> radix;
    private final IMqttTree<T> mqttTree;

    public SearchableMqttTree(IMqttTree<T> mqttTree) {
        this.mqttTree = mqttTree;
        radix = new RadixTreeImpl<>();
    }

    @Override
    public boolean hasMembers(String path) {
        return mqttTree.hasMembers(path);
    }

    @Override
    public boolean hasPath(String path) {
        return mqttTree.hasPath(path);
    }

    @Override
    public MqttTreeNode<T> getRootNode() {
        return mqttTree.getRootNode();
    }

    @Override
    public void visit(MqttTreeNodeVisitor visitor) {
        mqttTree.visit(visitor);
    }

    @Override
    public Set<String> getDistinctPaths(boolean considerMembership) {
        return mqttTree.getDistinctPaths(considerMembership);
    }

    @Override
    public int countDistinctPaths(boolean considerMembership) {
        return mqttTree.countDistinctPaths(considerMembership);
    }

    @Override
    public int getBranchCount() {
        return mqttTree.getBranchCount();
    }

    @Override
    public MqttTreeNode<T> subscribe(String path, T[] members)
            throws MqttTreeException, MqttTreeLimitExceededException {
        MqttTreeNode<T> node = null;
        try {
             node = mqttTree.subscribe(path, members);
             return node;
        } finally {
            synchronized (radix){
                //radix tree is not thread safe, so we need to populate it synchronized
                if(node != null && !radix.contains(path)){
                    radix.insert(path, node);
                }
            }
        }
    }

    @Override
    public boolean unsubscribe(String path, T member) throws MqttTreeException {

        boolean removed = mqttTree.unsubscribe(path, member);
        if(removed){
            if(!hasMembers(path)){
                synchronized (radix){
                    radix.delete(path);
                }
            }
        }
        return removed;
    }

    @Override
    public Set<T> search(String path) throws MqttTreeException {
        return mqttTree.search(path);
    }

    public List<MqttTreeNode<T>> prefixSearch(String path, int max) {
        return radix.searchPrefix(path, max);
    }
}
