package edu.riccardomori.wordle.client.frontend.GUI.utils;

import java.awt.Container;
import javax.swing.JOptionPane;
import java.awt.Component;

public class Utils {
    /**
     * Recursively remove focus from a container and all its children
     * 
     * @param container The root container from which to start
     */
    public static void removeFocusFromAllObjects(Container container) {
        container.setFocusable(false);
        for (Component child : container.getComponents()) {
            if (child instanceof Container)
                removeFocusFromAllObjects((Container) child);
            else
                child.setFocusable(false);
        }
    }

    /**
     * Spawn an error popup with {@code JOptionPane}
     * 
     * @param message The message object
     * @see JOptionPane
     */
    public static void errorPopup(Object message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Spawn a warning popup with {@code JOptionPane}
     * 
     * @param message The message object
     * @see JOptionPane
     */
    public static void warningPopup(Object message) {
        JOptionPane.showMessageDialog(null, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Spawn a success popup with {@code JOptionPane}
     * 
     * @param message The message object
     * @see JOptionPane
     */
    public static void successPopup(Object message) {
        JOptionPane.showMessageDialog(null, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}
