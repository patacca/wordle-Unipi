package edu.riccardomori.wordle.server;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import edu.riccardomori.wordle.protocol.Action;
import edu.riccardomori.wordle.protocol.ClientState;
import edu.riccardomori.wordle.protocol.MessageStatus;
import edu.riccardomori.wordle.server.WordleServer;

public class WordleServerCore {
    private int interestOps; // The interest set of operations as a bitmask
    private ClientState state = ClientState.ANONYMOUS;
    private Logger logger;

    private String username; // The username of the client who is running this session
    private ByteBuffer writeBuf; // The buffer holding the writable data

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
     */
    private void sendMessage(MessageStatus code) {
        this.writeBuf.clear();
        this.writeBuf.put(code.getValue());
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
            this.state = ClientState.LOGGED;
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

        this.state = ClientState.ANONYMOUS;
        this.username = null;

        // Prepare the success message
        this.sendMessage(MessageStatus.SUCCESS);
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

        if (this.state == ClientState.ANONYMOUS) {
            switch (Action.fromByte(buffer.get())) {
                case LOGIN:
                    this.loginHandler(buffer);
                    break;

                default:
                    this.logger.info("Not allowed to perform this action");
                    this.sendMessage(MessageStatus.ACTION_UNAUTHORIZED);
                    break;
            }
        } else if (this.state == ClientState.LOGGED) {
            switch (Action.fromByte(buffer.get())) {
                case LOGOUT:
                    this.logoutHandler();
                    break;

                default:
                    this.logger.info(String.format("User `%s` not allowed to perform this action",
                            this.username));
                    this.sendMessage(MessageStatus.ACTION_UNAUTHORIZED);
                    break;
            }
        } else {
            this.logger.info("Not allowed to perform this action");
            this.sendMessage(MessageStatus.ACTION_UNAUTHORIZED);
        }

        return this.interestOps;
    }
}
