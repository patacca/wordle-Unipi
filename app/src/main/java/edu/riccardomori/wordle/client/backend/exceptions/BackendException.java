package edu.riccardomori.wordle.client.backend.exceptions;

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
