package edu.riccardomori.wordle.server.logging;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * A modified StreamHandler that closely resembles a java.util.logging.ConsoleHandler, the
 * difference being that this handler publishes records to System.out
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
     * Override StreamHandler.close to do a flush and to avoid closing the output stream.
     */
    @Override
    public void close() {
        flush();
    }
}
