package edu.riccardomori.wordle.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.riccardomori.wordle.rmi.RMIConstants;
import edu.riccardomori.wordle.rmi.RMIStatus;
import edu.riccardomori.wordle.rmi.serverRMI;
import edu.riccardomori.wordle.rmi.exceptions.PasswordIllegalException;
import edu.riccardomori.wordle.rmi.exceptions.UsernameIllegalException;

/**
 * This is the main server class. It handles all the incoming connections with non-blocking
 * channels. It also handles the generation of the secret word and the authentication of a pair
 * (user, password). It is a singleton class.
 */
public final class WordleServer implements serverRMI {
    private static WordleServer instance; // Singleton instance

    // Constants
    private static final String USERS_DB_FILE = "users.json"; // File where to store the users
                                                              // credentials
    public static final int SOCKET_MSG_MAX_SIZE = 1024; // Maximum size for each message

    // Attributes
    private boolean isConfigured = false; // Flag that forbids running the server if previously it
                                          // was not configured
    private int tcpPort; // The port of the server socket
    private int rmiPort; // The port of the RMI server
    private Logger logger;

    private Map<String, String> users;

    /**
     * Private static class that is used to describe the state of a client connection.
     */
    private static class ConnectionState {
        public WordleServerCore backend; // The backend object that handles the interaction with the
                                         // client
        public ByteBuffer readBuffer; // Buffer used for reading
        public ByteBuffer writeBuffer; // Buffer used for writing

        // The size of the application message that needs to be read
        // If it is set to -1 it means that the message size is still unknown
        public int readMessageSize = -1;

        public ConnectionState(WordleServerCore backend, int readCapacity, int writeCapacity) {
            this.backend = backend;
            this.readBuffer = ByteBuffer.allocate(readCapacity);
            this.writeBuffer = ByteBuffer.allocate(writeCapacity);
        }

        /**
         * Completes the reading phase. It returns a copy of the ByteBuffer that contains the data
         * read, then it resets both readBuffer and readMessageSize
         */
        public ByteBuffer finishRead() {
            ByteBuffer retBuff = ByteBuffer.wrap(this.readBuffer.array().clone());
            retBuff.limit(this.readBuffer.position());
            this.readBuffer.clear();
            this.readMessageSize = -1;

            return retBuff;
        }
    }

