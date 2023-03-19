package edu.riccardomori.wordle.client.backend;

/**
 * Contains the information returned by the server as the result of a guess
 */
public class GuessDescriptor {
    public int triesLeft;
    public int[] correct;
    public int[] partial;
    public String secretWord;
    public String secretWordTranslation;
    public boolean gameWon;

    public GuessDescriptor(int triesLeft, int[] correct, int[] partial) {
        this.triesLeft = triesLeft;
        this.correct = correct;
        this.partial = partial;
        this.gameWon = false;
    }

    public GuessDescriptor(int triesLeft, int[] correct, int[] partial, String secretWord,
            String translation) {
        this.triesLeft = triesLeft;
        this.correct = correct;
        this.partial = partial;
        this.secretWord = secretWord;
        this.secretWordTranslation = translation;
        this.gameWon = false;
    }

    public GuessDescriptor(int triesLeft, String translation) {
        this.triesLeft = triesLeft;
        this.secretWordTranslation = translation;
        this.gameWon = true;
    }
}
