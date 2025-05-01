package org.frankframework.larva.output;

public class PlainTextScenarioOutputRenderer implements TestExecutionObserver {
	private final LarvaWriter out;

	public PlainTextScenarioOutputRenderer(LarvaWriter out) {
		this.out = out;
	}

	@Override
	public void startTestSuiteExecution() {

	}

	@Override
	public void endTestSuiteExecution() {

	}

	@Override
	public void startScenario(String scenarioName) {

	}

	@Override
	public void succeedScenario() {

	}

	@Override
	public void failScenario() {

	}

	@Override
	public void stepSucceeded() {

	}

	@Override
	public void stepFailed() {

	}
}
