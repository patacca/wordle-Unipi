package edu.riccardomori.wordle.client.backend;

// Descriptor of a new game
public class GameDescriptor {
    public int wordSize;
    public int tries;

    public GameDescriptor(int wordSize, int tries) {
        this.wordSize = wordSize;
        this.tries = tries;
    }
}
