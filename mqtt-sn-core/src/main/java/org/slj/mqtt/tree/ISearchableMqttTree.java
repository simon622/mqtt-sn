package org.slj.mqtt.tree;

import java.util.List;

/**
 * Provides index methods to search the Mqtt Tree character by character.
 */
public interface ISearchableMqttTree<T> extends IMqttTree<T>{

    /**
     * Use the radix index to perform quick lookup of your topic
     * @param path - any path prefix you would like to search
     * @param max - max results returned by the index
     * @return The nodes returned by the prefix search
     */
    List<MqttTreeNode<T>> prefixSearch(String path, int max);
}
