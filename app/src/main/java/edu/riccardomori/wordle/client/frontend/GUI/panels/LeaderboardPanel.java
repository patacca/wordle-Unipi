package edu.riccardomori.wordle.client.frontend.GUI.panels;

import java.util.List;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import edu.riccardomori.wordle.utils.Pair;

public class LeaderboardPanel extends JPanel {
    public LeaderboardPanel(List<Pair<String, Double>> leaderboard) {
        super();

        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // Header
        JLabel title = new JLabel("Leaderboard", JLabel.CENTER);
        title.setFont(new Font("Dialog", Font.BOLD, 26));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Leaderboard Panel
        JPanel leaderboardPanel = new JPanel();
        leaderboardPanel.setLayout(new GridLayout(leaderboard.size(), 3, 5, 10));

        // Useful fonts
        Font usernameFont = new Font("Dialog", Font.BOLD, 18);
        Font scoreFont = new Font("Dialog", Font.PLAIN, 16);

        int k = 1;
        for (Pair<String, Double> p : leaderboard) {
            leaderboardPanel.add(new JLabel(String.format("%d.", k), JLabel.CENTER));
            JLabel score = new JLabel(String.format("%.2f", p.second), JLabel.CENTER);
            score.setFont(scoreFont);
            leaderboardPanel.add(score);
            JLabel username = new JLabel(p.first, JLabel.CENTER);
            username.setFont(usernameFont);
            leaderboardPanel.add(username);
            ++k;
        }

        this.add(title);
        this.add(leaderboardPanel);
    }
}
