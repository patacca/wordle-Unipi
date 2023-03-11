package edu.riccardomori.wordle.server;


// Describes a completed game, either winning or losing
public class GameDescriptor {
    public long gameId; // the ID of the game
    public int tries; // Number of tries needed. -1 if game was lost
    public int maxTries; // Number of tries allowed
    public int wordLen; // Length of the secret word

    // These two matrices contains all the hints provided for each try.
    // correct[k] -> the correct hints after the k-th try.
    public int[][] correct;
    public int[][] partial;

    public GameDescriptor(long gameId, int tries, int maxTries, int wordLen, int[][] correct,
            int[][] partial) {
        this.gameId = gameId;
        this.tries = tries;
        this.maxTries = maxTries;
        this.wordLen = wordLen;
        this.correct = correct;
        this.partial = partial;
    }

    public GameDescriptor(GameDescriptor obj) {
        this.gameId = obj.gameId;
        this.tries = obj.tries;
        this.maxTries = obj.maxTries;
        this.wordLen = obj.wordLen;
        this.correct = obj.correct.clone();
        this.partial = obj.partial.clone();
    }

    public GameDescriptor copy() {
        return new GameDescriptor(this);
    }
}
