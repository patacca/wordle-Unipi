package edu.riccardomori.wordle.client.backend.exceptions;

public class IOError extends Exception {
    public IOError() {
        super();
    }

    public IOError(String s) {
        super(s);
    }
}
