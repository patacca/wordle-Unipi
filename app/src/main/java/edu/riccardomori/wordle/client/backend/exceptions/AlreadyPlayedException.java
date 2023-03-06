package edu.riccardomori.wordle.client.backend.exceptions;

public class AlreadyPlayedException extends BackendException {
    private Long result;

    public AlreadyPlayedException(Long result) {
        super();
        this.result = result;
    }

    public Long getResult() {
        return this.result;
    }
}
