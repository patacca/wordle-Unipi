package edu.riccardomori.wordle.client.backend.exceptions;

public class InvalidUserException extends Exception {
    public InvalidUserException() {
        super();
    }

    public InvalidUserException(String s) {
        super(s);
    }
}
