package edu.riccardomori.wordle.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientMain {
    private static final String configFile = "ClientMain.properties";

    private static String serverHost;
    private static int serverPort;
    private static int rmiPort;

    public static void main(String args[]) {
        // Load the configuration
        try {
            ClientMain.loadConfig();
        } catch (Exception e) {
            System.err.format("Error while reading the configuration file '%s'\n",
                    ClientMain.configFile);
            System.exit(1);
        }

        ClientCLI client = new ClientCLI(ClientMain.serverHost, ClientMain.serverPort, ClientMain.rmiPort);

        client.run();
    }

    /**
     * Read the configuration file
     * 
     * @throws FileNotFoundException if the configuration file is not found
     * @throws IOException if there is an error while reading the file
     */
    private static void loadConfig() throws FileNotFoundException, IOException {
        InputStream input = new FileInputStream(ClientMain.configFile);
        Properties prop = new Properties();
        prop.load(input);
        ClientMain.serverPort = Integer.parseInt(prop.getProperty("server_port"));
        ClientMain.rmiPort = Integer.parseInt(prop.getProperty("rmi_port"));
        ClientMain.serverHost = prop.getProperty("server_host");
        input.close();
    }
}
