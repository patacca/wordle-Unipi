package edu.riccardomori.wordle.rmi.exceptions;

public class UsernameIllegalException extends RuntimeException {
    public UsernameIllegalException() {
        super();
    }

    public UsernameIllegalException(String s) {
        super(s);
    }
}
