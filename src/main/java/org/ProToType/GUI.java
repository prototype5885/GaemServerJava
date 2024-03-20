package org.ProToType;

import javax.swing.*;

public class GUI {
    public SwingGUI swingGUI;

    public GUI() {
        swingGUI = new SwingGUI();

        JFrame window = new JFrame("alo");
        window.setSize(800, 600);
        window.setContentPane(swingGUI.rootPanel);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
    }

}
