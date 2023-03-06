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

public class WordleServerCore {
    private int interestOps; // The interest set of operations as a bitmask
    private ClientState state = new ClientState();
    private Logger logger;

    private String username; // The username of the client who is running this session
    private ByteBuffer writeBuf; // The buffer holding the writable data
    private String lastPlayedSecretWord; // Last played secret word
    private int triesLeft;

    public WordleServerCore() {
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
        int usernameSize = msg.get();
        int passwordSize = msg.get();
        byte[] usernameRaw = new byte[usernameSize];
        byte[] passwordRaw = new byte[passwordSize];
        try {
            msg.get(usernameRaw, 0, usernameSize);
            msg.get(passwordRaw, 0, passwordSize);
        } catch (BufferUnderflowException e) {
            this.logger.finer("Malformed login message");
            return;
        }
        String username = new String(usernameRaw, StandardCharsets.US_ASCII);
        String password = new String(passwordRaw, StandardCharsets.US_ASCII);

        this.logger.finest(String.format("user %s pass %s", username, password));

        // Authenticate
        if (WordleServer.getInstance().checkLogin(username, password)) {
            this.logger.finer(String.format("User `%s` logged in", username));
            this.state.login();
            this.username = username;

            // Prepare the success message
            this.sendMessage(MessageStatus.SUCCESS);
        } else {
            this.logger.finer(String.format("Authentication of user `%s` rejected", username));
            // Prepare the auth rejected message
            this.sendMessage(MessageStatus.INVALID_USER);
        }
    }

    private void logoutHandler() {
        this.logger.info(String.format("User `%s`: action Logout", this.username));

        this.state.logout();
        this.username = null;

        // Prepare the success message
        this.sendMessage(MessageStatus.SUCCESS);
    }

    private void playHandler() {
        this.logger.info(String.format("User `%s`: action Play", this.username));

        // Get the current word from server
        String secretWord = WordleServer.getInstance().getCurrentWord();

        // Check if player already played with that word
        if (secretWord.equals(this.lastPlayedSecretWord)) {
            this.sendMessage(MessageStatus.ALREADY_PLAYED);
            return;
        }

        // Update the session state
        this.state.play();
        this.lastPlayedSecretWord = secretWord;
        this.triesLeft = WordleServer.getInstance().getWordTries();

        // Prepare the success message
        ByteBuffer msg = ByteBuffer.allocate(Integer.BYTES * 2);
        msg.putInt(secretWord.length());
        msg.putInt(this.triesLeft);
        msg.flip();
        this.sendMessage(MessageStatus.SUCCESS, msg);
    }

    private void guessWordHandler(ByteBuffer msg) {
        // No leftover tries
        if (this.triesLeft == 0) {
            // Send the time to the next secret word
            this.sendMessage(MessageStatus.NO_TRIES_LEFT,
                    WordleServer.getInstance().getNextSWTime());
            return;
        }

        // Read the guessed word
        String guessWord = StandardCharsets.UTF_8.decode(msg).toString();

        this.logger.info(String.format("User `%s` guessed word `%s` (secret `%s`)", this.username,
                guessWord, this.lastPlayedSecretWord));

        // Invalid word
        if (!WordleServer.getInstance().isValidWord(guessWord)) {
            this.sendMessage(MessageStatus.INVALID_WORD, (byte) this.triesLeft);
            return;
        }

        // Spend a try
        this.triesLeft--;

        // Client won
        if (this.lastPlayedSecretWord.equals(guessWord)) {
            // congratz
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
        ByteBuffer sMsg = ByteBuffer.allocate(3 + correct.size() + partial.size());
        sMsg.put((byte) this.triesLeft);
        sMsg.put((byte) correct.size());
        sMsg.put((byte) partial.size());
        for (int p : correct)
            sMsg.put((byte) p);
        for (int p : partial)
            sMsg.put((byte) p);
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
                    this.playHandler();
                    break;

                default:
                    this.logger.info(String.format("User `%s` not allowed to perform this action",
                            this.username));
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
                            this.username));
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
