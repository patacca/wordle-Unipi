package edu.riccardomori.wordle.rmi.exceptions;

public class PasswordIllegalException extends RuntimeException {
    public PasswordIllegalException() {
        super();
    }

    public PasswordIllegalException(String s) {
        super(s);
    }
}
