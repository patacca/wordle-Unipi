package edu.riccardomori.wordle.client.frontend.GUI.panels;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import edu.riccardomori.wordle.client.backend.ClientBackend;
import edu.riccardomori.wordle.client.backend.GameDescriptor;
import edu.riccardomori.wordle.client.backend.GuessDescriptor;
import edu.riccardomori.wordle.client.backend.UserStats;
import edu.riccardomori.wordle.client.backend.exceptions.AlreadyPlayedException;
import edu.riccardomori.wordle.client.backend.exceptions.GenericError;
import edu.riccardomori.wordle.client.backend.exceptions.IOError;
import edu.riccardomori.wordle.client.backend.exceptions.InvalidWordException;
import edu.riccardomori.wordle.client.frontend.GUI.ClientSession;
import edu.riccardomori.wordle.client.frontend.GUI.models.WordleCell;
import edu.riccardomori.wordle.client.frontend.GUI.utils.Utils;

public class WordleGrid extends JPanel {
    private final int hspace = 10;
    private final int vspace = 10;
    private final int width = 60;
    private final int height = 60;

    private ClientBackend backend;
    private ClientSession session;

    private int rows;
    private int cols;
    private WordleCell[][] grid;
    private int currRow;
    private int currCol;

    public WordleGrid(int rows, int cols, ClientBackend backend, ClientSession session) {
        super();

        this.backend = backend;
        this.session = session;
        this.rows = rows;
        this.cols = cols;
        this.grid = new WordleCell[rows][cols];
        this.currRow = 0;
        this.currCol = 0;
        this.setPreferredSize(new Dimension(cols * (vspace + height), rows * (hspace + width)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int hshift = this.hspace + this.width;
        int vshift = this.vspace + this.height;
        Font titleFont = new Font("Dialog", Font.BOLD, 36);
        for (int r = 0; r < this.rows; ++r) {
            for (int c = 0; c < this.cols; ++c) {
                Rectangle rect = new Rectangle(c * hshift, r * vshift, this.width, this.height);
                drawBox(g2d, rect);
                drawChar(g2d, rect, this.grid[r][c], titleFont);
            }
        }
    }

    private void drawBox(Graphics2D g2d, Rectangle r) {
        int x = r.x + 1;
        int y = r.y + 1;
        int width = r.width - 2;
        int height = r.height - 2;
        g2d.setColor(new Color(211, 214, 218));
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawLine(x, y, x + width, y);
        g2d.drawLine(x, y + height, x + width, y + height);
        g2d.drawLine(x, y, x, y + height);
        g2d.drawLine(x + width, y, x + width, y + height);
    }

    private void drawChar(Graphics2D g2d, Rectangle r, WordleCell cell, Font titleFont) {
        if (cell == null)
            return;

        g2d.setColor(cell.getBackgroundColor());
        g2d.fillRect(r.x, r.y, r.width, r.height);
        g2d.setColor(cell.getForegroundColor());
        drawCenteredString(g2d, Character.toString(cell.getChar()), r, titleFont);
    }

    /**
     * Draw a String centered in the middle of a Rectangle.
     *
     * @param g The Graphics instance.
     * @param text The String to draw.
     * @param rect The Rectangle to center the text in.
     */
    private void drawCenteredString(Graphics2D g2d, String text, Rectangle rect, Font font) {
        FontMetrics metrics = g2d.getFontMetrics(font);
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();

        g2d.setFont(font);
        g2d.drawString(text, x, y);
    }

    private void showWaitingPopup(long nextGameTime) {
        String waitingTime = Duration.ofMillis(nextGameTime - System.currentTimeMillis()).toString()
                .substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .replaceAll("([0-9]+)\\.[0-9]*", "$1").toLowerCase();

        // Create the label
        String message = String.format(
                "<html>Sorry but you already played this game.<br />The next game will be available in %s</html>",
                waitingTime);
        final JLabel label = new JLabel(message, JLabel.CENTER);

        // Create the message dialog
        JOptionPane op = new JOptionPane(label, JOptionPane.WARNING_MESSAGE);
        final JDialog dialog = op.createDialog("Error");
        dialog.setAlwaysOnTop(true);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Create the timer
        Timer t = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Dispose dialog if the waiting time has ended
                long now = System.currentTimeMillis();
                if (nextGameTime - now <= 0) {
                    dialog.dispose();
                    return;
                }

                // Get the new time
                String waitingTime = Duration.ofMillis(nextGameTime - now).toString().substring(2)
                        .replaceAll("(\\d[HMS])(?!$)", "$1 ").replaceAll("([0-9]+)\\.[0-9]*", "$1")
                        .toLowerCase();

                // Change the label
                String message = String.format(
                        "<html>Sorry but you already played this game.<br />The next game will be available in %s</html>",
                        waitingTime);
                label.setText(message);
            }
        });
        t.start();

