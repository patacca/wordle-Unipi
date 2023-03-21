package edu.riccardomori.wordle.client.frontend.GUI.panels;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import edu.riccardomori.wordle.client.backend.ClientBackend;
import edu.riccardomori.wordle.client.backend.UserStats;
import edu.riccardomori.wordle.client.backend.exceptions.GenericError;
import edu.riccardomori.wordle.client.backend.exceptions.IOError;
import edu.riccardomori.wordle.client.backend.exceptions.NoGameException;
import edu.riccardomori.wordle.client.frontend.GUI.ClientSession;
import edu.riccardomori.wordle.client.frontend.GUI.ViewManager;
import edu.riccardomori.wordle.client.frontend.GUI.utils.Utils;
import edu.riccardomori.wordle.utils.Pair;

public class NavBarPanel extends JPanel {
    private static final String IMG_PATH = "static/img/";

    public NavBarPanel(ViewManager manager, ClientSession session, ClientBackend backend) {
        super();

        // this.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        // New game button
        JButton newGame = new JButton("New game");
        this.add(newGame);

        // Right navbar
        JPanel rightNav = new JPanel();
        rightNav.setLayout(new FlowLayout(FlowLayout.RIGHT));

        // Get the stats icon
        JButton statsIcon = this.createButton("stats.png");
        rightNav.add(statsIcon);

        // Get the leaderboard icon
        JButton leaderboardIcon = this.createButton("leaderboard.png");
        rightNav.add(leaderboardIcon);

        // Get the share icon
        JButton shareIcon = this.createButton("share.png");
        rightNav.add(shareIcon);

        this.add(rightNav);

        // Events
        leaderboardIcon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    List<Pair<String, Double>> leaderboard = backend.getFullLeaderboard();
                    JPanel panel = new LeaderboardPanel(leaderboard);
                    JOptionPane.showMessageDialog(null, panel, "Leaderboard",
                            JOptionPane.PLAIN_MESSAGE);
                } catch (GenericError | IOError e1) {
                    Utils.errorPopup("Cannot retrieve the leaderboard from the server");
                }
            }
        });
        statsIcon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Fetch stats
                UserStats stats;
                try {
                    stats = backend.getStats();
                    JPanel panel = new StatsPanel(stats);
                    JOptionPane.showMessageDialog(null, panel, "Statistics",
                            JOptionPane.PLAIN_MESSAGE);
                } catch (GenericError | IOError e1) {
                    Utils.errorPopup("Cannot retrieve stats from server");
                }
            }
        });
        shareIcon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    backend.shareLastGame();
                    Utils.successPopup("The result has been shared");
                } catch (NoGameException e1) {
                    Utils.errorPopup("You have to complete a game before sharing the result");
                } catch (GenericError | IOError e1) {
                    Utils.errorPopup("Cannot share the result with the server. Maybe it's down?");
                }
            }
        });
        newGame.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!session.isPlaying())
                    manager.switchTo(ViewManager.MAIN_VIEW);
                else
                    Utils.errorPopup("You are still playing a game!");
            }
        });
    }

    private JButton createButton(String imageName) {
        try (InputStream stream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(NavBarPanel.IMG_PATH + imageName)) {
            BufferedImage picture = ImageIO.read(stream);
            JButton button = new JButton(new ImageIcon(picture));
            return button;
        } catch (IOException e) {
            System.err.format("Cannot load the image %s", imageName);
            System.exit(1);
        }

        // Never happening
        return null;
    }
}
