package edu.riccardomori.wordle;

public enum Action {
    LOGIN((byte) 0), UNKNOWN((byte) 0xff);

    private final byte value;

    private Action(byte value) {
        this.value = value;
    }

    public static Action fromByte(byte value) {
        switch (value) {
            case 0:
                return LOGIN;
            default:
                return UNKNOWN;
        }
    }
}
