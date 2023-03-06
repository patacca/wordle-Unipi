package edu.riccardomori.wordle.client.backend;

public class GuessDescriptor {
    public int triesLeft;
    public int[] correct;
    public int[] partial;

    public GuessDescriptor(int triesLeft, int[] correct, int[] partial) {
        this.triesLeft = triesLeft;
        this.correct = correct;
        this.partial = partial;
    }
}