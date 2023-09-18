package org.slj.mqtt.tree;

import java.util.Set;

public interface MqttTreeNode<T> {

    boolean isRoot();

    boolean isLeaf();

    boolean isWildcard();

    boolean isWildpath();

    boolean hasChildren();

    boolean hasMembers();

    Set<String> getChildPaths();

    MqttTreeNode getChild(String path);

    String toPath(boolean climb);

    Set<T> getMembers();

    boolean hasChild(String path);

    int getMemberCount();

    String getPathSegment();

    MqttTreeNode getParent();
}
