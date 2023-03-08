package edu.riccardomori.wordle.server;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import edu.riccardomori.wordle.protocol.Action;
import edu.riccardomori.wordle.protocol.ClientState;
import edu.riccardomori.wordle.protocol.MessageStatus;
import edu.riccardomori.wordle.server.WordleServer;

public class UserSession {
    private int interestOps; // The interest set of operations as a bitmask
    private ClientState state = new ClientState();
    private Logger logger;
    private boolean sessionIsActive = true; // Tells if the session is active or if it has been
                                            // closed

    private User user; // The user who is running this session
    private ByteBuffer writeBuf; // The buffer holding the writable data
    private String lastPlayedSecretWord; // Last played secret word
    private int triesLeft;

    public UserSession() {
        this.interestOps = SelectionKey.OP_READ;
        this.writeBuf = ByteBuffer.allocate(WordleServer.SOCKET_MSG_MAX_SIZE);

        this.logger = Logger.getLogger("Wordle");
    }

    public int getInterestOps() {
        return this.interestOps;
    }

    public ByteBuffer getWriteBuffer() {
        return this.writeBuf;
    }

    public void close() {
        this.sessionIsActive = false;
    }

    public boolean isActive() {
        return this.sessionIsActive;
    }

    /**
     * Utility function to prepare a message to send to the client and set the interestOps to
     * OP_WRITE
     * 
     * @param code The return code that is set in the message
     * @param message An additional message to send
     */
    private void sendMessage(MessageStatus code, ByteBuffer message) {
        this.writeBuf.clear();
        this.writeBuf.put(code.getValue());
        this.writeBuf.put(message);
        this.writeBuf.flip();
        this.interestOps = SelectionKey.OP_WRITE;
    }

    /**
     * Encode the string message to bytes with utf-8 and calls
     * {@code sendMessage(code, byteMessage)}
     * 
     * @param code The return code that is set in the message
     * @param message An additional message to send
     */
    private void sendMessage(MessageStatus code, String message) {
        this.sendMessage(code, StandardCharsets.UTF_8.encode(message));
    }

    /**
     * Calls {@code sendMessage(code, "")}
     * 
     * @param code The return code that is set in the message
     */
    private void sendMessage(MessageStatus code) {
        this.sendMessage(code, ByteBuffer.allocate(0));
    }

    /**
     * Same as {@code sendMessage(MessageStatus, ByteBuffer)} but put only the byte {@code value} in
     * the {@code ByteBuffer}
     * 
     * @param code The return code that is set in the message
     */
    private void sendMessage(MessageStatus code, byte value) {
        this.writeBuf.clear();
        this.writeBuf.put(code.getValue());
        this.writeBuf.put(value);
        this.writeBuf.flip();
        this.interestOps = SelectionKey.OP_WRITE;
    }

    /**
     * Same as {@code sendMessage(MessageStatus, ByteBuffer)} but put only the long {@code value} in
     * the {@code ByteBuffer}
     * 
     * @param code The return code that is set in the message
     */
    private void sendMessage(MessageStatus code, long value) {
        this.writeBuf.clear();
        this.writeBuf.put(code.getValue());
        this.writeBuf.putLong(value);
        this.writeBuf.flip();
        this.interestOps = SelectionKey.OP_WRITE;
    }

