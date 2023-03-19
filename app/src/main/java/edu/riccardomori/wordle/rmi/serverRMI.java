package edu.riccardomori.wordle.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This is the interface that defines which remote methods are available
 */
public interface serverRMI extends Remote {
    /**
     * Register a new user with the pair ({@code username}, {@code password}).
     * 
     * @param username The username
     * @param password The password
     * @return {@code RMIStatus} describing the status of the registration
     * @throws RemoteException
     */
    public RMIStatus register(String username, String password) throws RemoteException;

    /**
     * Subscribe to the notification callbacks for the updates on the leaderboard top positions
     * 
     * @param client The client remote object for the callback
     * @throws RemoteException
     */
    public void subscribe(clientRMI client) throws RemoteException;

    /**
     * Unsubscribe the {@code client} from the notifications callback service
     * 
     * @param client The client remote object that needs to be unsubscribed
     * @throws RemoteException
     */
    public void cancelSubscription(clientRMI client) throws RemoteException;
}
