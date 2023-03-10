package edu.riccardomori.wordle.protocol;

public enum MessageStatus {
    // @formatter:off
    SUCCESS((byte) 1),
    INVALID_USER((byte) 2),
    ACTION_UNAUTHORIZED((byte) 3),
    ALREADY_PLAYED((byte) 4),
    NO_TRIES_LEFT((byte) 5),
    INVALID_WORD((byte) 6),
    ALREADY_LOGGED((byte) 7),
    GAME_WON((byte) 8),
    GENERIC_ERROR((byte) 0xff);
    // @formatter:on

    private final byte value;

    private MessageStatus(byte value) {
        this.value = value;
    }

    public static MessageStatus fromByte(byte value) {
        switch (value) {
            case 1:
                return SUCCESS;
            case 2:
                return INVALID_USER;
            case 3:
                return ACTION_UNAUTHORIZED;
            case 4:
                return ALREADY_PLAYED;
            case 5:
                return NO_TRIES_LEFT;
            case 6:
                return INVALID_WORD;
            case 7:
                return ALREADY_LOGGED;
            case 8:
                return GAME_WON;
            default:
                return GENERIC_ERROR;
        }
    }

    public byte getValue() {
        return this.value;
    }
}
