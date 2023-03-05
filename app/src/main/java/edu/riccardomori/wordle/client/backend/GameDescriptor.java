package edu.riccardomori.wordle.client.backend;

public class GameDescriptor {
    public int wordSize;
    public int tries;

    public GameDescriptor(int wordSize, int tries) {
        this.wordSize = wordSize;
        this.tries = tries;
    }
}
