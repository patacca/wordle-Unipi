package edu.riccardomori.wordle.logging;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * A modified ConsoleHandler that publishes records to System.out
 */
public class ConsoleHandler extends StreamHandler {
    public ConsoleHandler() {
        super(System.out, new SimpleFormatter());
    }

    /**
     * Publish a LogRecord and flushes the output stream.
     */
    @Override
    public void publish(LogRecord record) {
        super.publish(record);
        flush();
    }

    /**
     * Override StreamHandler.close to do a flush but not to close the output
     * stream.
     */
    @Override
    public void close() {
        flush();
    }
}