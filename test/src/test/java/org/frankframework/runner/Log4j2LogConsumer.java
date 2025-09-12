package org.frankframework.runner;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testcontainers.containers.output.OutputFrame;

/**
 * Testcontainers log consumer that logs directly to Log4j2.
 */
public class Log4j2LogConsumer implements Consumer<OutputFrame> {
    private final Logger logger;
    private final String prefix;

    public Log4j2LogConsumer(String loggerName, String prefix) {
        this.logger = LogManager.getLogger(loggerName);
        this.prefix = (prefix != null && !prefix.isBlank()) ? prefix + " " : "";
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        if (outputFrame == null || outputFrame.getUtf8String() == null) {
            return;
        }

        String msg = prefix + outputFrame.getUtf8String().trim();

        switch (outputFrame.getType()) {
            case STDERR -> logger.error(msg);
            case STDOUT -> logger.info(msg);
            case END -> logger.debug(msg);
        }
    }
}
