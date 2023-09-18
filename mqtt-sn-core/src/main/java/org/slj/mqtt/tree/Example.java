package org.slj.mqtt.tree;

import java.util.Set;

public class Example {
    public static void main(String[] args) throws MqttTreeException, MqttTreeLimitExceededException {
        MqttTree<String> tree = new MqttTree<String>(MqttTree.DEFAULT_SPLIT, true);

        tree.withMaxPathSegments(1024);
        tree.withMaxMembersAtLevel(1024);

        tree.subscribe("/this/is/a/topic", "ClientId1", "ClientId2");

        tree.subscribe("/this/+/a/topic", "ClientId3");

        tree.subscribe("/this/#", "ClientId4");

        Set<String> m = tree.search("/this/is/a/topic");

        System.out.println(String.format("matching search had [%s] members", m.size()));

        m = tree.search("/is/a/different/topic");

        System.out.println(String.format("non-matching search had [%s] members", m.size()));

        tree.unsubscribe("/this/is/a/topic", "ClientId2");

        m = tree.search("/this/is/a/topic");

        System.out.println(String.format("matching search had [%s] members", m.size()));
    }
}
