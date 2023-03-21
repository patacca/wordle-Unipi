package edu.riccardomori.wordle.client.frontend.GUI.panels;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import edu.riccardomori.wordle.client.backend.GameShared;
import edu.riccardomori.wordle.client.frontend.GUI.models.WordleCell;

public class SingleSharedGamePanel extends JPanel {
    private final int hspace = 1;
    private final int vspace = 1;
    private final int width = 20;
    private final int height = 20;

    private int rows;
    private int cols;
    private WordleCell[][] grid;

    // private JLabel title;

    public SingleSharedGamePanel(GameShared game, long gameId) {
        super();

        this.rows = game.correct.length;
        this.cols = game.wordLen;

        this.grid = new WordleCell[this.rows][this.cols];

        this.setPreferredSize(new Dimension(this.cols * (this.vspace + this.height),
                this.rows * (this.hspace + this.width)));

        // The actual ZK game
        for (int r = 0; r < game.correct.length; ++r) {
            for (int c = 0; c < game.wordLen; ++c)
                this.grid[r][c] =
                        new WordleCell(new Color(150, 150, 150), new Color(150, 150, 150), '-');
            for (int k = 0; k < game.correct[r].length; ++k)
                this.grid[r][game.correct[r][k]].setBackgroundColor(new Color(106, 170, 100));
            for (int k = 0; k < game.partial[r].length; ++k)
                this.grid[r][game.partial[r][k]].setBackgroundColor(new Color(201, 180, 88));
        }
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
        for (int r = 0; r < this.rows; ++r) {
            for (int c = 0; c < this.cols; ++c) {
                Rectangle rect = new Rectangle(c * hshift, r * vshift, this.width, this.height);
                drawBox(g2d, rect);
                drawCell(g2d, rect, this.grid[r][c]);
            }
        }
    }

    private void drawBox(Graphics2D g2d, Rectangle r) {
        int x = r.x + 1;
        int y = r.y + 1;
        int width = r.width - 2;
        int height = r.height - 2;
        g2d.setColor(new Color(120, 120, 120));
        g2d.setStroke(new BasicStroke(0.1f));
        g2d.drawLine(x, y, x + width, y);
        g2d.drawLine(x, y + height, x + width, y + height);
        g2d.drawLine(x, y, x, y + height);
        g2d.drawLine(x + width, y, x + width, y + height);
    }

    private void drawCell(Graphics2D g2d, Rectangle r, WordleCell cell) {
        if (cell == null)
            return;

        g2d.setColor(cell.getBackgroundColor());
        g2d.fillRect(r.x, r.y, r.width, r.height);
    }
}
