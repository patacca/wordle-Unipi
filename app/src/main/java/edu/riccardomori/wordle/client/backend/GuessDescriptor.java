package edu.riccardomori.wordle.client.backend;

public class GuessDescriptor {
    public int triesLeft;
    public int[] correct;
    public int[] partial;
    public String secretWord;

    public GuessDescriptor(int triesLeft, int[] correct, int[] partial) {
        this.triesLeft = triesLeft;
        this.correct = correct;
        this.partial = partial;
    }

    public GuessDescriptor(int triesLeft, int[] correct, int[] partial, String secretWord) {
        this.triesLeft = triesLeft;
        this.correct = correct;
        this.partial = partial;
        this.secretWord = secretWord;
    }
}
