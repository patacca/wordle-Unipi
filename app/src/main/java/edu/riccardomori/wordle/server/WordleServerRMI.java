package edu.riccardomori.wordle.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This is the interface that defines which RMI methods are available
 */
public interface WordleServerRMI extends Remote {
    /**
     * Register a new user with the pair (username, password).
     * 
     * @param username The username
     * @param password The password
     * @return True if the registration was successful, false otherwise 
     * @throws RemoteException
     */
    public boolean register(String username, String password) throws RemoteException;
}
