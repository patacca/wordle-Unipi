package edu.riccardomori.wordle.client.backend.exceptions;

public class InvalidWordException extends Exception {
    public InvalidWordException() {
        super();
    }

    public InvalidWordException(String s) {
        super(s);
    }
}
