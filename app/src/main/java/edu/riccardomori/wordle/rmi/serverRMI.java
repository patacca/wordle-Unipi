package edu.riccardomori.wordle.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This is the interface that defines which RMI methods are available
 */
public interface serverRMI extends Remote {
    /**
     * Register a new user with the pair (username, password).
     * 
     * @param username The username
     * @param password The password
     * @return {@code RMIStatus} describing the status of the registration
     * @throws RemoteException
     */
    public RMIStatus register(String username, String password) throws RemoteException;

    /**
     * @param client
     * @throws RemoteException
     */
    public void subscribe(clientRMI client) throws RemoteException;

    public void cancelSubscription(clientRMI client) throws RemoteException;
}
