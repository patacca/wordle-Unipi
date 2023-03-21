package edu.riccardomori.wordle.client.frontend.GUI.models;

import java.awt.Color;

public class WordleCell {
    private Color background;
    private Color foreground;
    private char ch;

    public WordleCell(Color foreground, Color background, char ch) {
        this.foreground = foreground;
        this.background = background;
        this.ch = ch;
    }

    public Color getBackgroundColor() {
        return this.background;
    }

    public Color getForegroundColor() {
        return this.foreground;
    }

    public char getChar() {
        return this.ch;
    }

    public void setForegroundColor(Color color) {
        this.foreground = color;
    }

    public void setBackgroundColor(Color color) {
        this.background = color;
    }
}
