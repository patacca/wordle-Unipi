package edu.riccardomori.wordle.client.backend.exceptions;

public class UserTakenException extends Exception {
    public UserTakenException() {
        super();
    }

    public UserTakenException(String s) {
        super(s);
    }
}
