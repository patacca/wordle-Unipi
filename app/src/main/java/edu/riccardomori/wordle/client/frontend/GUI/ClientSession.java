package edu.riccardomori.wordle.client.frontend.GUI;

import java.util.List;
import java.util.Map;
import edu.riccardomori.wordle.client.backend.GameShared;
import edu.riccardomori.wordle.utils.Pair;

public interface ClientSession {
    public String getUsername();

    public void login(String username);

    public void logout();

    public void startGame(int wordLen, int triesN);

    public void stopGame();

    public boolean isPlaying();

    public List<Pair<String, Double>> getTopLeaderboard();

    public Map<String, Map<Long, GameShared>> getNotifications();
}