    /**
     * Handler for the action login.
     * 
     * @param msg The message that has been sent by the client
     */
    private void loginHandler(ByteBuffer msg) {
        this.logger.info("Action Login");

        // Parse message
        String username;
        String password;
        try {
            int usernameSize = msg.get();
            int passwordSize = msg.get();
            byte[] usernameRaw = new byte[usernameSize];
            byte[] passwordRaw = new byte[passwordSize];
            msg.get(usernameRaw, 0, usernameSize);
            msg.get(passwordRaw, 0, passwordSize);

            username = new String(usernameRaw, StandardCharsets.US_ASCII);
            password = new String(passwordRaw, StandardCharsets.US_ASCII);
        } catch (BufferUnderflowException e) {
            this.logger.finer("Malformed login message");
            return;
        }

        this.logger.finest(String.format("user %s pass %s", username, password));

        // Authenticate
        WordleServer serverInstance = WordleServer.getInstance();
        User user = serverInstance.getUser(username, password);
        if (user == null) {
            this.logger.finer(String.format("Authentication of user `%s` rejected", username));
            // Prepare the auth rejected message
            this.sendMessage(MessageStatus.INVALID_USER);
            return;
        }

        // Enter synchronized block
        synchronized (serverInstance) {
            // Get the previous session if any
            UserSession previousSession = serverInstance.getUserSession(username);
            if (previousSession != null) {
                // Cannot login more than once at the same time
                if (previousSession.isActive()) {
                    this.logger.finer(
                            String.format("User `%s` already logged in. Rejected", username));
                    this.sendMessage(MessageStatus.ALREADY_LOGGED);
                    return;
                }

                // Restore the previous session
                this.lastPlayedSecretWord = previousSession.lastPlayedSecretWord;

                // Update the session
                serverInstance.saveUserSession(username, this);
            }
        }

        // Update the state
        this.state.login();
        this.user = user;

        this.logger.finer(String.format("User `%s` logged in", username));

        // Prepare the success message
        this.sendMessage(MessageStatus.SUCCESS);
    }

    private void logoutHandler() {
        this.logger.info(String.format("User `%s`: action Logout", this.user.getUsername()));

        // Clean the state
        this.state.logout();
        this.sessionIsActive = false;
        this.user = null;

        // Prepare the success message
        this.sendMessage(MessageStatus.SUCCESS);
    }

    private void startGameHandler() {
        this.logger.info(String.format("User `%s`: action playWORDLE", this.user.getUsername()));

        // Get the current word from server
        String secretWord = WordleServer.getInstance().getCurrentWord();

        // Check if player already played with that word
        if (secretWord.equals(this.lastPlayedSecretWord)) {
            // Send the time to the next secret word
            this.sendMessage(MessageStatus.ALREADY_PLAYED,
                    WordleServer.getInstance().getNextSWTime());
            return;
        }

        // Update the session state
        this.state.play();
        this.lastPlayedSecretWord = secretWord;
        this.triesLeft = WordleServer.getInstance().getWordTries();

        // Prepare the success message
        ByteBuffer msg = ByteBuffer.allocate(Integer.BYTES * 2);
        msg.put((byte) secretWord.length());
        msg.put((byte) this.triesLeft);
        msg.flip();
        this.sendMessage(MessageStatus.SUCCESS, msg);
    }

