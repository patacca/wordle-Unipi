package edu.riccardomori.wordle.server;

// Represents a single user account, with all the user stats.
// The class is not thread-safe so only one owner should operate on it
public class User {
    // Account details
    private String username;
    private String password;
    private transient UserSession session;

    // Stats
    private int[] guessDist = new int[WordleServer.WORD_TRIES + 1]; // index start from 1 just for
                                                                    // convenience
    public GameDescriptor lastGame;
    public int totGames;
    public int wonGames;
    public int currStreak;
    public int bestStreak;

    public User(String username, String password) {
        this.username = username;
        // TODO consider salting and hashing the password
        this.password = password;
    }

    /**
     * Updates the last game played
     * 
     * @param gameWon Whether the gam has been won or lost
     */
    private void updateLastGame(boolean gameWon) {
        // List to array conversion, we know... java is a burden
        int[][] correct = new int[this.session.correctHints.size()][];
        for (int i = 0; i < this.session.correctHints.size(); i++)
            correct[i] =
                    this.session.correctHints.get(i).stream().mapToInt(Integer::intValue).toArray();
        int[][] partial = new int[this.session.partialHints.size()][];
        for (int i = 0; i < this.session.partialHints.size(); i++)
            partial[i] =
                    this.session.partialHints.get(i).stream().mapToInt(Integer::intValue).toArray();

        if (gameWon) {
            this.lastGame = new GameDescriptor(this.session.gameId,
                    WordleServer.WORD_TRIES - this.session.triesLeft, WordleServer.WORD_TRIES,
                    this.session.secretWord.length(), correct, partial);
        } else {
            this.lastGame = new GameDescriptor(this.session.gameId, -1, WordleServer.WORD_TRIES,
                    this.session.secretWord.length(), correct, partial);
        }
    }

    public String getUsername() {
        return this.username;
    }

    public boolean passwordMatch(String password) {
        return this.password.equals(password);
    }

    public UserSession getSession() {
        return this.session;
    }

    public void setSession(UserSession session) {
        this.session = session;
    }

    public void newGame() {
        this.totGames++;
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

    /**
     * Register a winning a game in {@code tries} tries
     * 
     * @param tries number of tries used
     */
    public void winGame(int tries) {
        this.currStreak++;
        this.bestStreak = Math.max(this.currStreak, this.bestStreak);
        this.wonGames++;
        this.guessDist[tries]++;

        this.updateLastGame(true);
    }

    /**
     * Register a losing game
     */
    public void loseGame() {
        this.currStreak = 0;
        this.updateLastGame(false);
    }
}
