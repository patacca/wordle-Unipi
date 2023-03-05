package edu.riccardomori.wordle.client.backend.exceptions;

public class GenericError extends Exception {
    public GenericError() {
        super();
    }

    public GenericError(String s) {
        super(s);
    }
}
