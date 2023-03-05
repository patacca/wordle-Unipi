package edu.riccardomori.wordle.client.frontend;

import java.io.PrintStream;
import java.util.Scanner;
import java.util.function.Predicate;
import edu.riccardomori.wordle.client.backend.ClientBackend;
import edu.riccardomori.wordle.client.backend.Command;
import edu.riccardomori.wordle.client.backend.GameDescriptor;
import edu.riccardomori.wordle.client.backend.SessionState;
import edu.riccardomori.wordle.client.backend.exceptions.*;

public class ClientCLI implements ClientFrontend {
    private final PrintStream out = System.out;
    private final Scanner in = new Scanner(System.in);

    private SessionState session = new SessionState(); // Describes the state of the current session
    private ClientBackend backend; // The backend implementation of the client

    private String serverHost; // The server host

    public ClientCLI(String host, int serverPort, int rmiPort) {
        this.backend = new ClientBackend(host, serverPort, rmiPort);
        this.serverHost = host;
    }

    private void exit(int ret) {
        this.out.println("Bye bye!");
        System.exit(ret);
    }

    private void printActions() {
        this.out.println("\nChoose one of these actions");
        int k = 1;
        for (Command c : this.session.getCommands()) {
            String s = c.toString().toLowerCase();
            this.out.format("  %d. %s\n", k, s.substring(0, 1).toUpperCase() + s.substring(1));
            ++k;
        }
    }

    /**
     * Read a line from this.in until it is validated by the supplied predicate. It prints out the
     * error message when the predicate evaluates to false. It prints out (without a final newline)
     * `prefix` when reading the line except for the first message that is `prefixFirst` instead of
     * `prefix`. When a valid line is found, then it is returned.
     * 
     * @param pred The predicate that needs to validate the input
     * @param errorMsg The error message to print
     * @param prefix The prefix to print before reading
     * @param prefixFirst The first prefix message to print before reading. It is used only on the
     *        first print
     * @return The input string validated
     */
    private String readUntil(Predicate<String> pred, String errorMsg, String prefix,
            String prefixFirst) {
        this.out.print(prefixFirst);
        String value = this.in.nextLine();
        while (!pred.test(value)) {
            this.out.println(errorMsg);
            this.out.print(prefix);
            value = this.in.nextLine();
        }
        return value;
    }

    /**
     * It is equivalent to `readUntil(pred, errorMsg, prefix, prefix)`
     * 
     * @param pred The predicate that needs to validate the input
     * @param errorMsg The error message to print
     * @param prefix The prefix to print before reading
     * @return The input string validated
     */
    private String readUntil(Predicate<String> pred, String errorMsg, String prefix) {
        return this.readUntil(pred, errorMsg, prefix, prefix);
    }

    /**
     * Reads a valid command from the input stream.
     * 
     * @return A valid command
     */
    private Command readCommand() {
        if (this.session.isLogged())
            this.out.format("[%s] > ", this.session.getUsername());
        else
            this.out.print("> ");

        // Read command from input
        Command command = Command.INVALID;
        try {
            int c = Integer.parseInt(this.in.nextLine());
            command = this.session.getCommandByIndex(c);
        } catch (NumberFormatException e) {
            command = Command.INVALID;
        }

        // Repeat until a valid command is read
        while (!this.isValidCommand(command)) {
            this.out.println("Invalid command!");
            this.out.println(
                    "Please enter only the number that represents the action you want to do");
            if (this.session.isLogged())
                this.out.format("[%s] > ", this.session.getUsername());
            else
                this.out.print("> ");
            command = Command.INVALID;
            try {
                int c = Integer.parseInt(this.in.nextLine());
                command = this.session.getCommandByIndex(c);
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
        return this.session.getCommands().contains(command);
    }

    private void register() {
        // Check username & password
        String username = this.readUntil(
                (input) -> input.matches("^[a-zA-Z0-9][a-zA-Z0-9_]{2,64}$"), "Username not valid.",
                "Choose a valid username > ", "Enter your username > ");
        String password = this.readUntil(
                (input) -> input
                        .matches("^[a-zA-Z0-9_`~!@#$%^&*()\\-=+{}\\[\\];:''\",<.>/?\\\\|]{4,64}$"),
                "Password not valid.", "Choose a valid password > ", "Enter your password > ");

        // Call the backend
        try {
            this.backend.register(username, password);

            this.out.println("Successfully registered!");

            // Update session
            this.session.registered();
        } catch (UserTakenException e) {
            this.out.println("Username already taken.");
        } catch (ServerError e) {
            this.out.println("The server is not responding. It might be offline");
        } catch (GenericError e) {
            this.out.println("An error happened while registering.");
        }
    }

    private void login() {
        String username = this.readUntil((input) -> input.length() < 256,
                "The username is too large.", "Enter your username > ");
        String password = this.readUntil((input) -> input.length() < 256,
                "The password is too large.", "Enter your password > ");

        // Call backend
        try {
            this.backend.login(username, password);

            this.out.println("Logged in successfully!");

            // Update the session state
            this.session.login(username);
        } catch (InvalidUserException e) {
            this.out.println("Wrong username or password.");
        } catch (UnknownHostException e) {
            this.out.format("Hostname %s doesn't seem to exist.\n", this.serverHost);
            this.exit(1);
        } catch (IOError e) {
            this.out.println("I/O error during server communication.");
        } catch (GenericError e) {
            this.out.println("An error happened. Try again later.");
        }
    }

    private void logout() {
        // Call backend
        try {
            this.backend.logout();

            this.out.println("Logged out");

            // Update the session state
            this.session.logout();
        } catch (GenericError e) {
            this.out.println("An error happened. Try again later.");
        } catch (IOError e) {
            this.out.println("I/O error during server communication.");
        }
    }

    private void play() {
        // Call backend
        try {
            GameDescriptor descriptor = this.backend.startGame();

            this.out.println("Starting the game");

            // Update the session state
            this.session.startGame();

            this.out.format("Word size %d.  Number of tries left %d\n", descriptor.wordSize,
                    descriptor.tries);
        } catch (GenericError e) {
            this.out.println("An error happened. Try again later.");
        } catch (IOError e) {
            this.out.println("I/O error during server communication.");
        }
    }

    private void handleCommand(Command command) {
        switch (command) {
            case REGISTER:
                this.register();
                break;
            case LOGIN:
                this.login();
                break;
            case PLAY:
                this.play();
                break;
            case LOGOUT:
                this.logout();
                break;
            case EXIT:
                this.exit(0);
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

            this.handleCommand(command);
        }
    }
}
