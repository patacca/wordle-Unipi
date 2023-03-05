package edu.riccardomori.wordle.client.backend.exceptions;

public class UnknownHostException extends Exception {
    public UnknownHostException() {
        super();
    }

    public UnknownHostException(String s) {
        super(s);
    }
}
