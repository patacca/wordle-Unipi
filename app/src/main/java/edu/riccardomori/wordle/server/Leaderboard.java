package edu.riccardomori.wordle.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import edu.riccardomori.wordle.utils.Pair;

// Leadebord implementation
// All the operations are O(log(n))
public class Leaderboard {
    // The BST ordered by score. The boolean value can safely be ignored
    // The key (score, username) won't affect the ordering
    private TreeMap<Pair<Double, String>, Boolean> leaderboard;

    // Map { username -> key in the BST }
    private Map<String, Pair<Double, String>> userKeys;
    private int threshold; // threshold value for triggering the notification to the subscribers

    public Leaderboard(Collection<User> users, int threshold) {
        this.threshold = threshold;
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
     * Update the rank of {@code username} in the leaderboard. If this changes the first
     * {@code this.threshold} positions then notify all the subscribers
     * 
     * @param username
     * @param score
     */
    public synchronized void update(String username, Double score) {
        // Memorize the rank of the user
        int prevRank = -1;
        int k = 0;
        for (Pair<Double, String> curr : this.leaderboard.navigableKeySet()) {
            if (k >= this.threshold)
                break;
            if (curr.second.equals(username)) {
                prevRank = k;
                break;
            }
            k++;
        }

        Pair<Double, String> p = this.userKeys.get(username);
        if (p != null) {
            if (p.first == score)
                return; // Nothing changed, do not update the leaderboard

            // Remove the previous node and add the new one afterward
            this.leaderboard.remove(p);
        }

        // New node
        p = new Pair<Double, String>(score, username);
        this.leaderboard.put(p, true);
        this.userKeys.put(username, p);

        // Check if the first positions changed
        k = 0;
        for (Pair<Double, String> curr : this.leaderboard.navigableKeySet()) {
            if (k >= this.threshold)
                return;
            // The leaderboard changed <=> the current user changed rank
            if (curr.second.equals(username) && k != prevRank) {
                WordleServer.getInstance().notifySubscribers();
                return;
            }
            k++;
        }
    }
}
