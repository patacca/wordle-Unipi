package edu.riccardomori.wordle.client.backend;

public class UserStats {
    public int totGames;
    public int wonGames;
    public int currStreak;
    public int bestStreak;
    public double score;

    public UserStats(int totGames, int wonGames, int currStreak, int bestStreak, double score) {
        this.totGames = totGames;
        this.wonGames = wonGames;
        this.currStreak = currStreak;
        this.bestStreak = bestStreak;
        this.score = score;
    }
}
