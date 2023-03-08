package edu.riccardomori.wordle.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import edu.riccardomori.wordle.utils.Pair;

public class Leaderboard {
    // array sorted
    // add insertion sort
    // update smart insertion sort
    private List<Pair<String, Double>> leaderboard;

    public Leaderboard(Collection<User> users) {
        this.leaderboard = new ArrayList<>();
        // Add all the users
        for (User user : users)
            this.leaderboard.add(new Pair<String,Double>(user.getUsername(), user.score()));
        
        // Sort the users based on the score
        Collections.sort(this.leaderboard, (u1, u2) -> u1.second.compareTo(u2.second));
    }
}
