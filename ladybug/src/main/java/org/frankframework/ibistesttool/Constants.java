package org.frankframework.ibistesttool;

public class Constants {
	private Constants() {}

	// Both Ladybug and the FF! truncate messages as configured through
	// maxMessageLength. In this class we keep one character more such that
	// Ladybug can detect when a message is being truncated. This way,
	// Ladybug can show a message in its GUI that a report message has been
	// truncated.
	static final int MAX_MESSAGE_LENGTH_ADJUSTMENT = 1;
}
