package org.frankframework.credentialprovider;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Simple log handler for tests to capture java util logging messages.
 */
public class TestLogHandler extends Handler {
	private final StringBuilder sb = new StringBuilder();

	@Override
	public void publish(LogRecord record) {
		if (record.getMessage() != null) sb.append(record.getMessage());
	}

	@Override
	public void flush() {
		// do nothing
	}

	@Override
	public void close() throws SecurityException {
		// do nothing
	}

	public boolean contains(String s) {
		return sb.toString().contains(s);
	}
}
