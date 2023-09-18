package org.slj.mqtt.tree.ui;

import org.slj.mqtt.tree.*;

import javax.swing.*;
import java.io.IOException;

public class MqttTreeViewerLauncher {


    public static void main(String[] args) throws MqttTreeLimitExceededException, MqttTreeException {

        ISearchableMqttTree<String> tree = new SearchableMqttTree(
                new MqttTree<String>(MqttTree.DEFAULT_SPLIT, true));

        for (int i=0; i<1000; i++){
            tree.subscribe(MqttTreeUtils.generateRandomTopic(20), "SomeClientId");
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new MqttTreeViewer(tree);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