    private WordleServer() {
        this.logger = Logger.getLogger("Wordle");

        // Register a shutdown hook to keep syncronized the database
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.logger.finer("Exiting, syncing with the db");
            this.flush();
        }));
    }

    public static WordleServer getInstance() {
        if (WordleServer.instance == null)
            WordleServer.instance = new WordleServer();
        return WordleServer.instance;
    }

    private synchronized void flush() {
        Gson gson = new Gson();
        String data = gson.toJson(this.users);
        try (Writer out = new BufferedWriter(new FileWriter(WordleServer.USERS_DB_FILE))) {
            out.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Load the users from the database USERS_DB_FILE.
     */
    private void loadUsers() {
        try (Reader in = new BufferedReader(new FileReader(WordleServer.USERS_DB_FILE))) {
            Gson gson = new Gson();
            TypeToken<Map<String, String>> type = new TypeToken<Map<String, String>>() {};
            this.users = gson.fromJson(in, type);
        } catch (FileNotFoundException e) {
            this.logger.info("User database not found");
            this.users = new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Register all the RMI services
     */
    private void runRMIServer() {
        try {
            serverRMI stub = (serverRMI) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.createRegistry(this.rmiPort);
            registry.rebind(RMIConstants.RMI_REGISTER, stub);
        } catch (RemoteException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Configure the server.
     * 
     * @param tcpPort // The port for the server socket
     * @param rmiPort // The port for the RMI server
     */
    public void configure(int tcpPort, int rmiPort) {
        this.tcpPort = tcpPort;
        this.rmiPort = rmiPort;
        this.isConfigured = true;
    }

    @Override
    public RMIStatus register(String username, String password) throws RemoteException {
        this.logger.info("New registration");

        // Check username and password
        if (!username.matches("^[a-zA-Z0-9][a-zA-Z0-9_]{2,64}$")) {
            throw new UsernameIllegalException("username not valid");
        }
        if (!password.matches("[a-zA-Z0-9_`~!@#$%^&*()\\-=+{}\\[\\];:''\",<.>/?\\\\|]{4,64}")) {
            throw new PasswordIllegalException("password not valid");
        }

        // Check if exists username
        if (this.users.containsKey(username))
            return RMIStatus.USER_TAKEN;

        // Save the user and sync the database
        this.users.put(username, password);
        this.flush();

        return RMIStatus.SUCCESS;
    }

    /**
     * Check if the pair (username, password) correctly identifies a real user. If the pair is not
     * found in the user database then returns false
     * 
     * @param username The username
     * @param password The password
     * @return Whether the pair (username, password) is valid
     */
    public boolean checkLogin(String username, String password) {
        return (this.users.containsKey(username) && this.users.get(username).equals(password));
    }

    /**
     * The server main loop where it performs the multiplexing of the channels. The server must be
     * previously configured by calling WotdleServer.configure()
     */
    public void run() {
        // Check that all the parameters were configured
        if (!this.isConfigured)
            throw new RuntimeException("The server must be configured before running.");

        // Load the users database
        this.loadUsers();

        // Run RMI services
        this.runRMIServer();

        try (ServerSocketChannel socket = ServerSocketChannel.open();
                Selector selector = Selector.open()) {
            // Init server socket and listen on port `this.tcpPort`
            socket.bind(new InetSocketAddress(this.tcpPort));
            socket.configureBlocking(false);
            this.logger.info(String.format("Listening on port %d", this.tcpPort));

            // register the selector
            socket.register(selector, SelectionKey.OP_ACCEPT);

            // Main selector loop
            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (!key.isValid())
                        this.logger.warning("KEY NOT VALID!");

                    if (key.isAcceptable()) {
                        // Handle new connection
                        this.handleNewConnection(socket, selector);
                    } else if (key.isReadable()) {
                        this.handleRead(key, selector);
                    }

                    iter.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Handles a new incoming connection. It initializes the object WordleServerCore for that client
     * and register the channel to the selector
     * 
     * @param serverSocket The server socket channel
     * @param selector The selector where to register the new socket channel
     * @throws IOException
     */
    private void handleNewConnection(ServerSocketChannel serverSocket, Selector selector)
            throws IOException {
        // Accept the new connection
        SocketChannel socket = serverSocket.accept();
        this.logger.fine("New connection received");
        socket.configureBlocking(false);
        // Set TCP Keep Alive mode
        socket.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

        WordleServerCore serverBackend = new WordleServerCore();
        int interestOps = serverBackend.getInterestOps();

        socket.register(selector, interestOps, new ConnectionState(serverBackend,
                WordleServer.SOCKET_MSG_MAX_SIZE, WordleServer.SOCKET_MSG_MAX_SIZE));
    }

    // @formatter:off
    /**
     * Handles reading a message from the socket. All the messages must be in the format:
     *      [SIZE] [MESSAGE]
     * 
     * SIZE is always the size of a Integer (4 bytes) and it's encoded in big endian SIZE represents
     * the actual size of MESSAGE.
     * 
     * @param key The selection key
     * @param selector The selector
     * @throws IOException
     */
    // @formatter:on
    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        ConnectionState state = (ConnectionState) key.attachment();

        // Read data from the socket
        int nRead = socket.read(state.readBuffer);
        // Connection closed by client
        if (nRead < 0) {
            this.logger.finer("Connection closed");
            socket.close();
            return;
        }
        int size = state.readBuffer.position();

        // Read the message size
        if (state.readMessageSize == -1) {
            if (size < Integer.BYTES) // Not enough bytes have been read
                return;
            state.readBuffer.flip();
            state.readMessageSize = state.readBuffer.getInt();
            state.readBuffer.compact();

            // Update the size of the buffer
            size = state.readBuffer.position();
        }

        // Here we know the app message size
        this.logger.fine(String.format("Needs to receive a message of size %d bytes",
                state.readMessageSize));

        if (size < state.readMessageSize) // Not enough bytes
            return;

        state.backend.handleMessage(state.finishRead());
    }
}