        // Show message dialog
        dialog.setVisible(true);

        // Stop the timer
        t.stop();
    }

    private void showWinPanel(String translation) {
        // Fetch stats
        UserStats stats;
        try {
            stats = this.backend.getStats();
            JPanel panel = new WinPanel(translation, stats);
            JOptionPane.showMessageDialog(null, panel, "Congratulations",
                    JOptionPane.PLAIN_MESSAGE);
        } catch (GenericError | IOError e) {
            Utils.errorPopup("Cannot retrieve stats from server");
        }
    }

    private void showLosePanel(String secretWord, String translation) {
        // Fetch stats
        UserStats stats;
        try {
            stats = this.backend.getStats();
            JPanel panel = new LosePanel(secretWord, translation, stats);
            JOptionPane.showMessageDialog(null, panel, "You lost", JOptionPane.PLAIN_MESSAGE);
        } catch (GenericError | IOError e) {
            Utils.errorPopup("Cannot retrieve stats from server");
        }
    }

    private boolean startGame() {
        try {
            GameDescriptor descriptor = this.backend.startGame();

            // Update the session state
            this.session.startGame(descriptor.wordSize, descriptor.tries);

        } catch (GenericError e) {
            Utils.errorPopup("An error happened. Try again later.");
            return false;

        } catch (IOError e) {
            Utils.errorPopup("I/O error during server communication.");
            return false;

        } catch (AlreadyPlayedException e) {
            this.showWaitingPopup(e.getResult());
            return false;
        }

        return true;
    }

    public void delChar() {
        // No char
        if (this.currCol == 0 || (this.currRow != 0 && !this.session.isPlaying()))
            return;

        --this.currCol;
        this.grid[this.currRow][this.currCol] = null;
        this.repaint();
    }

    public void addChar(char ch) {
        // Max column reached
        if (this.currCol == this.cols || (this.currRow != 0 && !this.session.isPlaying()))
            return;

        this.grid[this.currRow][this.currCol] = new WordleCell(Color.BLACK, Color.WHITE, ch);
        ++this.currCol;
        this.repaint();
    }

    public void submit() {
        // Check that the word is complete
        if (this.currCol != this.cols)
            return;

        // First attempt, start the game first
        if (this.currRow == 0 & !this.session.isPlaying())
            if (!this.startGame()) // If it didn't succeed stop immediately
                return;

        // Extract the guessed word
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < this.cols; ++k)
            sb.append(this.grid[this.currRow][k].getChar());
        String word = sb.toString().toLowerCase();

        boolean won = false;
        String secretWord = "";
        String translation = "";

        // Send the request
        try {
            GuessDescriptor result = this.backend.sendWord(word);

            secretWord = result.secretWord;
            translation = result.secretWordTranslation;
            won = result.gameWon;

            // Populate the hints
            if (won) {
                result.correct = new int[this.cols];
                result.partial = new int[0];
                for (int k = 0; k < this.cols; ++k)
                    result.correct[k] = k;
            }

            // Show the hints
            for (int k = 0; k < this.cols; ++k) {
                this.grid[this.currRow][k].setBackgroundColor(new Color(120, 124, 126));
                this.grid[this.currRow][k].setForegroundColor(Color.WHITE);
            }
            for (int p : result.correct)
                this.grid[this.currRow][p].setBackgroundColor(new Color(106, 170, 100));
            for (int p : result.partial)
                this.grid[this.currRow][p].setBackgroundColor(new Color(201, 180, 88));

            // Move to the next row
            ++this.currRow;
            this.currCol = 0;

            // Repaint
            this.repaint();

        } catch (InvalidWordException e) {
            Utils.warningPopup("Invalid word");
        } catch (GenericError | IOError e) {
            Utils.errorPopup("An error ahappened. Try again later.");
        }

        // Show final message
        if (won) {
            this.showWinPanel(translation);
            this.session.stopGame();
        } else if (this.currRow == this.rows) {
            this.showLosePanel(secretWord, translation);
            this.session.stopGame();
        }
    }
}
