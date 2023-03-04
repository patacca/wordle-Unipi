package edu.riccardomori.wordle.client;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum Command {
    REGISTER(1), LOGIN(2), LOGOUT(3), EXIT(4), INVALID(-1);

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
