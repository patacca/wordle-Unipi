package edu.riccardomori.wordle.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import edu.riccardomori.wordle.client.frontend.ClientFrontend;
import edu.riccardomori.wordle.client.frontend.CLI.ClientCLI;
import edu.riccardomori.wordle.client.frontend.GUI.ClientGUI;

public class ClientMain {
    private static final String configFile = "ClientMain.properties";

    private static String serverHost;
    private static int serverPort;
    private static int rmiPort;
    private static String multicastAddress;
    private static int multicastPort;

    public static void main(String args[]) {
        // Load the configuration
        try {
            ClientMain.loadConfig();
        } catch (Exception e) {
            System.err.format("Error while reading the configuration file '%s'\n",
                    ClientMain.configFile);
            System.exit(1);
        }

        // Load frontend
        // ClientFrontend client = new ClientGUI(ClientMain.serverHost, ClientMain.serverPort,
        //         ClientMain.rmiPort, ClientMain.multicastAddress, ClientMain.multicastPort);
        ClientFrontend client = new ClientCLI(ClientMain.serverHost, ClientMain.serverPort,
                ClientMain.rmiPort, ClientMain.multicastAddress, ClientMain.multicastPort);

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
        ClientMain.multicastAddress = prop.getProperty("multicast_address");
        ClientMain.multicastPort = Integer.parseInt(prop.getProperty("multicast_port"));
        input.close();
    }
}
