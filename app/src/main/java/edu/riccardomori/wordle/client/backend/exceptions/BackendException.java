package edu.riccardomori.wordle.client.backend.exceptions;

// Base exception used by the client backend. Every exception should extend this one
public class BackendException extends Exception {
    public BackendException(String s) {
        super(s);
    }

    public BackendException() {
        super();
    }

    public Object getResult() {
        return null;
    }
}
