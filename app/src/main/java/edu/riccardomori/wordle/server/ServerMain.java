package edu.riccardomori.wordle.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.riccardomori.wordle.server.logging.ConsoleHandler;

/**
 * This is the entry point for the server
 */
public class ServerMain {
    private static final String configFile = "ServerMain.properties";

    // Static attributes that are read from the configuration file
    private static int serverPort;
    private static int rmiPort;
    private static int verbosity;
    private static int swRate;
    private static String wordsDb;
    private static String multicastAddress;
    private static int multicastPort;

    public static void main(String args[]) {
        // Load the configuration
        try {
            ServerMain.loadConfig();
        } catch (Exception e) {
            System.err.format("Error while reading the configuration file '%s'\n",
                    ServerMain.configFile);
            e.printStackTrace();
            System.exit(1);
        }

        // Configure logging
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tb %1$td, %1$tY %1$tH:%1$tM:%1$tS %4$s: %5$s%6$s%n");
        Logger logger = Logger.getLogger("Wordle");
        Handler handler = new ConsoleHandler();
        switch (ServerMain.verbosity) {
            case 0:
                handler.setLevel(Level.SEVERE);
                logger.setLevel(Level.SEVERE);
                break;
            case 1:
                handler.setLevel(Level.WARNING);
                logger.setLevel(Level.WARNING);
                break;
            case 2:
                handler.setLevel(Level.INFO);
                logger.setLevel(Level.INFO);
                break;
            case 3:
                handler.setLevel(Level.FINE);
                logger.setLevel(Level.FINE);
                break;
            case 4:
                handler.setLevel(Level.FINER);
                logger.setLevel(Level.FINER);
                break;
            case 5:
                handler.setLevel(Level.FINEST);
                logger.setLevel(Level.FINEST);
                break;
            default:
                handler.setLevel(Level.SEVERE);
                logger.setLevel(Level.SEVERE);
                break;
        }
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);

        // Initialize Server
        WordleServer server = WordleServer.getInstance();
        server.configure(ServerMain.multicastAddress, ServerMain.multicastPort,
                ServerMain.serverPort, ServerMain.rmiPort, ServerMain.swRate, ServerMain.wordsDb);

        // Run the server
        server.run();
    }

    /**
     * Read the configuration file
     * 
     * @throws FileNotFoundException if the configuration file is not found
     * @throws IOException if there is an error while reading the file
     */
    private static void loadConfig() throws FileNotFoundException, IOException {
        try (InputStream input = new FileInputStream(ServerMain.configFile)) {
            Properties prop = new Properties();
            prop.load(input);
            ServerMain.serverPort = Integer.parseInt(prop.getProperty("server_port"));
            ServerMain.rmiPort = Integer.parseInt(prop.getProperty("rmi_port"));
            ServerMain.verbosity = Integer.parseInt(prop.getProperty("verbose"));
            ServerMain.swRate = Integer.parseInt(prop.getProperty("secret_word_rate"));
            ServerMain.wordsDb = prop.getProperty("words_db");
            ServerMain.multicastAddress = prop.getProperty("multicast_address");
            ServerMain.multicastPort = Integer.parseInt(prop.getProperty("multicast_port"));
        }
    }
}
