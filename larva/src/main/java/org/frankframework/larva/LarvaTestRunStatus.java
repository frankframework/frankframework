package org.frankframework.larva;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;

// TODO: This has no use yet but I think it will be better than tracking these stats in the ScenarioRunner.
public class LarvaTestRunStatus {
	private final @Getter AtomicInteger scenariosFailed = new AtomicInteger();
	private final @Getter AtomicInteger scenariosPassed = new AtomicInteger();
	private final @Getter AtomicInteger scenariosAutosaved = new AtomicInteger();
	private @Getter int scenariosTotal;
}
