package edu.riccardomori.wordle.protocol;

public enum Action {
    LOGIN((byte) 0), LOGOUT((byte) 1), PLAY((byte) 2), UNKNOWN((byte) 0xff);

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
            case 2:
                return PLAY;
            default:
                return UNKNOWN;
        }
    }

    public byte getValue() {
        return this.value;
    }
}
