package org.frankframework.larva.output;

import jakarta.annotation.Nullable;

public interface TestExecutionObserver {

	void startTestSuiteExecution();
	void endTestSuiteExecution();
	void executionStatistics(@Nullable String scenariosTotalMessage, @Nullable String scenariosPassedMessage, @Nullable String scenariosFailedMessage, @Nullable String scenariosAutosavedMessage, int scenariosTotal, int scenariosPassed, int scenariosFailed, int scenariosAutosaved);

	void startScenario(String scenarioName);
	void finishScenario(String scenarioName, int scenarioResult, String scenarioResultMessage);

	void startStep(String stepName);
	void finishStep(String stepName, int stepResult, String stepResultMessage);
	void stepMessageSuccess(String stepName, String description, String stepResultMessage, String stepResultMessagePreparedForDiff);
	void stepMessageFailed(String stepName, String description, String stepExpectedResultMessage, String stepExpectedResultMessagePreparedForDiff, String stepActualResultMessage, String stepActualResultMessagePreparedForDiff);
}
