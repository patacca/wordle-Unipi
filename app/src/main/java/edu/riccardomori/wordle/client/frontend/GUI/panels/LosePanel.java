package edu.riccardomori.wordle.client.frontend.GUI.panels;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import edu.riccardomori.wordle.client.backend.UserStats;

public class LosePanel extends JPanel {
    public LosePanel(String secretWord, String translation, UserStats stats) {
        super();

        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();


        // Label
        JLabel label =
                new JLabel("Sorry but you used all the available tries", JLabel.CENTER);
        this.addComponent(label, gbc, 0, 0, 0, 0, GridBagConstraints.PAGE_START, 0, 0, 1, 1);

        // Label
        JLabel label2 = new JLabel(String.format("The secret word was `%s`", secretWord),
                JLabel.CENTER);
        this.addComponent(label2, gbc, 0, 1, 0, 0, GridBagConstraints.PAGE_START, 0, 0, 1, 1);

        // Label
        JLabel label3 = new JLabel(String.format("The italian translation is `%s`", translation),
                JLabel.CENTER);
        this.addComponent(label3, gbc, 0, 2, 0, 1, GridBagConstraints.PAGE_START, 0, 0, 1, 1);

        // Stats Panel
        JPanel statsPanel = new StatsPanel(stats);
        gbc.fill = GridBagConstraints.BOTH;
        this.addComponent(statsPanel, gbc, 0, 3, 1, 1, GridBagConstraints.CENTER, 0, 0, 1, 1);
    }

    private void addComponent(JComponent component, GridBagConstraints gbc, int gridx, int gridy,
            double weightx, double weighty, int anchor, int ipadx, int ipady, int width,
            int height) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        gbc.ipadx = ipadx;
        gbc.ipady = ipady;
        gbc.anchor = anchor;
        gbc.gridwidth = width;
        gbc.gridheight = height;

        this.add(component, gbc);
    }
}
