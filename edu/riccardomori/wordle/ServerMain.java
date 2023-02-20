package edu.riccardomori.wordle;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This is the entry point for the server
 */
public class ServerMain {
    private static final String configFile = "ServerMain.properties";

    private static int port;

    public static void main(String args[]) {
        // Load the configuration
        try {
            ServerMain.loadConfig();
        } catch (Exception e) {
            System.err.format("Error while reading the configuration file '%s'\n", ServerMain.configFile);
            System.exit(1);
        }

        // Initialize Server
        WordleServer server = new WordleServer(ServerMain.port);

        // Run the server
        server.run();
    }

    /**
     * Read the configuration file
     * 
     * @throws FileNotFoundException if the configuration file is not found
     * @throws IOException           if there is an error while reading the file
     */
    private static void loadConfig() throws FileNotFoundException, IOException {
        InputStream input = new FileInputStream(ServerMain.configFile);
        Properties prop = new Properties();
        prop.load(input);
        ServerMain.port = Integer.parseInt(prop.getProperty("port"));
        // swRefreshRate = Integer.parseInt(prop.getProperty("swRefreshRate"));
        input.close();
    }
}
