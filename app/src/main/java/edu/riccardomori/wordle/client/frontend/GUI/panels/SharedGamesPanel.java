package edu.riccardomori.wordle.client.frontend.GUI.panels;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import edu.riccardomori.wordle.client.backend.GameShared;

public class SharedGamesPanel extends JPanel {
    public SharedGamesPanel(Map<String, Map<Long, GameShared>> games, String ignoreUsername) {
        super();

        if (games == null)
            return;

        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        // Useful style
        Font boldFont = new Font("Dialog", Font.BOLD, 18);

        int p = 0;

        for (String username : games.keySet()) { // For every user
            // Ignore our own shared games
            if (ignoreUsername.equals(username))
                continue;

            JLabel usernameLabel = new JLabel(username);
            usernameLabel.setFont(boldFont);
            this.addComponent(usernameLabel, gbc, 0, p, 0, 0, GridBagConstraints.LINE_START, 0, 0,
                    1, 1, new Insets(0, 0, 20, 0));

            ++p;
            Map<Long, GameShared> userGames = games.get(username);

            // Print each game
            for (long gameId : userGames.keySet()) { // For every game
                GameShared game = userGames.get(gameId);

                // Title
                JLabel title;
                if (game.tries < 0)
                    title = new JLabel(String.format("Wordle %d X/%d\n", gameId, game.maxTries));
                else
                    title = new JLabel(
                            String.format("Wordle %d %d/%d\n", gameId, game.tries, game.maxTries));
                title.setAlignmentX(Component.CENTER_ALIGNMENT);
                this.addComponent(title, gbc, 0, p, 0, 0, GridBagConstraints.PAGE_START, 0, 0, 1, 1,
                        new Insets(0, 0, 10, 0));
                ++p;

                JPanel gamePanel = new SingleSharedGamePanel(game, gameId);
                gamePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
                this.addComponent(gamePanel, gbc, 0, p, 0, 0, GridBagConstraints.PAGE_START, 0, 0,
                        1, 1, new Insets(0, 0, 20, 0));
                ++p;
            } // game

        } // user
    }

    private void addComponent(JComponent component, GridBagConstraints gbc, int gridx, int gridy,
            double weightx, double weighty, int anchor, int ipadx, int ipady, int width, int height,
            Insets insets) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        gbc.ipadx = ipadx;
        gbc.ipady = ipady;
        gbc.anchor = anchor;
        gbc.insets = insets;
        gbc.gridwidth = width;
        gbc.gridheight = height;

        this.add(component, gbc);
    }
}
