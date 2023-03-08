package edu.riccardomori.wordle.client.backend;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import edu.riccardomori.wordle.client.backend.exceptions.AlreadyLoggedException;
import edu.riccardomori.wordle.client.backend.exceptions.AlreadyPlayedException;
import edu.riccardomori.wordle.client.backend.exceptions.GenericError;
import edu.riccardomori.wordle.client.backend.exceptions.IOError;
import edu.riccardomori.wordle.client.backend.exceptions.InvalidUserException;
import edu.riccardomori.wordle.client.backend.exceptions.InvalidWordException;
import edu.riccardomori.wordle.client.backend.exceptions.ServerError;
import edu.riccardomori.wordle.client.backend.exceptions.UnknownHostException;
import edu.riccardomori.wordle.client.backend.exceptions.UserTakenException;
import edu.riccardomori.wordle.protocol.Action;
import edu.riccardomori.wordle.protocol.MessageStatus;
import edu.riccardomori.wordle.rmi.RMIConstants;
import edu.riccardomori.wordle.rmi.RMIStatus;
import edu.riccardomori.wordle.rmi.serverRMI;
import edu.riccardomori.wordle.utils.Pair;

public class ClientBackend {
    // Constants
    public static final int SOCKET_MSG_MAX_SIZE = 1024; // Maximum size for each message

    private final int socketTimeout = 10000; // Timeout for reading on the socket
    private String serverHost; // The server host
    private int serverPort; // The port of the server socket
    private int rmiPort; // The port of the RMI server

    private Socket socket; // The socket for communicating with the server

    /**
     * Simple utility class that holds a message status code and the optional message
     */
    private static class Message {
        public MessageStatus status;
        public ByteBuffer message;

        public Message() {}

        public Message(MessageStatus status) {
            this.status = status;
        }

        public Message(MessageStatus status, ByteBuffer message) {
            this.status = status;
            this.message = message;
        }
    }

    public ClientBackend(String host, int serverPort, int rmiPort) {
        this.serverHost = host;
        this.serverPort = serverPort;
        this.rmiPort = rmiPort;
    }

    private MessageStatus socketGetStatus() throws IOException {
        DataInputStream input =
                new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        int size = input.readInt();
        byte[] message = input.readNBytes(size);
        return MessageStatus.fromByte(message[0]);
    }

    private Message socketGetMessage() throws IOException {
        DataInputStream input =
                new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        int fullSize = input.readInt();

        // Read the status code (1 byte)
        Message ret = new Message(MessageStatus.fromByte((byte) input.read()));

        // Read the rest of the message
        ret.message = ByteBuffer.wrap(input.readNBytes(fullSize - 1));

        return ret;
    }

    /**
     * Sends a message to the server. The message is encapsulated in the "communication protocol"
     * packet ([SIZE] [MESSAGE]). It initialize the socket if it has not already.
     * 
     * @param data The data to be sent
     * @throws IOException
     * @throws java.net.UnknownHostException
     */
    private void socketWrite(ByteBuffer data) throws IOException, java.net.UnknownHostException {
        // Init the socket
        if (this.socket == null) {
            this.socket = new Socket(this.serverHost, this.serverPort);
            this.socket.setSoTimeout(this.socketTimeout);
        }

        // Write data as [SIZE] [MESSAGE]
        OutputStream out = new BufferedOutputStream(this.socket.getOutputStream());
        out.write(ByteBuffer.allocate(Integer.BYTES).putInt(data.limit()).array());
        out.write(data.array(), 0, data.limit());
        out.flush();
    }

    /**
     * Register the pair (username, password) to the server.
     * 
     * @param username
     * @param password
     * @throws UserTakenException The username was already taken
     * @throws GenericError
     * @throws ServerError Internal server error
     */
    public void register(String username, String password)
            throws UserTakenException, GenericError, ServerError {
        try {
            // Call RMI
            Registry registry = LocateRegistry.getRegistry(this.serverHost, this.rmiPort);
            serverRMI service = (serverRMI) registry.lookup(RMIConstants.RMI_REGISTER);
            RMIStatus result = service.register(username, password);

            if (result == RMIStatus.SUCCESS)
                return;
            else if (result == RMIStatus.USER_TAKEN)
                throw new UserTakenException();
            else
                throw new GenericError();
        } catch (NotBoundException | RemoteException e) {
            throw new ServerError();
        }
    }

