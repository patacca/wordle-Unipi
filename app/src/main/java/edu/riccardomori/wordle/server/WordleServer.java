package edu.riccardomori.wordle.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.net.URL;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import edu.riccardomori.wordle.rmi.RMIConstants;
import edu.riccardomori.wordle.rmi.RMIStatus;
import edu.riccardomori.wordle.rmi.clientRMI;
import edu.riccardomori.wordle.rmi.serverRMI;
import edu.riccardomori.wordle.rmi.exceptions.PasswordIllegalException;
import edu.riccardomori.wordle.rmi.exceptions.UsernameIllegalException;
import edu.riccardomori.wordle.utils.LRUCache;
import edu.riccardomori.wordle.utils.Pair;

/**
 * This is the main server class. It handles all the incoming connections with non-blocking
 * channels. It also handles the generation of the secret word and the authentication of a pair
 * (user, password). It is a singleton class.
 */
// TODO every once in a while flush everything to the db
public final class WordleServer implements serverRMI {
    private static WordleServer instance; // Singleton instance

    // Constants
    // File where to store the previous state of the server
    private static final String SERVER_STATE_FILE = "server_state.json";
    private static final int TRANSLATION_CACHE = 512;
    public static final int SOCKET_MSG_MAX_SIZE = 1024; // Maximum size for each message
    public static final int UDP_MSG_MAX_SIZE = 512; // Maximum size for a UDP message
    public static final int WORD_MAX_SIZE = 48; // Maximum size in bytes of a word
    public static final int WORD_TRIES = 12; // Number of available tries for each game

    // If there is a change in the leaderboard in a position below this number then the server
    // notifies all the subscribers
    public static final int SUBS_THRESHOLD = 3;

    // Attributes
    private boolean isConfigured = false; // Flag that forbids running the server if previously it
                                          // was not configured
    private int tcpPort; // The port of the server socket
    private int rmiPort; // The port of the RMI server
    private int swRate; // Secret Word generation rate (in seconds)
    private String wordsDb; // File that contains the secret words to choose from
    private String multicastAddress;
    private int multicastPort;

    private Logger logger;

    // Scheduler for the current word generation
    private NetworkInterface multicastInterface;
    private MulticastSocket multicastSocket;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Map<String, User> users;
    private volatile String secretWord; // Secret Word
    private volatile long gameId = 0; // The game ID associated with the secret word
    private volatile long sWTime;
    private HashSet<String> words = new HashSet<>();
    private Leaderboard leaderboard;
    private List<clientRMI> subscribers = new ArrayList<>();
    // Caching the translations of the secret words. TODO make it thread-safe
    private LRUCache<String, String> translationCache =
            new LRUCache<>(WordleServer.TRANSLATION_CACHE);

    /**
     * Private static class that is used to describe the state of a client connection.
     */
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
         * read, then it resets both readBuffer and readMessageSize
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

