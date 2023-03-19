package edu.riccardomori.wordle.client.backend;

/**
 * Describes the stats of a user
 */
public class UserStats {
    public int totGames;
    public int wonGames;
    public int currStreak;
    public int bestStreak;
    public double score;
    public int[] guessDist;

    public UserStats(int totGames, int wonGames, int currStreak, int bestStreak, double score,
            int[] guessDist) {
        this.totGames = totGames;
        this.wonGames = wonGames;
        this.currStreak = currStreak;
        this.bestStreak = bestStreak;
        this.score = score;
        this.guessDist = guessDist;
    }
}
