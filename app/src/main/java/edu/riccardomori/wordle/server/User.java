package edu.riccardomori.wordle.server;

// Represents a single user account, with all the user stats
public class User {
    // Account details
    private String username;
    private String password;

    // Stats
    private int totGames;
    private int wonGames;
    private int[] guessDist = new int[WordleServer.WORD_TRIES + 1]; // index start from 1 just for
                                                                    // convenience
    private int currStreak;
    private int bestStreak;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public boolean passwordMatch(String password) {
        return this.password.equals(password);
    }

    public void newGame() {
        this.totGames++;
    }

    /**
     * Win a game in {@code tries} tries
     * 
     * @param tries number of tries used
     */
    public void winGame(int tries) {
        this.currStreak++;
        this.bestStreak = Math.max(this.currStreak, this.bestStreak);
        this.wonGames++;
        this.guessDist[tries]++;
    }

    public void loseGame() {
        this.currStreak = 0;
    }

    /**
     * Get the WAS (Wordle Average Score)
     * 
     * @return the WAS
     */
    public double score() {
        int sum = 0;
        for (int i = 1; i < this.guessDist.length; i++)
            sum += i * guessDist[i];

        sum += (WordleServer.WORD_TRIES + 1) * (this.totGames - this.wonGames);
        return (double) sum / this.totGames;
    }
}
