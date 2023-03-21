package edu.riccardomori.wordle.client.frontend.GUI.panels;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import edu.riccardomori.wordle.client.backend.ClientBackend;
import edu.riccardomori.wordle.client.frontend.GUI.ClientSession;
import edu.riccardomori.wordle.client.frontend.GUI.ViewManager;
import edu.riccardomori.wordle.client.frontend.GUI.utils.Utils;

public class GamePanel extends JPanel {
    private ClientBackend backend;
    private ClientSession session;
    private WordleGrid wordleGrid;

    public GamePanel(ViewManager manager, ClientBackend backend, ClientSession session) {
        super();

        this.backend = backend;
        this.session = session;

        this.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        this.setLayout(new BorderLayout(0, 10));

        // Nav bar
        JPanel navbar = new NavBarPanel(manager, session, backend);
        Utils.removeFocusFromAllObjects(navbar);
        this.add(navbar, BorderLayout.PAGE_START);

        this.wordleGrid = new WordleGrid(12, 10, backend, session);

        this.add(this.wordleGrid, BorderLayout.CENTER);

        // Event listeners
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char ch = e.getKeyChar();
                if (ch == KeyEvent.VK_ENTER) {
                    GamePanel.this.wordleGrid.submit();
                } else if (ch == KeyEvent.VK_BACK_SPACE) {
                    GamePanel.this.wordleGrid.delChar();
                } else if (Character.isLetter(ch)) {
                    GamePanel.this.wordleGrid.addChar(Character.toUpperCase(ch));
                }
            }
        });

        this.setFocusable(true);
    }
}

// TODO
// menu > top leaderboard > show shared games
// registration