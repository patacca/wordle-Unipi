package edu.riccardomori.wordle.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Descriptor of a user session.
// This class is not thread safe.
public class UserSession {
    public boolean isActive = true; // Tells if the session is active or if it has been closed
    public String secretWord; // Last played secret word
    public long gameId; // Last played gameId
    public int triesLeft;
    public List<List<Integer>> correctHints = new ArrayList<>();
    public List<List<Integer>> partialHints = new ArrayList<>();

    public UserSession() {}

    /**
     * Generate the correct and partial hints and append them in the appropriate class fields
     * 
     * @param word The guessed word
     */
    public void addHint(String word) {
        List<Integer> correct = new ArrayList<>();
        List<Integer> partial = new ArrayList<>();
        Map<Character, Integer> map = new HashMap<>();

        // Check the correct ones and store the remeaining chars in map
        for (int k = 0; k < this.secretWord.length(); ++k) {
            if (this.secretWord.charAt(k) == word.charAt(k)) {
                correct.add(k);
            } else {
                int v = map.computeIfAbsent(this.secretWord.charAt(k), i -> 0);
                map.put(this.secretWord.charAt(k), v + 1);
            }
        }
        // Compute the partials
        for (int k = 0; k < this.secretWord.length(); ++k) {
            if (this.secretWord.charAt(k) != word.charAt(k)) {
                int c = map.getOrDefault(word.charAt(k), 0);
                if (c > 0) {
                    partial.add(k);
                    map.put(word.charAt(k), c - 1);
                }
            }
        }

        // Store the hints
        this.correctHints.add(correct);
        this.partialHints.add(partial);
    }

    /**
     * Returns the last correct hint
     * 
     * @return The last correct hint. The hint is unmodifiable
     */
    public List<Integer> getLastCorrectHint() {
        return Collections.unmodifiableList(this.correctHints.get(this.correctHints.size() - 1));
    }

    /**
     * Returns the last partial hint
     * 
     * @return The last partial hint. The hint is unmodifiable
     */
    public List<Integer> getLastPartialHint() {
        return Collections.unmodifiableList(this.partialHints.get(this.partialHints.size() - 1));
    }
}
