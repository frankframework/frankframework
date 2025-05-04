package org.frankframework.larva.output;

import org.frankframework.larva.TestRunStatus;

public interface TestExecutionObserver {

	void startTestSuiteExecution(TestRunStatus testRunStatus);
	void endTestSuiteExecution(TestRunStatus testRunStatus);
	void executionStatistics(TestRunStatus testRunStatus, long executionTime);

	void startScenario(TestRunStatus testRunStatus, String scenarioName);
	void finishScenario(TestRunStatus testRunStatus, String scenarioName, int scenarioResult, String scenarioResultMessage);

	void startStep(TestRunStatus testRunStatus, String stepName);
	void finishStep(TestRunStatus testRunStatus, String stepName, int stepResult, String stepResultMessage);
	void stepMessage(String stepName, String description, String stepMessage);
	void stepMessageSuccess(String stepName, String description, String stepResultMessage, String stepResultMessagePreparedForDiff);
	void stepMessageFailed(String stepName, String description, String stepSaveFileName, String stepExpectedResultMessage, String stepExpectedResultMessagePreparedForDiff, String stepActualResultMessage, String stepActualResultMessagePreparedForDiff);
	void messageError(String description, String messageError);
}
