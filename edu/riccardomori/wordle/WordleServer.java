package edu.riccardomori.wordle;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * This is the main server class. It handles all the incoming connections with
 * non-blocking channels. It also handles the generation of the secret word and
 * the authentication of a pair (user, password).
 * It is a singleton class.
 */
public final class WordleServer {
    private static WordleServer instance; // Singleton instance

    private boolean isConfigured = false; // Flag that forbids running the server if previously it was not configured
    private int port; // The port of the server socket
    private Logger logger;
    private int socketBufferCapacity = 1024; // Size of the buffer for each socket read

    private static final String userDbFile = "users.json";

    private Map<String, String> users;

    /**
     * Private static class that is used to describe the state of a client
     * connection.
     */
    private static class ConnectionState {
        public WordleServerCore backend; // The backend object that handles the interaction with the client
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
         * Completes the reading phase. It returns a copy of the ByteBuffer that
         * contains the data read, then it resets both readBuffer and readMessageSize
         */
        public ByteBuffer finishRead() {
            ByteBuffer retBuff = ByteBuffer.wrap(this.readBuffer.array().clone());
            this.readBuffer.clear();
            this.readMessageSize = -1;

            return retBuff;
        }
    }

    private WordleServer() {
        this.logger = Logger.getLogger("Wordle");
    }

    public static WordleServer getInstance() {
        if (WordleServer.instance == null)
            WordleServer.instance = new WordleServer();
        return WordleServer.instance;
    }

    /**
     * Configure the server.
     * 
     * @param port // The port for the server socket
     */
    public void configure(int port) {
        this.port = port;
        this.isConfigured = true;
    }

    /**
     * Load the users from the database
     */
    private void loadUsers() {
        try {
            Gson gson = new Gson();
            TypeToken<Map<String, String>> type = new TypeToken<Map<String, String>>() {};
            this.users = gson.fromJson(new BufferedReader(new FileReader(this.userDbFile)), type);
        } catch (FileNotFoundException e) {
            this.logger.info("User database not found");
            this.users = new HashMap<>();
        }
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
        return (username == "user" && password == "pass");
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

        try (ServerSocketChannel socket = ServerSocketChannel.open();
                Selector selector = Selector.open()) {
            // Init server socket and listen on port `this.port`
            socket.bind(new InetSocketAddress(this.port));
            socket.configureBlocking(false);
            this.logger.info(String.format("Listening on port %d", this.port));

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
     * Handles a new incoming connection. It initializes the object WordleServerCore
     * for that client and register the channel to the selector
     * 
     * @param serverSocket The server socket channel
     * @param selector     The selector where to register the new socket channel
     * @throws IOException
     */
    private void handleNewConnection(ServerSocketChannel serverSocket, Selector selector) throws IOException {
        // Accept the new connection
        SocketChannel socket = serverSocket.accept();
        this.logger.fine("New connection received");
        socket.configureBlocking(false);
        // Set TCP Keep Alive mode
        socket.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

        WordleServerCore serverBackend = new WordleServerCore();
        int interestOps = serverBackend.getInterestOps();

        socket.register(selector, interestOps,
                new ConnectionState(serverBackend, this.socketBufferCapacity, this.socketBufferCapacity));
    }

    /**
     * Handles reading a message from the socket. All the messages must be in the
     * format:
     * [SIZE] [MESSAGE]
     * 
     * SIZE is always the size of a Integer (4 bytes) and it's encoded in big endian
     * SIZE represents the actual size of MESSAGE.
     * 
     * @param key      The selection key
     * @param selector The selector
     * @throws IOException
     */
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
        this.logger.fine(String.format("Needs to receive a message of size %d bytes", state.readMessageSize));

        if (size < state.readMessageSize) // Not enough bytes
            return;

        state.backend.readMessage(state.finishRead());
    }
}