    private void flush() {
        String usersData;
        synchronized (this.users) {
            // Check whether the data has been loaded before so we don't overwrite the file
            if (this.users == null)
                return;

            Gson gson = new Gson();
            usersData = gson.toJson(this.users);
        }

        try (JsonWriter writer = new JsonWriter(
                new BufferedWriter(new FileWriter(WordleServer.SERVER_STATE_FILE)))) {

            // Write the whole object
            writer.beginObject();
            writer.name("lastGameID");
            writer.value(this.gameId);
            writer.name("users");
            writer.jsonValue(usersData);
            writer.endObject();
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

        // Load words
        this.loadWords();

        // Load the previous server state
        this.loadPrevState();

        // Run the scheduled services
        this.runScheduler();

        // Run RMI services
        this.runRMIServer();
    }

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
        try (JsonReader reader = new JsonReader(
                new BufferedReader(new FileReader(WordleServer.SERVER_STATE_FILE)))) {
            Gson gson = new Gson();

            // Parse the initial Object
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();

                if (name.equals("users")) { // All the users
                    TypeToken<Map<String, User>> type = new TypeToken<Map<String, User>>() {};
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
            this.users = new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Load leaderboard
        this.leaderboard = new Leaderboard(Collections.unmodifiableCollection(this.users.values()),
                WordleServer.SUBS_THRESHOLD);
    }

    /**
     * Load all the possible words in memory
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

    private void runScheduler() {
        // Change the secret word at the specified rate
        this.scheduler.scheduleAtFixedRate(() -> {
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

    // TODO Make it thread safe
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

        // Check if username exists
        if (this.users.containsKey(username))
            return RMIStatus.USER_TAKEN;

        // Save the user and sync the database
        this.users.put(username, new User(username, password));
        this.flush();

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
     * Check if the pair (username, password) correctly identifies a real user, if it does then
     * returns the {@code User} identified, otherwise returns {@code null}
     * 
     * @param username The username
     * @param password The password
     * @return The {@code User} identified by the pair (username, password), {@code null} otherwise
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

    public boolean isValidWord(String word) {
        return this.words.contains(word);
    }

    public long getNextSWTime() {
        return this.sWTime + this.swRate * 1000;
    }

    public List<Pair<String, Double>> getTopLeaderboard() {
        return this.leaderboard.get(WordleServer.SUBS_THRESHOLD);
    }

    public List<Pair<String, Double>> getFullLeaderboard() {
        return this.leaderboard.get();
    }

    public String translateWord(String word) {
        // Cache lookup first
        if (this.translationCache.containsKey(word))
            return this.translationCache.get(word);

        // HTTP request to mymemory
        try {
            URL url = new URL(String
                    .format("https://api.mymemory.translated.net/get?q=%s&langpair=en|it", word));
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            // Parse json
            try (JsonReader reader = new JsonReader(
                    new BufferedReader(new InputStreamReader(connection.getInputStream())))) {

                String translation = null;

                // @formatter:off
                // The message has this format:
                //   {"responseData": {"translatedText": String, ...}, ...}
                // @formatter:on
                reader.beginObject();
                while (reader.hasNext()) { // Whole response object
                    String name = reader.nextName();

                    if (name.equals("responseData")) {
                        reader.beginObject();
                        while (reader.hasNext()) { // Whole responseData object
                            name = reader.nextName();
                            if (name.equals("translatedText"))
                                translation = reader.nextString();
                            else
                                reader.skipValue();
                        }
                        reader.endObject();
                    } else // Ignore anything else
                        reader.skipValue();
                }
                reader.endObject();

                if (translation == null) {
                    this.logger.severe("Cannot parse the json response from the mymemory server");
                    return null;
                }

                this.translationCache.put(word, translation);
                return translation;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void shareGame(GameDescriptor game, String username) {
        // Run it in a new thread to avoid slowing down the server
        new Thread(() -> {
            // Create the message packet
            ByteBuffer msg = ByteBuffer.allocate(WordleServer.UDP_MSG_MAX_SIZE);
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
            try (DatagramSocket ds = new DatagramSocket()) {
                DatagramPacket packet = new DatagramPacket(msg.array(), msg.limit(),
                        InetAddress.getByName(this.multicastAddress), this.multicastPort);
                ds.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Notify all the subscribers by calling the callback registered
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
                    // for (clientRMI sub : this.subscribers) {
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
     * Update the leaderboard by repositioning (updating its score) {@code username}. This might
     * call the subscribers callback, in case there is a change in the first 3 ranks.
     * 
     * @param username The user for which the score must be updated
     */
    public void updateLeaderboard(String username, double score) {
        this.leaderboard.update(username, score);
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

                    if (!key.isValid())
                        this.logger.warning("KEY NOT VALID!");

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

        ClientSession clientSession = new ClientSession();
        int interestOps = clientSession.getInterestOps();

        socket.register(selector, interestOps, new ConnectionState(clientSession,
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
            if (state.readMessageSize > WordleServer.SOCKET_MSG_MAX_SIZE) {
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
