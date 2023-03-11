package edu.riccardomori.wordle.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import edu.riccardomori.wordle.utils.Pair;

// Leadebord implementation. All the operations are O(n)
// Every access if mutually exclusive.
// TODO consider using order statistic tree to make update O(log(n))
public class Leaderboard {
    // The BST ordered by score. The boolean value can safely be ignored
    // The key (score, username) are lexicographically ordered
    private TreeMap<Pair<Double, String>, Boolean> leaderboard;

    // Map { username -> key in the BST }
    private Map<String, Pair<Double, String>> userKeys;

    public Leaderboard(Collection<User> users) {
        this.leaderboard = new TreeMap<Pair<Double, String>, Boolean>();
        this.userKeys = new HashMap<String, Pair<Double, String>>();

        // Add all the users
        for (User user : users) {
            Pair<Double, String> p = new Pair<>(user.score(), user.getUsername());
            this.leaderboard.put(p, true);
            this.userKeys.put(user.getUsername(), p);
        }
    }

    /**
     * Returns the first {@code ranks} positions of the leaderboard
     * 
     * @param ranks
     * @return List of pairs <Username, Score> in the order they appear in the leaderboard
     */
    public synchronized List<Pair<String, Double>> get(int ranks) {
        List<Pair<String, Double>> ret = new ArrayList<>();
        for (Pair<Double, String> curr : this.leaderboard.navigableKeySet()) {
            ret.add(new Pair<String, Double>(curr.second, curr.first));
            if (ret.size() == ranks)
                return ret;
        }

        return ret;
    }

    /**
     * Returns the full leaderboard
     * 
     * @param ranks
     * @return List of pairs <Username, Score> in the order they appear in the leaderboard
     */
    public synchronized List<Pair<String, Double>> get() {
        List<Pair<String, Double>> ret = new ArrayList<>();
        for (Pair<Double, String> curr : this.leaderboard.navigableKeySet())
            ret.add(new Pair<String, Double>(curr.second, curr.first));

        return ret;
    }

    /**
     * Update the rank of {@code username} in the leaderboard and returns it's new position in the
     * leaderboard
     * 
     * @param username
     * @param score
     */
    public synchronized int update(String username, Double score) {
        Pair<Double, String> p = this.userKeys.get(username);
        if (p != null) {
            if (p.first == score)
                return -1; // Nothing changed, do not update the leaderboard

            // Remove the previous node and add the new one afterward
            this.leaderboard.remove(p);
        }

        // New node
        p = new Pair<Double, String>(score, username);
        this.leaderboard.put(p, true);
        this.userKeys.put(username, p);

        // Find the new position
        int k = 0;
        for (Pair<Double, String> curr : this.leaderboard.navigableKeySet()) {
            if (curr.second.equals(username))
                return k;
            k++;
        }

        // This never happens
        throw new RuntimeException();
    }
}
