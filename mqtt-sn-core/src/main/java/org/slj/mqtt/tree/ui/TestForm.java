package org.slj.mqtt.tree.ui;

import javax.swing.*;

public class TestForm extends JFrame {
    private JTextField textField1;
    private JTextField textField2;
    private JTree tree1;
    private JTable table1;
    private JPanel mainPanel;

    public TestForm() {

        setContentPane(mainPanel);
        setTitle("Welcome");
        setSize(450, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String[] args) {
        TestForm form =new TestForm();
    }
}
