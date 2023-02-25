package edu.riccardomori.wordle.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.riccardomori.wordle.logging.ConsoleHandler;

/**
 * This is the entry point for the server
 */
public class ServerMain {
    private static final String configFile = "ServerMain.properties";

    private static int serverPort;
    private static int rmiPort;
    private static int verbosity;

    public static void main(String args[]) {
        // Load the configuration
        try {
            ServerMain.loadConfig();
        } catch (Exception e) {
            System.err.format("Error while reading the configuration file '%s'\n",
                    ServerMain.configFile);
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
                break;
            case 1:
                handler.setLevel(Level.WARNING);
                break;
            case 2:
                handler.setLevel(Level.INFO);
                break;
            case 3:
                handler.setLevel(Level.FINE);
                break;
            case 4:
                handler.setLevel(Level.FINER);
                break;
            case 5:
                handler.setLevel(Level.FINEST);
                break;
            default:
                handler.setLevel(Level.SEVERE);
                break;
        }
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        // Initialize Server
        WordleServer server = WordleServer.getInstance();
        server.configure(ServerMain.serverPort, ServerMain.rmiPort);

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
        InputStream input = new FileInputStream(ServerMain.configFile);
        Properties prop = new Properties();
        prop.load(input);
        ServerMain.serverPort = Integer.parseInt(prop.getProperty("server_port"));
        ServerMain.rmiPort = Integer.parseInt(prop.getProperty("rmi_port"));
        verbosity = Integer.parseInt(prop.getProperty("verbose"));
        input.close();
    }
}
