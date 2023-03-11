package edu.riccardomori.wordle.server;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;
import edu.riccardomori.wordle.protocol.Action;
import edu.riccardomori.wordle.protocol.ClientState;
import edu.riccardomori.wordle.protocol.Constants;
import edu.riccardomori.wordle.protocol.MessageStatus;
import edu.riccardomori.wordle.utils.Pair;

// Class that handles a client session. The client is solely identified by the tcp session.
// During a session, the same client can operate different users.
// This class is not thread-safe.
public class ClientSession {
    private int interestOps; // The interest set of operations as a bitmask
    private ClientState state = new ClientState(); // The current state of the client
    private Logger logger;

    private User user; // The user who is running this session
    private ByteBuffer writeBuf; // The buffer holding the writable data

    public ClientSession() {
        this.interestOps = SelectionKey.OP_READ;
        this.writeBuf = ByteBuffer.allocate(Constants.SOCKET_MSG_MAX_SIZE);

        this.logger = Logger.getLogger("Wordle");
    }

    public int getInterestOps() {
        return this.interestOps;
    }

    public ByteBuffer getWriteBuffer() {
        return this.writeBuf;
    }

    /**
     * Close the session
     */
    public void close() {
        if (this.user != null) {
            // If user was playing then lose the game
            if (this.state.isPlaying()) {
                this.user.loseGame();
                WordleServer.getInstance().updateLeaderboard(this.user.getUsername(),
                        this.user.score());
            }

            // Close the user session
            this.user.getSession().isActive = false;
            this.user = null;
        }
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
     * Same as {@code sendMessage(code, ByteBuffer.allocate(0))}
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

        // Enter synchronized block to update the user session
        UserSession newSession = new UserSession();
        synchronized (user) {
            // Get the previous user session if any
            UserSession previousSession = user.getSession();
            if (previousSession != null) {
                // Cannot login more than once at the same time
                if (previousSession.isActive) {
                    this.logger.finer(
                            String.format("User `%s` already logged in. Rejected", username));
                    this.sendMessage(MessageStatus.ALREADY_LOGGED);
                    return;
                }

                // Restore the previous session. The other attributes can safely be ignored
                newSession.secretWord = previousSession.secretWord;
                newSession.gameId = previousSession.gameId;
            }

            // Update the session
            user.setSession(newSession);
        }

        // Now we have established the ownership over the User object
        // Update the state
        this.state.login();
        this.user = user;

        this.logger.finer(String.format("User `%s` logged in", username));

        // Prepare the success message
        this.sendMessage(MessageStatus.SUCCESS);
    }

    /**
     * Logout the user
     */
    private void logoutHandler() {
        this.logger.info(String.format("User `%s`: action Logout", this.user.getUsername()));

        // Clean the state
        this.state.logout();
        this.close();

        // Prepare the success message
        this.sendMessage(MessageStatus.SUCCESS);
    }

    /**
     * Start a new game.
     */
    private void startGameHandler() {
        this.logger.info(String.format("User `%s`: action playWORDLE", this.user.getUsername()));

        // Get the current word from server
        Pair<String, Long> p = WordleServer.getInstance().getCurrentWord();
        String secretWord = p.first;
        long gameId = p.second;
        UserSession session = this.user.getSession();

        // Check if player already played with that word
        if (gameId == session.gameId) {
            // Send the time to the next secret word
            this.sendMessage(MessageStatus.ALREADY_PLAYED,
                    WordleServer.getInstance().getNextSWTime());
            return;
        }

        // Update the session state
        this.state.play();
        session.secretWord = secretWord;
        session.gameId = gameId;
        session.triesLeft = WordleServer.WORD_TRIES;
        this.user.newGame();

        // Prepare the success message
        ByteBuffer msg = ByteBuffer.allocate(Integer.BYTES * 2);
        msg.put((byte) secretWord.length());
        msg.put((byte) session.triesLeft);
        msg.flip();
        this.sendMessage(MessageStatus.SUCCESS, msg);
    }

