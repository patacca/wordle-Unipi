package edu.riccardomori.wordle.client.frontend.GUI;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import edu.riccardomori.wordle.client.backend.ClientBackend;
import edu.riccardomori.wordle.client.backend.GameShared;
import edu.riccardomori.wordle.client.backend.exceptions.GenericError;
import edu.riccardomori.wordle.client.backend.exceptions.IOError;
import edu.riccardomori.wordle.client.frontend.GUI.panels.GamePanel;
import edu.riccardomori.wordle.client.frontend.GUI.panels.LeaderboardPanel;
import edu.riccardomori.wordle.client.frontend.GUI.panels.LoginPanel;
import edu.riccardomori.wordle.client.frontend.GUI.panels.SharedGamesPanel;
import edu.riccardomori.wordle.client.frontend.GUI.utils.Utils;
import edu.riccardomori.wordle.utils.Pair;

public class ViewManager {
    // Constants
    private static final String FONT_PATH = "static/fonts/";
    public static final int MAIN_VIEW = 1;
    public static final int LOGIN_VIEW = 2;

    private JMenuItem logoutItem;
    private JFrame frame;

    private ClientSession session;
    private ClientBackend backend;

    public ViewManager(String title) {
        // Initialize the main frame
        this.frame = new JFrame();
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setTitle(title);

        // Register new font
        try (InputStream stream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(ViewManager.FONT_PATH + "NYTKarnakCondensed.ttf")) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, stream));
        } catch (IOException | FontFormatException e) {
            System.err.println("Cannot load the font NYTKarnakCondensed.ttf");
            System.exit(1);
        }

        this.frame.setJMenuBar(createMenuBar());
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenu showMenu = new JMenu("Show");
        menuBar.add(fileMenu);
        menuBar.add(showMenu);

        // Logout
        this.logoutItem = new JMenuItem("Logout");
        this.logoutItem.addActionListener(event -> {
            ViewManager.this.session.logout();
            ViewManager.this.switchTo(ViewManager.LOGIN_VIEW);
        });
        this.logoutItem.setEnabled(false); // Disabled by default
        fileMenu.add(this.logoutItem);

        // Exit
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(event -> {
            this.frame.dispose();
            System.exit(0);
        });
        fileMenu.add(exitItem);

        // Top Leaderboard
        JMenuItem topLeaderboardItem = new JMenuItem("Top leaderboard");
        topLeaderboardItem.addActionListener(event -> {
            List<Pair<String, Double>> leaderboard = this.session.getTopLeaderboard();
            if (leaderboard == null) {
                try {
                    leaderboard = this.backend.getLeaderboard();
                } catch (GenericError | IOError e) {
                    Utils.errorPopup("Cannot retrieve the leaderboard from the server");
                    return;
                }
            }
            JPanel panel = new LeaderboardPanel(leaderboard);
            JOptionPane.showMessageDialog(null, panel, "Top leaderboard",
                    JOptionPane.PLAIN_MESSAGE);
        });
        showMenu.add(topLeaderboardItem);

        // Shared games
        JMenuItem sharedGamesItem = new JMenuItem("Shared games");
        sharedGamesItem.addActionListener(event -> {
            Map<String, Map<Long, GameShared>> games = this.session.getNotifications();
            JPanel panel = new SharedGamesPanel(games, this.session.getUsername());
            JOptionPane.showMessageDialog(null, panel, "Shared games",
                    JOptionPane.PLAIN_MESSAGE);
        });
        showMenu.add(sharedGamesItem);

        return menuBar;
    }

    public void run(ClientSession session, ClientBackend backend) {
        this.session = session;
        this.backend = backend;

        this.frame.setContentPane(new LoginPanel(this, session, backend));

        this.frame.pack();
        this.frame.setMinimumSize(this.frame.getSize());

        // Set initial frame position to the center of the screen
        this.frame.setLocationRelativeTo(null);
        Point location = this.frame.getLocation();
        location.y -= location.y * 2 / 3;
        this.frame.setLocation(location);
        this.frame.setVisible(true);
    }

    public void switchTo(int view) {
        switch (view) {
            case ViewManager.MAIN_VIEW:
                this.logoutItem.setEnabled(true);
                this.frame.setContentPane(new GamePanel(this, this.backend, this.session));
                this.resync();
                break;

            case ViewManager.LOGIN_VIEW:
                this.logoutItem.setEnabled(false);
                this.frame.setContentPane(new LoginPanel(this, this.session, this.backend));
                this.resync();
                break;

            default:
                throw new IllegalArgumentException(String.format("No view with id %d", view));
        }
    }

    public void resync() {
        // Resize the frame only to enlarge it
        Dimension previousSize = this.frame.getSize();
        this.frame.pack();
        Dimension newSize = this.frame.getSize();
        newSize.height = Math.max(previousSize.height, newSize.height);
        newSize.width = Math.max(previousSize.width, newSize.width);
        this.frame.setSize(newSize);

        this.frame.revalidate();
        this.frame.repaint();
        this.frame.getContentPane().requestFocusInWindow();
    }
}