    private void guessWordHandler(ByteBuffer msg) {
        // No leftover tries
        if (this.triesLeft == 0) {
            this.state.stopPlaying();

            // Send the time to the next secret word
            this.sendMessage(MessageStatus.NO_TRIES_LEFT,
                    WordleServer.getInstance().getNextSWTime());
            return;
        }

        // Read the guessed word
        String guessWord = StandardCharsets.UTF_8.decode(msg).toString();

        this.logger.info(String.format("User `%s` guessed word `%s` (secret `%s`)",
                this.user.getUsername(), guessWord, this.lastPlayedSecretWord));

        // Invalid word
        if (!WordleServer.getInstance().isValidWord(guessWord)) {
            this.sendMessage(MessageStatus.INVALID_WORD, (byte) this.triesLeft);
            return;
        }

        // Spend a try
        this.triesLeft--;

        // If there are no available tries left, end the game
        if (this.triesLeft == 0)
            this.state.stopPlaying();

        // Client won
        if (this.lastPlayedSecretWord.equals(guessWord)) {
            ByteBuffer sMsg = ByteBuffer.allocate(3 + guessWord.length());
            sMsg.put((byte) this.triesLeft);
            sMsg.put((byte) guessWord.length()); // correct size
            sMsg.put((byte) 0); // partial size
            for (int k = 0; k < guessWord.length(); ++k)
                sMsg.put((byte) k);
            sMsg.flip();

            this.state.stopPlaying();

            this.sendMessage(MessageStatus.SUCCESS, sMsg);
            return;
        }

        // Generate hints
        List<Integer> correct = new ArrayList<>();
        List<Integer> partial = new ArrayList<>();
        Map<Character, Integer> map = new HashMap<>();
        for (int k = 0; k < this.lastPlayedSecretWord.length(); ++k) {
            if (this.lastPlayedSecretWord.charAt(k) == guessWord.charAt(k)) {
                correct.add(k);
            } else {
                int v = map.computeIfAbsent(this.lastPlayedSecretWord.charAt(k), i -> 0);
                map.put(this.lastPlayedSecretWord.charAt(k), v + 1);
            }
        }
        for (int k = 0; k < this.lastPlayedSecretWord.length(); ++k) {
            if (this.lastPlayedSecretWord.charAt(k) != guessWord.charAt(k)) {
                int c = map.getOrDefault(guessWord.charAt(k), 0);
                if (c > 0) {
                    partial.add(k);
                    map.put(guessWord.charAt(k), c - 1);
                }
            }
        }

        // Forge message
        // Let's make the buffer bigger than necessary to avoid unnecessary calculations
        ByteBuffer sMsg = ByteBuffer.allocate(
                3 + correct.size() + partial.size() + 4 * this.lastPlayedSecretWord.length());
        sMsg.put((byte) this.triesLeft);
        sMsg.put((byte) correct.size());
        sMsg.put((byte) partial.size());
        for (int p : correct)
            sMsg.put((byte) p);
        for (int p : partial)
            sMsg.put((byte) p);

        // No more tries left. Send the secret word
        if (this.triesLeft == 0)
            sMsg.put(this.lastPlayedSecretWord.getBytes(StandardCharsets.UTF_8));

        sMsg.flip();

        this.sendMessage(MessageStatus.SUCCESS, sMsg);
    }

    /**
     * Read the message provided in the buffer and perform the action requested considering the
     * current state of the session. Note that the message **must** always be complete. A partial or
     * malformed message results in it being rejected.
     * 
     * @param buffer The buffer containing the message coming from the client. It must be ready to
     *        be read
     * @return The interest set of operations
     */
    public int handleMessage(ByteBuffer buffer) {
        this.logger.finest(String.format("Received a message of size %d", buffer.limit()));

        if (this.state.isAnonymous()) { // Anonymous
            switch (Action.fromByte(buffer.get())) {
                case LOGIN:
                    this.loginHandler(buffer);
                    break;

                default:
                    this.logger.info("Not allowed to perform this action");
                    this.sendMessage(MessageStatus.ACTION_UNAUTHORIZED);
                    break;
            }

        } else if (this.state.isLogged() && !this.state.isPlaying()) { // Logged but not playing
            switch (Action.fromByte(buffer.get())) {
                case LOGOUT:
                    this.logoutHandler();
                    break;

                case PLAY:
                    this.startGameHandler();
                    break;

                default:
                    this.logger.info(String.format("User `%s` not allowed to perform this action",
                            this.user.getUsername()));
                    this.sendMessage(MessageStatus.ACTION_UNAUTHORIZED);
                    break;
            }

        } else if (this.state.isLogged() && this.state.isPlaying()) { // Logged and playing
            switch (Action.fromByte(buffer.get())) {
                case SEND_WORD:
                    this.guessWordHandler(buffer);
                    break;

                case LOGOUT:
                    this.logoutHandler();
                    break;

                default:
                    this.logger.info(String.format("User `%s` not allowed to perform this action",
                            this.user.getUsername()));
                    this.sendMessage(MessageStatus.ACTION_UNAUTHORIZED);
                    break;
            }

        } else { // Else
            this.logger.info("Not allowed to perform this action");
            this.sendMessage(MessageStatus.ACTION_UNAUTHORIZED);
        }

        return this.interestOps;
    }
}
