package edu.riccardomori.wordle.client.backend;

import java.util.SortedSet;
import java.util.TreeSet;
import edu.riccardomori.wordle.protocol.ClientState;

/**
 * Descriptor of the client session
 */
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

    public Command getCommandByIndex(int i, Command defaultValue) {
        for (Command curr : this.commandSet) {
            --i;
            if (i == 0)
                return curr;
        }
        return defaultValue;
    }

    public Command getCommandByIndex(int i) {
        return this.getCommandByIndex(i, Command.INVALID);
    }

    public void login(String username) {
        this.state.login();
        this.username = username;

        this.commandSet.remove(Command.LOGIN);
        this.commandSet.remove(Command.REGISTER);
        this.commandSet.add(Command.LOGOUT);
        this.commandSet.add(Command.PLAY);
    }

    public void registered() {
        this.commandSet.add(Command.LOGIN);
        this.commandSet.remove(Command.REGISTER);
    }

    public void logout() {
        this.state.logout();
        this.username = null;

        this.commandSet.add(Command.LOGIN);
        this.commandSet.add(Command.REGISTER);
        this.commandSet.remove(Command.LOGOUT);
        this.commandSet.remove(Command.PLAY);
    }

    public void startGame() {
        this.state.play();
    }

    public void stopGame() {
        this.state.stopPlaying();
    }

    public boolean isLogged() {
        return this.state.isLogged();
    }

    public boolean isPlaying() {
        return this.state.isPlaying();
    }
}
