package edu.riccardomori.wordle.protocol;

public enum Action {
    // @formatter:off
    LOGIN((byte) 0),
    LOGOUT((byte) 1),
    PLAY((byte) 2),
    SEND_WORD((byte) 3),
    STATS((byte) 4),
    UNKNOWN((byte) 0xff);
    // @formatter:on

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
            case 3:
                return SEND_WORD;
            case 4:
                return STATS;
            default:
                return UNKNOWN;
        }
    }

    public byte getValue() {
        return this.value;
    }
}
