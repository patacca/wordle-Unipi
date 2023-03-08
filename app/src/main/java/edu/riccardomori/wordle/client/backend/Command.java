package edu.riccardomori.wordle.client.backend;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum Command {
    REGISTER(1), LOGIN(2), PLAY(3), SHOW_STATS(4), LOGOUT(5), EXIT(6), INVALID(-1);

    private final int value;
    private final static Map<Integer, Command> map = Arrays.stream(Command.values())
            .collect(Collectors.toMap(command -> command.value, command -> command));

    private Command(final int value) {
        this.value = value;
    }

    public static Command fromInt(int value) {
        Command ret = Command.map.get(value);
        if (ret == null)
            throw new IllegalArgumentException(
                    String.format("value %d does not idendify a Command", value));
        return ret;
    }
}
