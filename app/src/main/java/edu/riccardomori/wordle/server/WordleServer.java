package edu.riccardomori.wordle.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import edu.riccardomori.wordle.protocol.Constants;
import edu.riccardomori.wordle.rmi.RMIConstants;
import edu.riccardomori.wordle.rmi.RMIStatus;
import edu.riccardomori.wordle.rmi.clientRMI;
import edu.riccardomori.wordle.rmi.serverRMI;
import edu.riccardomori.wordle.rmi.exceptions.PasswordIllegalException;
import edu.riccardomori.wordle.rmi.exceptions.UsernameIllegalException;
import edu.riccardomori.wordle.utils.Pair;

// @formatter:off
// This is the main server class. It is a singleton class. It is thread safe.
// It is responsible for:
//   - Handling all the incoming connections with non-blocking channels
//   - Handling the generation of the secret word
//   - Authenticating the users
//   - Implementing the remote methods
//   - Managing the subscribers
//   - Handling the notification over multicast
// @formatter:on
public final class WordleServer implements serverRMI {
    private static WordleServer instance; // Singleton instance

    // Constants
    // File where to store the previous state of the server
    private static final String SERVER_STATE_FILE = "server_state.json";
    public static final int WORD_MAX_SIZE = 48; // Maximum size in bytes of a word
    public static final int WORD_TRIES = 12; // Number of available tries for each game
    // If there is an update in the leaderboard in a position below this number then the server
    // notifies all the subscribers
    public static final int SUBS_THRESHOLD = 3;

    // Configuration attributes
    private boolean isConfigured = false; // Flag that forbids running the server if it
                                          // has not been previously configured
    private int tcpPort; // The port of the server socket
    private int rmiPort; // The port of the RMI server
    private int swRate; // Secret Word generation rate (in seconds)
    private String wordsDb; // File that contains the secret words to choose from
    private String multicastAddress; // Multicast group address
    private int multicastPort; // Multicast port

    private Logger logger;

    private NetworkInterface multicastInterface;
    private MulticastSocket multicastSocket;
    private volatile ConcurrentMap<String, User> users; // Map {username -> User}
    private volatile String secretWord;
    private volatile long gameId = 0; // The game ID associated with the secret word
    private volatile long sWTime; // Last time the secret word was generated
    private Leaderboard leaderboard;

    // Scheduler for the current word generation
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private HashSet<String> words = new HashSet<>();
    private List<clientRMI> subscribers = new ArrayList<>();

    // Private static class that is used to describe the state of a client connection.
    private static class ConnectionState {
        public ClientSession session; // The session object that handles the interaction with the
                                      // client
        public ByteBuffer readBuffer; // Buffer used for reading
        public ByteBuffer writeBuffer; // Buffer used for writing

        // The size of the application message that needs to be read
        // If it is set to -1 it means that the message size is still unknown
        public int readMessageSize = -1;

        public ConnectionState(ClientSession session, int readCapacity, int writeCapacity) {
            this.session = session;
            // Size of the packet + Max capacity
            this.readBuffer = ByteBuffer.allocate(Integer.BYTES + readCapacity);
            this.writeBuffer = ByteBuffer.allocate(Integer.BYTES + writeCapacity);
        }

        /**
         * Completes the reading phase. It returns a copy of the ByteBuffer that contains the data
         * read, then it resets both {@code readBuffer} and {@code readMessageSize}
         * 
         * @return A {@code ByteBuffer} holding the data read
         */
        public ByteBuffer finishRead() {
            ByteBuffer retBuff = ByteBuffer.wrap(this.readBuffer.array().clone());
            retBuff.limit(this.readBuffer.position());
            this.readBuffer.clear();
            this.readMessageSize = -1;

            return retBuff;
        }

        /**
         * Save the input message in the internal buffer for writable data. It encapsulates the
         * message in the following packet: [SIZE] [MESSAGE]
         * 
         * @param data The input data
         */
        public void setWritableMessage(ByteBuffer message) {
            this.writeBuffer.clear();
            this.writeBuffer.putInt(message.limit());
            this.writeBuffer.put(message);
            this.writeBuffer.flip();
        }
    }

