package edu.riccardomori.wordle.client.frontend;

import java.util.SortedSet;
import java.util.TreeSet;
import edu.riccardomori.wordle.protocol.ClientState;

// Descriptor of the client session.
// It also contains all the legal commands at any moment.
public class SessionState {
    private ClientState state = new ClientState(); // The state of the client in the communication
    private String username; // Only set if logged in
    private SortedSet<Command> commandSet; // The set of currently valid commands

    public SessionState() {
        // Initialize the valid commands
        this.commandSet = new TreeSet<>();
        this.commandSet.add(Command.REGISTER);
        this.commandSet.add(Command.LOGIN);
        this.commandSet.add(Command.EXIT);
    }

    public String getUsername() {
        return this.username;
    }

    public SortedSet<Command> getCommands() {
        return this.commandSet;
    }

    /**
     * Get the {@code Command} identified by the relative index {@code i} (starting from 1) in the
     * list of available commands. If it is not found returns the default value.
     * 
     * @param i The relative index of the {@code Command} to retrieve
     * @param defaultValue The default value to be returned if no command is identified by the
     *        specified index
     * @return The {@code Command} identified or the default value
     */
    public Command getCommandByIndex(int i, Command defaultValue) {
        try {
            return this.commandSet.stream().skip(i - 1).findFirst().orElse(defaultValue);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Same as {@code getCommandByIndex(i, Command.INVALID)}
     * 
     * @param i The relative index of the {@code Command} to retrieve
     * @return The {@code Command} identified or {@code INVALID}
     */
    public Command getCommandByIndex(int i) {
        return this.getCommandByIndex(i, Command.INVALID);
    }

    public boolean isLogged() {
        return this.state.isLogged();
    }

    public boolean isPlaying() {
        return this.state.isPlaying();
    }

    /**
     * Update the status after a successfull login
     * 
     * @param username The username
     */
    public void login(String username) {
        this.state.login();
        this.username = username;

        this.commandSet.remove(Command.LOGIN);
        this.commandSet.remove(Command.REGISTER);
        this.commandSet.add(Command.LOGOUT);
        this.commandSet.add(Command.PLAY);
        this.commandSet.add(Command.SHOW_STATS);
        this.commandSet.add(Command.SHOW_LEADERBOARD);
        this.commandSet.add(Command.SHOW_FULL_LEADERBOARD);
        this.commandSet.add(Command.SHOW_SHARED);
        this.commandSet.add(Command.SHARE);
    }

    /**
     * Update status after a successfull registration
     */
    public void register() {
        this.commandSet.add(Command.LOGIN);
        this.commandSet.remove(Command.REGISTER);
    }

    /**
     * Update status after a successfull logout
     */
    public void logout() {
        this.state.logout();
        this.username = null;

        this.commandSet.add(Command.LOGIN);
        this.commandSet.add(Command.REGISTER);
        this.commandSet.remove(Command.LOGOUT);
        this.commandSet.remove(Command.PLAY);
        this.commandSet.remove(Command.SHOW_STATS);
        this.commandSet.remove(Command.SHOW_LEADERBOARD);
        this.commandSet.remove(Command.SHOW_FULL_LEADERBOARD);
        this.commandSet.remove(Command.SHOW_SHARED);
        this.commandSet.remove(Command.SHARE);
    }

    /**
     * Update status after starting a game
     */
    public void startGame() {
        this.state.play();
        this.commandSet.remove(Command.SHARE);
    }

    /**
     * Update status after stopping a game
     */
    public void stopGame() {
        this.state.stopPlaying();
        this.commandSet.add(Command.SHARE);
    }
}
