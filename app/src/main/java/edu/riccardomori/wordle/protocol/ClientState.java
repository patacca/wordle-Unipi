package edu.riccardomori.wordle.protocol;

public class ClientState {
    // Bitmask constants
    public static final int LOGGED = 1;
    public static final int PLAYING = 2;

    private int value = 0;

    public ClientState() {}

    public void login() {
        this.value |= ClientState.LOGGED;
    }

    public void logout() {
        this.value &= ~(ClientState.LOGGED | ClientState.PLAYING);
    }

    public void play() {
        this.value |= ClientState.PLAYING;
    }

    public void stopPlaying() {
        this.value &= ~ClientState.PLAYING;
    }

    public boolean isLogged() {
        return (this.value & ClientState.LOGGED) != 0;
    }

    public boolean isAnonymous() {
        return (this.value & ClientState.LOGGED) == 0;
    }

    public boolean isPlaying() {
        return (this.value & ClientState.PLAYING) != 0;
    }
}
