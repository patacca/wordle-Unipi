package edu.riccardomori.wordle.protocol;

public enum Action {
    LOGIN((byte) 0), LOGOUT((byte) 1), UNKNOWN((byte) 0xff);

    private final byte value;

    private Action(byte value) {
        this.value = value;
    }

    public static Action fromByte(byte value) {
        switch (value) {
            case 0:
                return LOGIN;
            case 1:
                return LOGOUT;
            default:
                return UNKNOWN;
        }
    }

    public byte getValue() {
        return this.value;
    }
}
