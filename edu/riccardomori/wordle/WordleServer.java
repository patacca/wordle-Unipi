package edu.riccardomori.wordle;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.riccardomori.wordle.logging.ConsoleHandler;

/**
 * This is the main server class. It handles all the incoming connections with
 * non-blocking channels
 */
public class WordleServer {
    private int port; // The port of the server socket
    private Logger logger;

    public WordleServer(int port) {
        this.port = port;
        this.logger = Logger.getLogger("Wordle");

        // MOVE ALL OF THIS
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tb %1$td, %1$tY %1$tH:%1$tM:%1$tS %4$s: %5$s%6$s%n");
        this.logger.addHandler(new ConsoleHandler());
        this.logger.setUseParentHandlers(false);
        this.logger.setLevel(Level.ALL); // to be moved

        // Initialize the state
    }

    /**
     * Run the server.
     */
    public void run() {
        // Init server socket and listen on port `this.port`
        try (ServerSocketChannel socket = ServerSocketChannel.open(); Selector selector = Selector.open()) {
            socket.bind(new InetSocketAddress(this.port));
            socket.configureBlocking(false);
            this.logger.info(String.format("Listening on port %d", this.port));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
