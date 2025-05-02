package org.frankframework.larva.output;

import jakarta.annotation.Nullable;

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
	public void executionStatistics(
			@Nullable String scenariosTotalMessage,
			@Nullable String scenariosPassedMessage,
			@Nullable String scenariosFailedMessage,
			@Nullable String scenariosAutosavedMessage, int scenariosTotal, int scenariosPassed, int scenariosFailed, int scenariosAutosaved) {

	}

	@Override
	public void startScenario(String scenarioName) {

	}

	@Override
	public void finishScenario(String scenarioName, int scenarioResult, String scenarioResultMessage) {

	}

	@Override
	public void startStep(String stepName) {

	}

	@Override
	public void finishStep(String stepName, int stepResult, String stepResultMessage) {

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
