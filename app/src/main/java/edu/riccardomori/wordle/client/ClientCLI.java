package edu.riccardomori.wordle.client;

import java.io.PrintStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import edu.riccardomori.wordle.rmi.RMIConstants;
import edu.riccardomori.wordle.rmi.RMIStatus;
import edu.riccardomori.wordle.rmi.serverRMI;

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
        this.commandSet.add(Command.LOGIN);
        this.commandSet.add(Command.EXIT);
    }

    private void printActions() {
        this.out.println("\nChoose one of these actions");
        int k = 1;
        for (Command c : this.commandSet) {
            String s = c.toString().toLowerCase();
            this.out.format("  %d. %s\n", k, s.substring(0, 1).toUpperCase() + s.substring(1));
            ++k;
        }
    }

    /**
     * Reads a valid command from the input stream.
     * 
     * @return A valid command
     */
    private Command readCommand() {
        this.out.print("> ");
        Command command = Command.INVALID;
        try {
            int c = Integer.parseInt(this.in.nextLine());
            for (Command curr : this.commandSet) {
                --c;
                if (c == 0)
                    command = curr;
            }
        } catch (NumberFormatException e) {
            command = Command.INVALID;
        }
        while (!this.isValidCommand(command)) {
            this.out.println("Invalid command!");
            this.out.println(
                    "Please enter only the number that represents the action you want to do");
            this.out.print("> ");
            command = Command.INVALID;
            try {
                int c = Integer.parseInt(this.in.nextLine());
                for (Command curr : this.commandSet) {
                    --c;
                    if (c == 0)
                        command = curr;
                }
            } catch (NumberFormatException e) {
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

        // Check username
        while (!username.matches("^[a-zA-Z0-9][a-zA-Z0-9_]{2,64}$")) {
            this.out.println("Username not valid.");
            this.out.print("Choose a valid username > ");
            username = this.in.nextLine();
        }

        this.out.print("Enter your password > ");
        String password = this.in.nextLine();

        // Check password
        while (!password.matches("[a-zA-Z0-9_`~!@#$%^&*()\\-=+{}\\[\\];:''\",<.>/?\\\\|]{4,64}")) {
            this.out.println("Password not valid.");
            this.out.print("Choose a valid password > ");
            password = this.in.nextLine();
        }

        Registry registry = LocateRegistry.getRegistry(this.serverHost, this.rmiPort);
        try {
            serverRMI service = (serverRMI) registry.lookup(RMIConstants.RMI_REGISTER);
            RMIStatus result = service.register(username, password);
            if (result == RMIStatus.SUCCESS) {
                this.out.println("Successfully registered!");
                this.commandSet.add(Command.LOGIN);
                this.commandSet.remove(Command.REGISTER);
            } else if (result == RMIStatus.USER_TAKEN) {
                this.out.println("Username already taken.");
            } else {
                this.out.println("An error happened while registering.");
            }
        } catch (NotBoundException e) {
            this.out.println("The server is not responding. It might be offline");
            return;
        }
    }

    private void login() {
        this.out.print("Enter your username > ");
        String username = this.in.nextLine();
        this.out.print("Enter your password > ");
        String password = this.in.nextLine();

        // init-retrieve the socket
        // send the login command
        // wait for response
        // notify the user about the result
    }

    private void handleCommand(Command command) throws RemoteException {
        switch (command) {
            case REGISTER:
                this.register();
                break;
            case LOGIN:
                this.login();
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
                // e.printStackTrace(); // TODO delete this
                this.out.println("Remote exception occurred. Try again");
            }
        }
    }
}