    /**
     * Handler for checking a new guess that the user made.
     * 
     * @param msg The message containing the guessed word
     */
    private void guessWordHandler(ByteBuffer msg) {
        UserSession session = this.user.getSession();

        // No leftover tries
        if (session.triesLeft == 0) {
            this.state.stopPlaying();

            // Send the time to the next secret word
            this.sendMessage(MessageStatus.NO_TRIES_LEFT,
                    WordleServer.getInstance().getNextSWTime());
            return;
        }

        // Read the guessed word
        String guessWord = StandardCharsets.UTF_8.decode(msg).toString();

        this.logger.info(String.format("User `%s` guessed word `%s` (secret `%s`)",
                this.user.getUsername(), guessWord, session.secretWord));

        // Invalid word
        if (!WordleServer.getInstance().isValidWord(guessWord)) {
            this.sendMessage(MessageStatus.INVALID_WORD, (byte) session.triesLeft);
            return;
        }

        // Spend a try
        session.triesLeft--;
        session.addHint(guessWord);

        // Client won
        if (session.secretWord.equals(guessWord)) {
            // Update state
            this.state.stopPlaying();
            this.user.winGame(WordleServer.WORD_TRIES - session.triesLeft);
            WordleServer.getInstance().updateLeaderboard(this.user.getUsername(),
                    this.user.score());

            // Send the secret word translation
            byte[] encTranslation = TranslationServer.getInstance().get(session.secretWord)
                    .getBytes(StandardCharsets.UTF_8);
            ByteBuffer sMsg = ByteBuffer.allocate(1 + encTranslation.length);
            sMsg.put((byte) session.triesLeft);
            sMsg.put(encTranslation);
            sMsg.flip();

            this.sendMessage(MessageStatus.GAME_WON, sMsg);
            return;
        }

        List<Integer> correct = session.getLastCorrectHint();
        List<Integer> partial = session.getLastPartialHint();

        // Forge message
        // Let's make the buffer bigger than necessary to avoid unnecessary calculations
        ByteBuffer sMsg = ByteBuffer.allocate(Constants.SOCKET_MSG_MAX_SIZE);
        sMsg.put((byte) session.triesLeft);
        sMsg.put((byte) correct.size());
        sMsg.put((byte) partial.size());
        for (int p : correct)
            sMsg.put((byte) p);
        for (int p : partial)
            sMsg.put((byte) p);

        if (session.triesLeft == 0) {
            // Update the state. Since there are no more tries left the game is lost
            this.state.stopPlaying();
            this.user.loseGame();
            WordleServer.getInstance().updateLeaderboard(this.user.getUsername(),
                    this.user.score());

            // No more tries left. Send the secret word alongside its translation
            byte[] encWord = session.secretWord.getBytes(StandardCharsets.UTF_8);
            sMsg.put((byte) encWord.length);
            sMsg.put(encWord);
            sMsg.put(TranslationServer.getInstance().get(session.secretWord)
                    .getBytes(StandardCharsets.UTF_8));
        }

        sMsg.flip();

        this.sendMessage(MessageStatus.SUCCESS, sMsg);
    }

    /**
     * Sends the user stats
     */
    private void statsHandler() {
        this.logger.info(String.format("User %s action STATS", this.user.getUsername()));

        // Prepare the message
        ByteBuffer msg = ByteBuffer.allocate(Integer.BYTES * 4 + Double.BYTES);
        msg.putInt(this.user.totGames);
        msg.putInt(this.user.wonGames);
        msg.putInt(this.user.currStreak);
        msg.putInt(this.user.bestStreak);
        msg.putDouble(this.user.score());
        // TODO put leaderboard position
        msg.flip();

        this.sendMessage(MessageStatus.SUCCESS, msg);
    }

    /**
     * Sends the top of the leaderboard
     */
    private void topLeaderboardHandler() {
        this.logger.info(String.format("User %s action TOP_LEADERBOARD", this.user.getUsername()));

        // Get the top leaderboard
        List<Pair<String, Double>> leaderboard = WordleServer.getInstance().getTopLeaderboard();

        // Prepare the message
        ByteBuffer msg = ByteBuffer.allocate(Constants.SOCKET_MSG_MAX_SIZE);
        msg.putInt(leaderboard.size());
        for (Pair<String, Double> p : leaderboard) {
            ByteBuffer enc = StandardCharsets.UTF_8.encode(p.first);
            msg.putInt(enc.limit());
            msg.put(enc);
            msg.putDouble(p.second);
        }
        msg.flip();

        this.sendMessage(MessageStatus.SUCCESS, msg);
    }

    // TODO add pagination
    /**
     * Sends the full leaderboard
     */
    private void fullLeaderboardHandler() {
        this.logger.info(String.format("User %s action FULL_LEADERBOARD", this.user.getUsername()));

        // Get the full leaderboard
        List<Pair<String, Double>> leaderboard = WordleServer.getInstance().getFullLeaderboard();

        // Prepare the message
        ByteBuffer msg = ByteBuffer.allocate(Constants.SOCKET_MSG_MAX_SIZE);
        msg.putInt(leaderboard.size());
        for (Pair<String, Double> p : leaderboard) {
            ByteBuffer enc = StandardCharsets.UTF_8.encode(p.first);
            msg.putInt(enc.limit());
            msg.put(enc);
            msg.putDouble(p.second);
        }
        msg.flip();

        this.sendMessage(MessageStatus.SUCCESS, msg);
    }

    /**
     * Share the user's last game
     */
    private void shareHandler() {
        this.logger.info(String.format("User %s action SHARE", this.user.getUsername()));

        GameDescriptor lastGame = this.user.lastGame;
        if (lastGame == null) {
            this.sendMessage(MessageStatus.NO_GAME);
        } else {
            this.sendMessage(MessageStatus.SUCCESS);

            // Actually share
            WordleServer.getInstance().shareGame(lastGame, this.user.getUsername());
        }
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

                case STATS:
                    this.statsHandler();
                    break;

                case TOP_LEADERBOARD:
                    this.topLeaderboardHandler();
                    break;

                case FULL_LEADERBOARD:
                    this.fullLeaderboardHandler();
                    break;

                case SHARE:
                    this.shareHandler();
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

                case STATS:
                    this.statsHandler();
                    break;

                case TOP_LEADERBOARD:
                    this.topLeaderboardHandler();
                    break;

                case FULL_LEADERBOARD:
                    this.fullLeaderboardHandler();
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
