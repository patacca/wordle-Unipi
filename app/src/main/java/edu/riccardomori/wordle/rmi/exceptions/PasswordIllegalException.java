package edu.riccardomori.wordle.rmi.exceptions;

public class PasswordIllegalException extends RuntimeException {
    public PasswordIllegalException(String s) {
        super(s);
    }
}
