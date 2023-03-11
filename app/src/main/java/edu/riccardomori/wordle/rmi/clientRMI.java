package edu.riccardomori.wordle.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import edu.riccardomori.wordle.utils.Pair;

// Interface for a client that supports RMI
public interface clientRMI extends Remote {
    /**
     * Update the leaderboard top positions with the new one {@code leaderboard}
     * 
     * @param leaderboard The new leaderboard for the top positions
     * @throws RemoteException
     */
    public void updateLeaderboard(List<Pair<String, Double>> leaderboard) throws RemoteException;
}
