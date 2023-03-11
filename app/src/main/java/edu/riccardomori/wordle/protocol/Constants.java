package edu.riccardomori.wordle.protocol;

public abstract class Constants {
    public static final int SOCKET_MSG_MAX_SIZE = 1024; // Maximum size for each message
    public static final int UDP_MSG_MAX_SIZE = 512; // Maximum size for a UDP message

    private Constants() {}
}
