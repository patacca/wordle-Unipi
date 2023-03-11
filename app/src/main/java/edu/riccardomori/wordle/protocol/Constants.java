package edu.riccardomori.wordle.protocol;

// Some constants value that must be shared between the client and the server
public abstract class Constants {
    public static final int SOCKET_MSG_MAX_SIZE = 1024; // Maximum size for each message
    public static final int UDP_MSG_MAX_SIZE = 512; // Maximum size for a UDP message

    private Constants() {}
}
