package edu.riccardomori.wordle.protocol;

// All the commands that the client is allowed to perform on the server
public enum Action {
    // @formatter:off
    LOGIN((byte) 0),
    LOGOUT((byte) 1),
    PLAY((byte) 2),
    SEND_WORD((byte) 3),
    STATS((byte) 4),
    TOP_LEADERBOARD((byte) 5),
    FULL_LEADERBOARD((byte) 6),
    SHARE((byte) 7),
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
            case 5:
                return TOP_LEADERBOARD;
            case 6:
                return FULL_LEADERBOARD;
            case 7:
                return SHARE;
            default:
                return UNKNOWN;
        }
    }

    public byte getValue() {
        return this.value;
    }
}
