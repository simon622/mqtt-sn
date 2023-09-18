package org.slj.mqtt.tree.ui;

import org.slj.mqtt.tree.ISearchableMqttTree;
import org.slj.mqtt.tree.MqttTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class MqttTreeViewer  extends JFrame {
    private JTree tree;
    private JLabel selectedLabel;
    private JTextField searchBar;

    private final ISearchableMqttTree mqttTree;
    private DefaultMutableTreeNode jtreeRoot;


    public MqttTreeViewer(ISearchableMqttTree mqttTree) throws IOException {
        this.mqttTree = mqttTree;
        createSearchBar();
        initTree();

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("MQTT Tree Browser");
        this.setSize(400, 400);
        this.setVisible(true);
    }

    protected DefaultMutableTreeNode readNodes(ISearchableMqttTree mqttTree){

        MqttTreeNode node = mqttTree.getRootNode();
        return readTree(null, node);
    }

    public void initTree() throws IOException {
        try {
            if(jtreeRoot != null){
                jtreeRoot.removeAllChildren();
            }
            readNodes(mqttTree);
            if(tree == null)
                createTree(jtreeRoot);
        } finally {
            tree.repaint();
            tree.updateUI();
        }
    }

    protected void resetTreeWithFilter(DefaultMutableTreeNode parent, MqttTreeNode... nodes){
        if(parent == null) {
            jtreeRoot.removeAllChildren();
        }

        for (MqttTreeNode node : nodes){
            parent = jtreeRoot;
            List<MqttTreeNode> path = readToRoot(node);
            for(MqttTreeNode pathNode : path){
                DefaultMutableTreeNode exists = findChildByUserObject(parent, pathNode);
                if(exists == null){
                    DefaultMutableTreeNode current = new DefaultMutableTreeNode(pathNode.getPathSegment());
                    parent.add(current);
                    current.setUserObject(pathNode);
                    parent = current;
                } else {
                    parent = exists;
                }
            }
        }

        tree.repaint();
        tree.updateUI();

        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
            if(i > 10)
                break;
        }
    }

    protected DefaultMutableTreeNode findChildByUserObject(DefaultMutableTreeNode parent, MqttTreeNode node){
        Enumeration e = parent.children();
        while (e.hasMoreElements()){
            DefaultMutableTreeNode tNode = (DefaultMutableTreeNode) e.nextElement();
            if(tNode.getUserObject() == node){
                return tNode;
            }
        }
        return null;
    }

    protected DefaultMutableTreeNode readTree(DefaultMutableTreeNode parent, MqttTreeNode node){

        DefaultMutableTreeNode current = null;
        if(parent == null){
            if(jtreeRoot == null){
                jtreeRoot = new DefaultMutableTreeNode("MQTT Subscriptions");
            }
            parent = jtreeRoot;
            current = parent;
        } else {
            current = new DefaultMutableTreeNode(node.getPathSegment());
            parent.add(current);
        }

        current.setUserObject(node);
        Set<String> children = node.getChildPaths();
        for (String child : children){
            MqttTreeNode childNode = node.getChild(child);
            readTree(current, childNode);
        }
        return parent;
    }

    protected List<MqttTreeNode> readToRoot(MqttTreeNode leaf){
        List<MqttTreeNode> path = new ArrayList<>();

        while(!leaf.isRoot()){
            path.add(leaf);
            leaf = leaf.getParent();
        }
        Collections.reverse(path);
        return path;
    }

    protected void resetSearch(){
        searchBar.setText("Filter Subscriptions..");
        initialised = false;
    }
    volatile boolean initialised = false;

    protected void createSearchBar() throws IOException {

        searchBar = new JFormattedTextField();
        resetSearch();

        searchBar.addMouseListener(new MouseListener() {
                   @Override
                   public void mouseClicked(MouseEvent e) {
                       if(!initialised){
                           searchBar.setText("");
                           initialised = true;
                       }

                       if(searchBar.getText().equals("")){
                           try {
                               initTree();
                           } catch (IOException ex) {
                               throw new RuntimeException(ex);
                           }
                       }

                   }

                   @Override
                   public void mousePressed(MouseEvent e) {
                   }

                   @Override
                   public void mouseReleased(MouseEvent e) {
                   }

                   @Override
                   public void mouseEntered(MouseEvent e) {
                   }

                   @Override
                   public void mouseExited(MouseEvent e) {
                   }
               });

        searchBar.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if(searchBar.getText().trim().equals("")){
                    try {
                        initTree();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    String prefix = searchBar.getText();
                    List<MqttTreeNode> nodes = mqttTree.prefixSearch(prefix, 100);
                    System.out.println("'"+prefix+"' =  nodes " + Objects.toString(nodes));
                    resetTreeWithFilter(null,
                            nodes.toArray(new MqttTreeNode[0]));
                }

            }
        });
        add(searchBar, BorderLayout.PAGE_START);
    }


    protected void createTree(DefaultMutableTreeNode root) throws IOException {
        //create the tree by passing in the root node
        tree = new JTree(root){
            @Override
            public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if(value != null){
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    MqttTreeNode treeNode = (MqttTreeNode) node.getUserObject();
                    if(treeNode.isRoot()){
                        return "MQTT Subscriptions";
                    } else {
                        if(treeNode.hasMembers()){
                            return treeNode.getPathSegment() + " (" + treeNode.getMemberCount() + ")";
                        } else {
                            return treeNode.getPathSegment();
                        }
                    }
                } else {
                    return super.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
                }
            }
        };
//        ImageIcon folderIcon = MqttViewerUtils.loadIcon("folder.gif");
//        ImageIcon leafIcon = MqttViewerUtils.loadIcon("cellphone.gif");
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
//        renderer.setLeafIcon(leafIcon);
//        renderer.setOpenIcon(folderIcon);
//        renderer.setClosedIcon(folderIcon);
        tree.setCellRenderer(renderer);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(true);
        add(new JScrollPane(tree));

        selectedLabel = new JLabel();
        add(selectedLabel, BorderLayout.SOUTH);
        tree.getSelectionModel().addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if(selectedNode != null){
                MqttTreeNode node = (MqttTreeNode) selectedNode.getUserObject();
                selectedLabel.setText(node.toPath(true));
            }
        });
    }
}