package edu.riccardomori.wordle.client.frontend.GUI;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import edu.riccardomori.wordle.client.backend.ClientBackend;
import edu.riccardomori.wordle.client.backend.GameShared;
import edu.riccardomori.wordle.client.backend.NotificationListener;
import edu.riccardomori.wordle.client.backend.exceptions.GenericError;
import edu.riccardomori.wordle.client.backend.exceptions.IOError;
import edu.riccardomori.wordle.client.backend.exceptions.IfaceExcpetion;
import edu.riccardomori.wordle.client.frontend.ClientFrontend;
import edu.riccardomori.wordle.client.frontend.SessionState;
import edu.riccardomori.wordle.client.frontend.GUI.utils.Utils;
import edu.riccardomori.wordle.rmi.clientRMI;
import edu.riccardomori.wordle.utils.Pair;

public class ClientGUI implements ClientFrontend, ClientSession, clientRMI {
    private SessionState session = new SessionState(); // Describes the state of the current session

    private ClientBackend backend; // The backend implementation of the client
    private ViewManager viewManager;

    // Listen for the notifications shared by the server
    private NotificationListener notificationListener;
    // The top positions of the leaderboard
    private volatile List<Pair<String, Double>> topLeaderboard;

    public ClientGUI(String host, int serverPort, int rmiPort, String multicastAddress,
            int multicastPort) {

        // Create the main window
        this.viewManager = new ViewManager("Wordle");

        // Initialize the backend
        this.backend = new ClientBackend(host, serverPort, rmiPort, this);

        this.notificationListener = new NotificationListener(multicastAddress, multicastPort);
    }

    /**
     * Start listening for notifications
     */
    private void startNotificationListener() {
        try {
            this.notificationListener.start();
        } catch (IfaceExcpetion e) {
            Utils.errorPopup("Cannot find a valid interface for multicast notifications");
        } catch (IOError e) {
            Utils.errorPopup("Cannot create a multicast socket");
        }
    }

    @Override
    public void run() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ClientGUI.this.viewManager.run(ClientGUI.this, ClientGUI.this.backend);
            }
        });
    }

    @Override
    public void updateLeaderboard(List<Pair<String, Double>> leaderboard) throws RemoteException {
        this.topLeaderboard = leaderboard;
    }

    @Override
    public String getUsername() {
        return this.session.getUsername();
    }

    @Override
    public void login(String username) {
        // Subscribe to the leaderboard updates
        try {
            this.backend.subscribe();
        } catch (GenericError e) {
            System.err.println("Cannot subscribe to the leaderboard updates");
        }

        // Start the notification listener
        this.startNotificationListener();

        // Update the session state
        this.session.login(username);
    }

    @Override
    public void logout() {
        try {
            this.backend.logout();

            // Unsubscribe from the server notification callbacks
            try {
                this.backend.unsubscribe();
            } catch (Exception e) {
            }

            // Stop the notification listener
            this.notificationListener.stop();

            // Update the session state
            this.session.logout();
        } catch (GenericError e) {
            JOptionPane.showMessageDialog(null, "An error happened. Try again later.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOError e) {
            JOptionPane.showMessageDialog(null, "I/O error during server communication.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void startGame(int wordLen, int triesN) {
        this.session.startGame();
    }

    @Override
    public void stopGame() {
        this.session.stopGame();
    }

    @Override
    public boolean isPlaying() {
        return this.session.isPlaying();
    }

    @Override
    public List<Pair<String, Double>> getTopLeaderboard() {
        return this.topLeaderboard;
    }

    @Override
    public Map<String, Map<Long, GameShared>> getNotifications() {
        if (this.notificationListener == null)
            return null;
        return this.notificationListener.getAllData();
    }
}
