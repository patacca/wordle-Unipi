package edu.riccardomori.wordle.client.frontend.GUI.panels;

import java.awt.GridBagLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import edu.riccardomori.wordle.client.backend.UserStats;

public class StatsPanel extends JPanel {
    public StatsPanel(UserStats stats) {
        super();

        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        // Config
        int leftMargin = 40;
        int rightMargin = 30;
        int topMargin = 30;

        // Label stats
        JLabel statsLabel = new JLabel("STATISTICS", JLabel.CENTER);
        statsLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        this.addComponent(statsLabel, gbc, 0, 0, 0, 0, GridBagConstraints.LINE_START, 0, 0, 5, 1,
                new Insets(topMargin, leftMargin, 0, 0));

        // Reusable values
        Font stdFont = new Font("Dialog", Font.BOLD, 20);
        int rightGap = 20;
        int innerLeftMargin = leftMargin - 10;

        // Total games
        JLabel totGames = new JLabel(String.format("%d", stats.totGames), JLabel.CENTER);
        totGames.setFont(stdFont);
        this.addComponent(totGames, gbc, 0, 1, 1, 0, GridBagConstraints.CENTER, 0, 0, 1, 1,
                new Insets(20, innerLeftMargin, 0, rightGap));
        JLabel totGamesLabel = new JLabel("Played", JLabel.CENTER);
        this.addComponent(totGamesLabel, gbc, 0, 2, 1, 0, GridBagConstraints.CENTER, 0, 0, 1, 1,
                new Insets(5, innerLeftMargin, 0, rightGap));

        // Game won percentage
        int gameWonPctValue = 100 * stats.wonGames / (stats.totGames != 0 ? stats.totGames : 1);
        JLabel gameWonPct = new JLabel(String.format("%d", gameWonPctValue), JLabel.CENTER);
        gameWonPct.setFont(stdFont);
        this.addComponent(gameWonPct, gbc, 1, 1, 1, 0, GridBagConstraints.CENTER, 0, 0, 1, 1,
                new Insets(20, 0, 0, rightGap));
        JLabel gameWonPctLabel = new JLabel("Win %", JLabel.CENTER);
        this.addComponent(gameWonPctLabel, gbc, 1, 2, 1, 0, GridBagConstraints.CENTER, 0, 0, 1, 1,
                new Insets(5, 0, 0, rightGap));

        // Current streak
        JLabel currStreak = new JLabel(String.format("%d", stats.currStreak), JLabel.CENTER);
        currStreak.setFont(stdFont);
        this.addComponent(currStreak, gbc, 2, 1, 1, 0, GridBagConstraints.CENTER, 0, 0, 1, 1,
                new Insets(20, 0, 0, rightGap));
        JLabel currStreakLabel =
                new JLabel("<html><div style='text-align: center;'>Current<br>Streak</div></html>",
                        JLabel.CENTER);
        this.addComponent(currStreakLabel, gbc, 2, 2, 1, 0, GridBagConstraints.CENTER, 0, 0, 1, 1,
                new Insets(5, 0, 0, rightGap));

        // Best streak
        JLabel bestStreak = new JLabel(String.format("%d", stats.bestStreak), JLabel.CENTER);
        bestStreak.setFont(stdFont);
        this.addComponent(bestStreak, gbc, 3, 1, 1, 0, GridBagConstraints.CENTER, 0, 0, 1, 1,
                new Insets(20, 0, 0, rightGap));
        JLabel bestStreakLabel =
                new JLabel("<html><div style='text-align: center;'>Best<br>Streak</div></html>",
                        JLabel.CENTER);
        this.addComponent(bestStreakLabel, gbc, 3, 2, 1, 0, GridBagConstraints.CENTER, 0, 0, 1, 1,
                new Insets(5, 0, 0, rightGap));

        // Score
        JLabel score = new JLabel(String.format("%.2f", stats.score), JLabel.CENTER);
        score.setFont(stdFont);
        this.addComponent(score, gbc, 4, 1, 1, 0, GridBagConstraints.CENTER, 0, 0, 1, 1,
                new Insets(20, 0, 0, rightMargin));
        JLabel scoreLabel = new JLabel("Score", JLabel.CENTER);
        this.addComponent(scoreLabel, gbc, 4, 2, 1, 0, GridBagConstraints.CENTER, 0, 0, 1, 1,
                new Insets(5, 0, 0, rightMargin));

        // Guess distribution
        JLabel guessDistLabel = new JLabel("GUESS DISTRIBUTION", JLabel.CENTER);
        guessDistLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        this.addComponent(guessDistLabel, gbc, 0, 3, 0, 0, GridBagConstraints.LINE_START, 0, 0, 5,
                1, new Insets(30, leftMargin, 0, 0));
        JPanel guessDist = new GuessDistPanel(stats.guessDist);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.addComponent(guessDist, gbc, 0, 4, 0, 0, GridBagConstraints.LINE_START, 0, 0, 5, 1,
                new Insets(20, leftMargin, 0, 0));
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
