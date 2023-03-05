package edu.riccardomori.wordle.client.backend.exceptions;

public class ServerError extends Exception {
    public ServerError() {
        super();
    }

    public ServerError(String s) {
        super(s);
    }
}
