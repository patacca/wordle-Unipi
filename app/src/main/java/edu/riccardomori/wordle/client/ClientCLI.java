package edu.riccardomori.wordle.client;

import java.io.PrintStream;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import edu.riccardomori.wordle.server.WordleServerRMI;
import edu.riccardomori.wordle.utils.RMIConstants;

public class ClientCLI {
    private final PrintStream out = System.out;
    private final Scanner in = new Scanner(System.in);
    private SortedSet<Command> commandSet;

    private String serverHost; // The server host
    private int serverPort; // The port of the server socket
    private int rmiPort; // The port of the RMI server

    public ClientCLI(String host, int serverPort, int rmiPort) {
        this.serverHost = host;
        this.serverPort = serverPort;
        this.rmiPort = rmiPort;

        // Initialize the valid commands
        this.commandSet = new TreeSet<>();
        this.commandSet.add(Command.REGISTER);
        this.commandSet.add(Command.EXIT);
    }

    private void printActions() {
        this.out.println("\nChoose one of these actions");
        this.out.println("  1. Register");
        this.out.println("  2. Exit\n");
    }

    /**
     * Reads a valid command from the input stream.
     * 
     * @return A valid command
     */
    private Command readCommand() {
        this.out.print("> ");
        Command command;
        try {
            command = Command.fromInt(Integer.parseInt(this.in.nextLine()));
        } catch (IllegalArgumentException e) {
            command = Command.INVALID;
        }
        while (!this.isValidCommand(command)) {
            this.out.println("Invalid command!");
            this.out.println(
                    "Please enter only the number that represents the action you want to do");
            this.out.print("> ");
            try {
                command = Command.fromInt(Integer.parseInt(this.in.nextLine()));
            } catch (IllegalArgumentException e) {
                command = Command.INVALID;
            }
        }

        return command;
    }

    /**
     * Check whether the command is one of the expected command.
     * 
     * @param command The command to be checked
     * @return true if the command is valid, false otherwise
     */
    private boolean isValidCommand(Command command) {
        return this.commandSet.contains(command);
    }

    private void register() throws RemoteException {
        this.out.print("Enter your username > ");
        String username = this.in.nextLine();
        this.out.print("Enter your password > ");
        String password = this.in.nextLine();

        Registry registry = LocateRegistry.getRegistry(this.serverHost, this.rmiPort);
        try {
            WordleServerRMI service = (WordleServerRMI) registry.lookup(RMIConstants.RMI_REGISTER);
            boolean result = service.register(username, password);
            if (result) {
                this.out.println("Successfully registered!");
            } else {
                this.out.println("Cannot register");
            }
        } catch (NotBoundException e) {
            this.out.println("The server is not responding. It might be offline");
            return;
        }
    }

    private void handleCommand(Command command) throws RemoteException {
        switch (command) {
            case REGISTER:
                this.register();
                break;
            case EXIT:
                this.out.println("Bye bye!");
                System.exit(0);
            default:
                throw new IllegalArgumentException("Cannot handle illegal command!");
        }
    }

    public void run() {
        this.out.println("Welcome to WORDLE");
        this.out.println("a game for smart people\n");

        // Main client loop
        while (true) {
            this.printActions();
            Command command = this.readCommand();

            try {
                this.handleCommand(command);
            } catch (RemoteException e) {
                this.out.println("Remote exception occurred. Try again");
            }
        }
    }
}
