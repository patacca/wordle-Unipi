package edu.riccardomori.wordle.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import edu.riccardomori.wordle.utils.Pair;

public interface clientRMI extends Remote {
    /**
     * @param leaderboard
     * @throws RemoteException
     */
    public void updateLeaderboard(List<Pair<String, Double>> leaderboard) throws RemoteException;
}
