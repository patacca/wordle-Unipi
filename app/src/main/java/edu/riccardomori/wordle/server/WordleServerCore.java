package edu.riccardomori.wordle.server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.logging.Logger;
import edu.riccardomori.wordle.Action;
import edu.riccardomori.wordle.ClientState;
import edu.riccardomori.wordle.server.WordleServer;

public class WordleServerCore {
    private int interestOps; // The interest set of operations as a bitmask
    private ClientState state = ClientState.ANONYMOUS;
    private Logger logger;

    public WordleServerCore() {
        this.interestOps = SelectionKey.OP_READ;

        this.logger = Logger.getLogger("Wordle");
    }

    public int getInterestOps() {
        return interestOps;
    }

    /**
     * Read the message provided in the buffer as data coming from the client. It then performs the
     * actions required considering the current state of the server. Note that the message **must**
     * always be complete. A partial message results in undefined behavior.
     * 
     * @param buffer The buffer containing the message coming from the client. It must be ready to
     *        be read
     * @return The interest set of operations
     */
    public int readMessage(ByteBuffer buffer) {
        if (this.state == ClientState.ANONYMOUS) {
            Action action = Action.fromByte(buffer.get());
            if (action == Action.UNKNOWN) {
                this.logger.info("Action Unknown");
            }

            if (action == Action.LOGIN) {
                this.logger.info("Action Login");
                String username = "user";
                String password = "pass";

                if (WordleServer.getInstance().checkLogin(username, password)) {
                    this.logger.finer(String.format("User `%s` logged in", username));
                    this.state = ClientState.LOGGED;
                } else {
                    this.logger
                            .finer(String.format("Authentication of user `%s` rejected", username));
                }
            }
        }
        return this.interestOps;
    }
}
