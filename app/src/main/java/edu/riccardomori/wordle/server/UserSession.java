package edu.riccardomori.wordle.server;

public class UserSession {
    public boolean isActive = true; // Tells if the session is active or if it has been closed
    public String secretWord; // Last played secret word

    public UserSession() {}
}
