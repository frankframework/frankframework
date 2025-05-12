package org.frankframework.larva;

import java.time.Instant;

import lombok.Getter;

public class LarvaMessage {
	private final @Getter LarvaLogLevel logLevel;
	private final @Getter String message;
	private final @Getter Instant timestamp = Instant.now();

	public LarvaMessage(LarvaLogLevel logLevel, String message) {
		this.logLevel = logLevel;
		this.message = message;
	}
}
