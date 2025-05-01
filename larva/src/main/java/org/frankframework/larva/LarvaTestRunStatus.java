package org.frankframework.larva;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;

public class LarvaTestRunStatus {
	private @Getter @Setter int messageCounter = 0;
	private @Getter @Setter int scenarioCounter = 1;
	private final @Getter AtomicInteger scenariosFailed = new AtomicInteger();
	private final @Getter AtomicInteger scenariosPassed = new AtomicInteger();
	private final @Getter AtomicInteger scenariosAutosaved = new AtomicInteger();
	private @Getter int scenariosTotal;
}
