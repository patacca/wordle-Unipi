package edu.riccardomori.wordle.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import edu.riccardomori.wordle.protocol.Action;
import edu.riccardomori.wordle.protocol.MessageStatus;
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
    private Socket socket; // The socket for communicating with the server
    private final int socketTimeout = 10000; // Timeout for reading on the socket

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

    private void exit(int ret) {
        this.out.println("Bye bye!");
        System.exit(ret);
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

    /**
     * Sends a message to the server. The message is encapsulated in the "communication protocol"
     * packet ([SIZE] [MESSAGE])
     * 
     * @param data The data to be sent
     * @throws IOException
     */
    private void socketWrite(ByteBuffer data) throws IOException {
        // Init the socket
        if (this.socket == null) {
            try {
                this.socket = new Socket(this.serverHost, this.serverPort);
            } catch (UnknownHostException e) {
                this.out.format("Hostname %s doesn't seem to exist.\n", this.serverHost);
                this.exit(1);
            }

            this.socket.setSoTimeout(this.socketTimeout);
        }

        // Write data as [SIZE] [MESSAGE]
        OutputStream out = new BufferedOutputStream(this.socket.getOutputStream());
        out.write(ByteBuffer.allocate(Integer.BYTES).putInt(data.limit()).array());
        out.write(data.array(), 0, data.limit());
        out.flush();
    }

    private MessageStatus socketGetStatus() throws IOException {
        DataInputStream input =
                new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        int size = input.readInt();
        byte[] message = input.readNBytes(size);
        return MessageStatus.fromByte(message[0]);
    }

    private void register() throws RemoteException {
        // Check username & password
        String username = this.readUntil(
                (input) -> input.matches("^[a-zA-Z0-9][a-zA-Z0-9_]{2,64}$"), "Username not valid.",
                "Choose a valid username > ", "Enter your username > ");
        String password = this.readUntil(
                (input) -> input
                        .matches("^[a-zA-Z0-9_`~!@#$%^&*()\\-=+{}\\[\\];:''\",<.>/?\\\\|]{4,64}$"),
                "Password not valid.", "Choose a valid password > ", "Enter your password > ");

        // Call RMI
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
        String username = this.readUntil((input) -> input.length() < 256,
                "The username is too large.", "Enter your username > ");
        String password = this.readUntil((input) -> input.length() < 256,
                "The password is too large.", "Enter your password > ");

        // Send the login command
        ByteBuffer data = ByteBuffer.allocate(1024);
        data.put(Action.LOGIN.getValue());
        data.put((byte) username.length());
        data.put((byte) password.length());
        data.put(username.getBytes(StandardCharsets.US_ASCII));
        data.put(password.getBytes(StandardCharsets.US_ASCII));
        data.flip();

        try {
            this.socketWrite(data);

            // Wait for the response
            MessageStatus status = this.socketGetStatus();

            // Success
            if (status == MessageStatus.SUCCESS) {
                this.out.println("Logged in successfully!");
                this.commandSet.remove(Command.LOGIN);
            } else if (status == MessageStatus.INVALID_USER) {
                this.out.println("Wrong username or password.");
            } else {
                this.out.println("An error happened. Try again later.");
            }
        } catch (IOException e) {
            // TODO
            // this.out.println("I/O during server communication.");
            e.printStackTrace();
            this.exit(1);
        }
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

            try {
                this.handleCommand(command);
            } catch (RemoteException e) {
                // e.printStackTrace(); // TODO delete this
                this.out.println("Remote exception occurred. Try again");
            }
        }
    }
}