    /**
     * Sends the login message to the server and fetches the response.
     * 
     * @param username
     * @param password
     * @throws InvalidUserException The pair (username, password) in invalid
     * @throws UnknownHostException Hostname unknown
     * @throws GenericError
     * @throws IOError
     * @throws AlreadyLoggedException
     */
    public void login(String username, String password) throws InvalidUserException,
            UnknownHostException, GenericError, IOError, AlreadyLoggedException {
        // Prepare the login message
        ByteBuffer data = ByteBuffer.allocate(ClientBackend.SOCKET_MSG_MAX_SIZE);
        data.put(Action.LOGIN.getValue());
        data.put((byte) username.length());
        data.put((byte) password.length());
        data.put(username.getBytes(StandardCharsets.US_ASCII));
        data.put(password.getBytes(StandardCharsets.US_ASCII));
        data.flip();

        try {
            this.socketWrite(data);

            // Wait for the response
            MessageStatus status = this.socketGetStatus();

            if (status == MessageStatus.SUCCESS)
                return;
            else if (status == MessageStatus.INVALID_USER)
                throw new InvalidUserException();
            else if (status == MessageStatus.ALREADY_LOGGED)
                throw new AlreadyLoggedException();
            else
                throw new GenericError();
        } catch (java.net.UnknownHostException e) {
            throw new UnknownHostException();
        } catch (IOException e) {
            throw new IOError();
        }
    }

    /**
     * Sends the PLAY message to server to start a new game. It returns a game descriptor
     * 
     * @return The game descriptor containing the size of the secret word and the number of
     *         availables tries
     * @throws GenericError
     * @throws IOError
     * @throws AlreadyPlayedException
     */
    public GameDescriptor startGame() throws GenericError, IOError, AlreadyPlayedException {
        // Prepare the play message
        ByteBuffer data = ByteBuffer.allocate(1);
        data.put(Action.PLAY.getValue());
        data.flip();

        try {
            this.socketWrite(data);

            // Wait for the response
            Message message = this.socketGetMessage();

            // Success
            if (message.status == MessageStatus.SUCCESS) {
                // Parse the message
                int wordSize = message.message.get();
                int nTries = message.message.get();

                return new GameDescriptor(wordSize, nTries);
            } else if (message.status == MessageStatus.ALREADY_PLAYED) {
                // fetch the next date
                long nextGameTime = message.message.getLong();
                throw new AlreadyPlayedException(nextGameTime);
            } else {
                throw new GenericError();
            }
        } catch (IOException e) {
            throw new IOError();
        }
    }

    /**
     * Sends the logout command to the server
     * 
     * @throws GenericError
     * @throws IOError
     */
    public void logout() throws GenericError, IOError {
        // Prepare the logout message
        ByteBuffer data = ByteBuffer.allocate(1);
        data.put(Action.LOGOUT.getValue());
        data.flip();

        try {
            this.socketWrite(data);

            // Wait for the response
            MessageStatus status = this.socketGetStatus();

            // Success
            if (status == MessageStatus.SUCCESS)
                return;
            else
                throw new GenericError();
        } catch (IOException e) {
            throw new IOError();
        }
    }

    public GuessDescriptor sendWord(String word)
            throws InvalidWordException, GenericError, IOError {
        // Prepare the sendWord message
        byte[] encodedWord = word.getBytes(StandardCharsets.UTF_8);
        ByteBuffer data = ByteBuffer.allocate(1 + encodedWord.length);
        data.put(Action.SEND_WORD.getValue());
        data.put(encodedWord);
        data.flip();

        try {
            this.socketWrite(data);

            Message message = this.socketGetMessage();

            if (message.status == MessageStatus.SUCCESS) {
                // Parse the message
                int triesLeft = message.message.get();
                int correctSize = message.message.get();
                int partialSize = message.message.get();
                int[] correct = new int[correctSize];
                int[] partial = new int[partialSize];
                for (int k = 0; k < correctSize; ++k)
                    correct[k] = message.message.get();
                for (int k = 0; k < partialSize; ++k)
                    partial[k] = message.message.get();

                // No more tries. Read the secret word
                if (triesLeft == 0 && message.message.hasRemaining()) {
                    String secretWord = StandardCharsets.UTF_8.decode(message.message).toString();
                    return new GuessDescriptor(triesLeft, correct, partial, secretWord);
                } else {
                    return new GuessDescriptor(triesLeft, correct, partial);
                }

            } else if (message.status == MessageStatus.INVALID_WORD) {
                throw new InvalidWordException();
            } else
                throw new GenericError();
        } catch (IOException e) {
            throw new IOError();
        }
    }

    public UserStats getStats() throws GenericError, IOError {
        // Prepare the STATS message
        ByteBuffer data = ByteBuffer.allocate(1);
        data.put(Action.STATS.getValue());
        data.flip();

        try {
            this.socketWrite(data);

            // Wait for the response
            Message msg = this.socketGetMessage();

            if (msg.status == MessageStatus.SUCCESS) {
                // Parse message
                int totGames = msg.message.getInt();
                int wonGames = msg.message.getInt();
                int currStrak = msg.message.getInt();
                int bestStreak = msg.message.getInt();
                double score = msg.message.getDouble();

                return new UserStats(totGames, wonGames, currStrak, bestStreak, score);
            } else
                throw new GenericError();
        } catch (IOException e) {
            throw new IOError();
        }
    }

    public List<Pair<String, Double>> getLeaderboard() {
        return null;
    }
}
