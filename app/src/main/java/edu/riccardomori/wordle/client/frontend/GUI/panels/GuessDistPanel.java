package edu.riccardomori.wordle.client.frontend.GUI.panels;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public class GuessDistPanel extends JPanel {
    private double[] percentages;
    private int[] distribution;

    private int interspace;
    private int barLeftMargin = 30;
    private int baseBarWidth = 20;
    private int minBarWidth = 20 + this.baseBarWidth;
    private int minBarHeight;
    private int minWidth;
    private int minHeight;

    private int topMargin = 20;
    private int leftMargin = 20;
    private int rightMargin = 30;
    private Font textFont = new Font("Dialog", Font.PLAIN, 16);

    public GuessDistPanel(int[] distribution) {
        super();

        this.distribution = distribution;

        int maxValue = 0;
        for (int k = 0; k < distribution.length; ++k)
            maxValue = Math.max(maxValue, distribution[k]);

        this.percentages = new double[distribution.length];
        for (int k = 0; k < distribution.length; ++k)
            this.percentages[k] = (double) distribution[k] / maxValue;

        // Get the font metrics. It is used to calculate the metrics of the panel
        Canvas c = new Canvas();
        FontMetrics metrics = c.getFontMetrics(this.textFont);

        // Set default metrics for the panel
        this.interspace = metrics.getHeight() / 3;
        this.minBarHeight = metrics.getHeight();
        this.minHeight =
                this.topMargin + this.distribution.length * (this.minBarHeight + this.interspace);
        this.minWidth = this.leftMargin + this.barLeftMargin + this.minBarWidth + this.rightMargin;
        this.setPreferredSize(new Dimension(this.minWidth, this.minHeight));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setFont(textFont);
        FontMetrics metrics = g2d.getFontMetrics(this.textFont);

        int x = this.leftMargin;
        int y = this.topMargin;
        // TODO get the correct height from this.getHeight()
        int barHeight = Math.max(this.minBarHeight, 0);
        int barX = x + this.barLeftMargin;
        int fullBarWidth = Math.max(this.minBarWidth, this.getWidth() - this.rightMargin - barX);

        for (int k = 0; k < this.percentages.length; ++k) {
            int barY = y + this.interspace - barHeight;

            // The number
            String text = Integer.toString(k + 1);
            g2d.setColor(Color.BLACK);
            g2d.drawString(text, x, y + 2);

            // The bar
            g2d.setColor(new Color(120, 124, 126));
            int barWidth =
                    (int) (Math.round(this.percentages[k] * fullBarWidth) + this.baseBarWidth);
            g2d.fillRect(barX, barY, barWidth, barHeight);

            // The count
            g2d.setColor(Color.WHITE);
            text = String.format("%d", this.distribution[k]);
            int textWidth = metrics.stringWidth(text);
            g2d.drawString(Integer.toString(this.distribution[k]), barX + barWidth - textWidth - 6,
                    y + 2);

            y += barHeight + this.interspace;
        }
    }
}