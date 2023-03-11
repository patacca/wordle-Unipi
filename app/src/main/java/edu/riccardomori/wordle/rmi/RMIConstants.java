package edu.riccardomori.wordle.rmi;

// Some constants that need to be shared between the server and the client
public abstract class RMIConstants {
    // Name on the RMI registry
    public static final String SERVER_NAME = "Wordle";

    private RMIConstants() {}
}