    private WordleServer() {
        this.logger = Logger.getLogger("Wordle");

        // Register a shutdown hook to keep syncronized the persistent state
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.logger.finer("Exiting, saving the server state");
            this.flush();
        }));
    }

    /**
     * Get singleton instance. Note that the instance must be configured before being able to run it
     * 
     * @return the singleton instance
     * @see configure
     */
    public static WordleServer getInstance() {
        if (WordleServer.instance == null)
            WordleServer.instance = new WordleServer();
        return WordleServer.instance;
    }

    /**
     * Save the current state to the save file
     */
    private void flush() {
        // Check whether the data has been loaded before so we don't overwrite the file
        if (this.users == null)
            return;

        Gson gson = new Gson();

        try (JsonWriter writer = new JsonWriter(new BufferedWriter(
                new FileWriter(WordleServer.SERVER_STATE_FILE, StandardCharsets.UTF_8)))) {

            writer.beginObject(); // Begin root object
            writer.name("lastGameID");
            writer.value(this.gameId);
            writer.name("users");

            // When serializing, in order to avoid concurrent modification to the User objects it is
            // mandatory to gain the lock over each one of them
            writer.beginObject();
            for (Map.Entry<String, User> entry : this.users.entrySet()) {
                writer.name(entry.getKey());
                User user = entry.getValue();
                synchronized (user) {
                    writer.jsonValue(gson.toJson(user));
                }
            }
            writer.endObject();

            writer.endObject(); // End root object
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Initialize the server and all its components
     */
    private void initialize() {
        // Check that all the parameters have been configured
        if (!this.isConfigured)
            throw new RuntimeException("The server must be configured before running.");

        // Initialize the multicastSocket
        this.initMulticastSocket();

        // Load the previous server state
        this.loadPrevState();

        // Load words
        this.loadWords();

        // Run the scheduled services
        this.runScheduler();

        // Run RMI services
        this.runRMIServer();
    }

    /**
     * Initialize the multicast socket
     */
    private void initMulticastSocket() {
        try {
            // Get one valid interface for multicast
            // If more than one is found, choose one of them non-deterministically
            this.multicastInterface = NetworkInterface.networkInterfaces().filter(iface -> {
                try {
                    return iface.isUp() && iface.supportsMulticast() && !iface.isLoopback()
                            && !iface.getName().equals("");
                } catch (SocketException e) {
                    return false;
                }
            }).findAny().get();
            this.logger.info(String.format("Using interface %s for multicast",
                    this.multicastInterface.getDisplayName()));

            // Create socket and join multicast group
            this.multicastSocket = new MulticastSocket(this.multicastPort);
            this.multicastSocket.joinGroup(
                    new InetSocketAddress(this.multicastAddress, this.multicastPort),
                    this.multicastInterface);
        } catch (NoSuchElementException | SocketException e) {
            this.logger.severe("Cannot find a valid interface for multicast notifications");
        } catch (IOException e) {
            this.logger.severe("Cannot create a multicast socket");
        }
    }

    /**
     * Load the previous state from SERVER_STATE_FILE. This will load the users and initialize the
     * previous server state.
     */
    private void loadPrevState() {
        try (JsonReader reader = new JsonReader(new BufferedReader(
                new FileReader(WordleServer.SERVER_STATE_FILE, StandardCharsets.UTF_8)))) {
            Gson gson = new Gson();

            // Parse the initial Object
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();

                if (name.equals("users")) { // All the users
                    TypeToken<ConcurrentMap<String, User>> type =
                            new TypeToken<ConcurrentMap<String, User>>() {};
                    this.users = gson.fromJson(reader, type);

                } else if (name.equals("lastGameID")) { // Last game ID
                    this.gameId = reader.nextLong();

                } else { // Ignored
                    this.logger.warning(String
                            .format("The server state file is corrupted. Unknown key `%s`", name));
                    reader.skipValue();
                }
            }
            reader.endObject();

        } catch (FileNotFoundException e) {
            this.logger.info("User database not found");
            this.users = new ConcurrentHashMap<String, User>();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Generate the leaderboard
        this.leaderboard = new Leaderboard(Collections.unmodifiableCollection(this.users.values()));
    }

    /**
     * Load all the possible secret words in memory
     */
    private void loadWords() {
        try (BufferedReader input =
                new BufferedReader(new FileReader(this.wordsDb, StandardCharsets.UTF_8))) {
            String line;
            while ((line = input.readLine()) != null)
                this.words.add(line);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Run the scheduler
     */
    private void runScheduler() {
        // Change the secret word at the specified rate
        this.scheduler.scheduleAtFixedRate(() -> {
            // Choose a random word that is different from the current one
            String newWord;
            do {
                int randPos = ThreadLocalRandom.current().nextInt(this.words.size());
                newWord = this.words.stream().skip(randPos).findFirst().get();
            } while (newWord.equals(this.secretWord));

            // Update new secret word
            this.secretWord = newWord;
            this.gameId++;
            this.sWTime = System.currentTimeMillis();
            this.logger.info(String.format("Secret word changed to `%s`", newWord));
        }, 0, this.swRate, TimeUnit.SECONDS);

        // Periodically call this.flush
        this.scheduler.scheduleWithFixedDelay(() -> {
            this.flush();
        }, 60, 120, TimeUnit.SECONDS);
    }

    /**
     * Register all the RMI services
     */
    private void runRMIServer() {
        try {
            serverRMI stub = (serverRMI) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.createRegistry(this.rmiPort);
            registry.rebind(RMIConstants.SERVER_NAME, stub);
        } catch (RemoteException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Configure the server.
     * 
     * @param multicastAddress // IP address for the multicast notification service
     * @param multicastPort // Port number for the multicast notification service
     * @param tcpPort // The port for the server socket
     * @param rmiPort // The port for the RMI server
     * @param swRate // Refresh rate (in seconds) for the secret word
     * @param wordsDb // The file that contains all the secret words to choose from
     */
    public void configure(String multicastAddress, int multicastPort, int tcpPort, int rmiPort,
            int swRate, String wordsDb) {
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.tcpPort = tcpPort;
        this.rmiPort = rmiPort;
        this.swRate = swRate;
        this.wordsDb = wordsDb;
        this.isConfigured = true;
    }

    @Override
    public RMIStatus register(String username, String password) throws RemoteException {
        this.logger.info("New registration");

        // Check username and password
        if (!username.matches("^[a-zA-Z0-9][a-zA-Z0-9_]{2,64}$")) {
            throw new UsernameIllegalException("username not valid");
        }
        if (!password.matches("^[a-zA-Z0-9_`~!@#$%^&*()\\-=+{}\\[\\];:''\",<.>/?\\\\|]{4,64}$")) {
            throw new PasswordIllegalException("password not valid");
        }

        synchronized (this.users) {
            // Check if username exists
            if (this.users.containsKey(username))
                return RMIStatus.USER_TAKEN;

            // Add the user
            this.users.put(username, new User(username, password));
        }

        return RMIStatus.SUCCESS;
    }

    @Override
    public void subscribe(clientRMI client) throws RemoteException {
        synchronized (this.subscribers) {
            if (this.subscribers.contains(client))
                return;

            this.logger.finer("New subscription");
            this.subscribers.add(client);
        }
    }

    @Override
    public void cancelSubscription(clientRMI client) throws RemoteException {
        synchronized (this.subscribers) {
            this.subscribers.remove(client);
        }
        this.logger.finer("Removing a subscriber");
    }

    /**
     * Check if the pair ({@code username}, {@code password}) correctly identifies a real user, if
     * it does then returns the {@code User} identified, otherwise returns {@code null}
     * 
     * @param username The username
     * @param password The password
     * @return The {@code User} identified by the pair ({@code username}, {@code password}),
     *         {@code null} otherwise
     */
    public User getUser(String username, String password) {
        if (this.users.containsKey(username) && this.users.get(username).passwordMatch(password))
            return this.users.get(username);
        return null;
    }

    /**
     * Returns the current secret word and its game ID
     * 
     * @return The {@code Pair} (current word, gameID)
     */
    public Pair<String, Long> getCurrentWord() {
        return new Pair<String, Long>(this.secretWord, this.gameId);
    }

    /**
     * Tells whether the {@code word} is valid
     * 
     * @param word
     * @return True if it is valid, false otherwise
     */
    public boolean isValidWord(String word) {
        return this.words.contains(word);
    }

    /**
     * Returns the the next time (in unix time in milliseconds) the secret word is changed
     * 
     * @return Unix timestamp in milliseconds
     */
    public long getNextSWTime() {
        return this.sWTime + this.swRate * 1000;
    }

    /**
     * Returns the top positions in the leaderboard (up to {@code SUBS_THRESHOLD}) as a list of
     * pairs (username, score)
     * 
     * @return The top positions in the leaderboard (up to {@code SUBS_THRESHOLD})
     */
    public List<Pair<String, Double>> getTopLeaderboard() {
        return this.leaderboard.get(WordleServer.SUBS_THRESHOLD);
    }

    /**
     * Returns the full leaderboard as a list of pairs (username, score)
     * 
     * @return The full leaderboard
     */
    public List<Pair<String, Double>> getFullLeaderboard() {
        return this.leaderboard.get();
    }

    /**
     * Update the leaderboard by repositioning (updating its score) {@code username}. This might
     * call the subscribers callback, in case there is a change in the first {@code SUBS_THRESHOLD}
     * positions.
     * 
     * @param username The user for which the score must be updated
     */
    public void updateLeaderboard(String username, double score) {
        int pos = this.leaderboard.update(username, score);
        if (pos >= 0 && pos < WordleServer.SUBS_THRESHOLD)
            this.notifySubscribers();
    }

    /**
     * Share asynchronously a completed game in the multicast group
     * 
     * @param game The game to be shared
     * @param username The user that played the game
     */
    public void shareGame(GameDescriptor game, String username) {
        // Run it in a new thread to avoid slowing down the server
        new Thread(() -> {
            // Create the message packet
            ByteBuffer msg = ByteBuffer.allocate(Constants.UDP_MSG_MAX_SIZE);
            ByteBuffer encUsername = StandardCharsets.UTF_8.encode(username);
            msg.putInt(encUsername.limit());
            msg.put(encUsername);
            msg.putLong(game.gameId);
            msg.put((byte) game.tries);
            msg.put((byte) game.maxTries);
            msg.put((byte) game.wordLen);
            msg.put((byte) game.correct.length);
            assert game.correct.length == game.partial.length;
            for (int k = 0; k < game.correct.length; ++k) {
                msg.put((byte) game.correct[k].length);
                msg.put((byte) game.partial[k].length);
                for (int j = 0; j < game.correct[k].length; ++j)
                    msg.put((byte) game.correct[k][j]);
                for (int j = 0; j < game.partial[k].length; ++j)
                    msg.put((byte) game.partial[k][j]);
            }
            msg.flip();

            // Send it
            try {
                DatagramPacket packet = new DatagramPacket(msg.array(), msg.limit(),
                        InetAddress.getByName(this.multicastAddress), this.multicastPort);
                this.multicastSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Notify all the subscribers by calling the callback registered. It runs in a separate thread
     */
    public void notifySubscribers() {
        // Run in a separate thread to avoid slowing down the server
        new Thread(() -> {
            this.logger.fine("Notifying all the subscribers");

            List<Pair<String, Double>> leaderboard =
                    this.leaderboard.get(WordleServer.SUBS_THRESHOLD);
            synchronized (this.subscribers) {
                Iterator<clientRMI> it = this.subscribers.iterator();
                while (it.hasNext()) {
                    clientRMI sub = it.next();
                    try {
                        sub.updateLeaderboard(leaderboard);
                    } catch (RemoteException e) {
                        // Remove the subscriber from the list
                        it.remove();
                    }
                }
            }
        }).start();
    }

    /**
     * The server main loop where it performs the multiplexing of the channels. The server must be
     * previously configured by calling WotdleServer.configure()
     */
    public void run() {
        this.initialize();

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

                    if (key.isAcceptable()) {
                        // Handle new connection
                        this.handleNewConnection(socket, selector);
                    } else if (key.isReadable()) {
                        this.handleRead(key);
                    } else if (key.isWritable()) {
                        this.handleWrite(key);
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
     * Handles a new incoming connection. It initializes the ClientSession for that client and
     * register the channel to the selector
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

        ClientSession clientSession = new ClientSession();
        int interestOps = clientSession.getInterestOps();

        socket.register(selector, interestOps, new ConnectionState(clientSession,
                Constants.SOCKET_MSG_MAX_SIZE, Constants.SOCKET_MSG_MAX_SIZE));
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
     * @throws IOException
     */
    // @formatter:on
    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        ConnectionState state = (ConnectionState) key.attachment();

        // Read data from the socket
        int nRead = socket.read(state.readBuffer);
        // Connection closed by client
        if (nRead < 0) {
            this.logger.finer("Connection closed");
            state.session.close();
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

            // If the size is not acceptable close the connection
            if (state.readMessageSize > Constants.SOCKET_MSG_MAX_SIZE) {
                this.logger.info(String.format(
                        "Message (%d bytes) exceeds maximum size. Closing connection.",
                        state.readMessageSize));
                state.session.close();
                socket.close();
                return;
            }
        }

        // Here we know the app message size
        this.logger.fine(String.format("Needs to receive a message of size %d bytes",
                state.readMessageSize));

        if (size < state.readMessageSize) // Not enough bytes
            return;
        if (size > state.readMessageSize) // Message is too long
            this.logger.warning(String.format(
                    "Received a message longer than what previously advertised (%d over %d bytes)",
                    size, state.readMessageSize));

        // Handle the message and update the interest ops
        int newInterestOps = state.session.handleMessage(state.finishRead());
        if ((newInterestOps & SelectionKey.OP_WRITE) != 0)
            state.setWritableMessage(state.session.getWriteBuffer());
        key.interestOps(newInterestOps);
    }

    // @formatter:off
    /**
     * Handles writing a message in the socket. All the messages must be in the format:
     *      [SIZE] [MESSAGE]
     * 
     * SIZE is always the size of a Integer (4 bytes) and it's encoded in big endian SIZE represents
     * the actual size of MESSAGE.
     * 
     * @param key The selection key
     * @throws IOException
     */
    // @formatter:on
    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        ConnectionState state = (ConnectionState) key.attachment();

        // Write data in the socket
        socket.write(state.writeBuffer);
        if (!state.writeBuffer.hasRemaining()) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }
}
