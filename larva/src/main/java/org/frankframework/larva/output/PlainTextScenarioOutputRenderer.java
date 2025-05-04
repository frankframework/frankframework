package org.frankframework.larva.output;

import org.frankframework.larva.TestRunStatus;

public class PlainTextScenarioOutputRenderer implements TestExecutionObserver {
	private final LarvaWriter out;

	public PlainTextScenarioOutputRenderer(LarvaWriter out) {
		this.out = out;
	}

	@Override
	public void startTestSuiteExecution(TestRunStatus testRunStatus) {

	}

	@Override
	public void endTestSuiteExecution(TestRunStatus testRunStatus) {

	}

	@Override
	public void executionStatistics(TestRunStatus testRunStatus, long executionTime) {

	}

	@Override
	public void startScenario(TestRunStatus testRunStatus, String scenarioName) {

	}

	@Override
	public void finishScenario(TestRunStatus testRunStatus, String scenarioName, int scenarioResult, String scenarioResultMessage) {

	}

	@Override
	public void startStep(TestRunStatus testRunStatus, String stepName) {

	}

	@Override
	public void finishStep(TestRunStatus testRunStatus, String stepName, int stepResult, String stepResultMessage) {

	}

	@Override
	public void stepMessage(String stepName, String description, String stepMessage) {

	}

	@Override
	public void stepMessageSuccess(String stepName, String description, String stepResultMessage, String stepResultMessagePreparedForDiff) {

	}

	@Override
	public void stepMessageFailed(String stepName, String description, String stepSaveFileName, String stepExpectedResultMessage, String stepExpectedResultMessagePreparedForDiff, String stepActualResultMessage, String stepActualResultMessagePreparedForDiff) {

	}

	@Override
	public void messageError(String description, String messageError) {

	}
}
