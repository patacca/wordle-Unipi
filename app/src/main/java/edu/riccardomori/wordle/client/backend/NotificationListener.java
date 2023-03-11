package edu.riccardomori.wordle.client.backend;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import edu.riccardomori.wordle.client.backend.exceptions.IOError;
import edu.riccardomori.wordle.client.backend.exceptions.IfaceExcpetion;
import edu.riccardomori.wordle.protocol.Constants;

public class NotificationListener {
    private String address;
    private int port;
    private NetworkInterface iface;
    private MulticastSocket socket;
    private volatile Map<String, Map<Long, GameShared>> gamesShared;
    private Thread daemonListener;

    public NotificationListener(String address, int port) {
        this.address = address;
        this.port = port;
    }

    private void initSocket() throws IOError, IfaceExcpetion {
        try {
            // Get one valid interface for multicast
            // If more than one is found, choose one of them non-deterministically
            this.iface = NetworkInterface.networkInterfaces().filter(iface -> {
                try {
                    return iface.isUp() && iface.supportsMulticast() && !iface.isLoopback()
                            && !iface.getName().equals("");
                } catch (SocketException e) {
                    return false;
                }
            }).findAny().get();

            // Create socket and join multicast group
            this.socket = new MulticastSocket(this.port);
            this.socket.joinGroup(new InetSocketAddress(this.address, this.port), this.iface);
        } catch (NoSuchElementException | SocketException e) {
            throw new IfaceExcpetion();
        } catch (IOException e) {
            throw new IOError();
        }
    }

    public void start() throws IOError, IfaceExcpetion {
        this.initSocket();

        // Clean all the previous notifications
        this.gamesShared = new HashMap<String, Map<Long, GameShared>>();

        // Start a background thread that handles the multicast socket packets
        this.daemonListener = new Thread(() -> {
            byte[] buffer = new byte[Constants.UDP_MSG_MAX_SIZE];

            // Listen passively until an interrupt happens
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                try {
                    this.socket.receive(packet);
                } catch (IOException e) {
                    break; // Might have been interrupted. Stop listening
                }

                // Parse the gameId and username
                ByteBuffer msg =
                        ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength());
                int usernameSize = msg.getInt();
                byte[] usernameRaw = new byte[usernameSize];
                msg.get(usernameRaw);
                String username = new String(usernameRaw, StandardCharsets.UTF_8);
                long gameId = msg.getLong();

                // If this message has already been received then ignore it
                synchronized (this.gamesShared) {
                    if (this.gamesShared.containsKey(username)
                            && this.gamesShared.get(username).containsKey(gameId))
                        continue;
                }

                // Parse the rest of the message
                int tries = msg.get();
                int maxTries = msg.get();
                int wordLen = msg.get();
                int hintsN = msg.get();
                int[][] correct = new int[hintsN][];
                int[][] partial = new int[hintsN][];
                for (int k = 0; k < hintsN; ++k) {
                    int correctSize = msg.get();
                    int partialSize = msg.get();
                    correct[k] = new int[correctSize];
                    partial[k] = new int[partialSize];
                    for (int j = 0; j < correctSize; ++j)
                        correct[k][j] = msg.get();
                    for (int j = 0; j < partialSize; ++j)
                        partial[k][j] = msg.get();
                }

                // Store it for later
                GameShared game = new GameShared(tries, maxTries, wordLen, correct, partial);
                synchronized (this.gamesShared) {
                    this.gamesShared.computeIfAbsent(username,
                            key -> new HashMap<Long, GameShared>());
                    this.gamesShared.get(username).computeIfAbsent(gameId, key -> game);
                }
            }
        });

        this.daemonListener.start();
    }

    public void stop() {
        this.daemonListener.interrupt();
    }

    public Map<String, Map<Long, GameShared>> getAllData() {
        Map<String, Map<Long, GameShared>> games;
        synchronized (this.gamesShared) {
            games = this.gamesShared;
            this.gamesShared = new HashMap<String, Map<Long, GameShared>>();
        }
        return games;
    }
}
