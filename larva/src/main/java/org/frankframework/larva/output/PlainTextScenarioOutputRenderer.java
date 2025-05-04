/*
   Copyright 2025 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.frankframework.larva.output;

import org.frankframework.larva.LarvaLogLevel;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.TestRunStatus;

public class PlainTextScenarioOutputRenderer implements TestExecutionObserver {
	private final LarvaWriter out;

	public PlainTextScenarioOutputRenderer(LarvaWriter out) {
		this.out = out;
	}

	@Override
	public void startTestSuiteExecution(TestRunStatus testRunStatus) {
		// No-op
	}

	@Override
	public void endTestSuiteExecution(TestRunStatus testRunStatus) {
		// No-op
	}

	@Override
	public void executionStatistics(TestRunStatus testRunStatus, long executionTime) {
		String scenariosPassedMessage = testRunStatus.buildScenariosPassedMessage(executionTime);
		String scenariosFailedMessage = testRunStatus.buildScenariosFailedMessage(executionTime);
		String scenariosAutosavedMessage = testRunStatus.buildScenariosAutoSavedMessage(executionTime);
		String scenariosTotalMessage = testRunStatus.buildScenariosTotalMessage(executionTime);

		if (scenariosPassedMessage != null) {
			out.writeOutputMessage(LarvaLogLevel.TOTALS, scenariosPassedMessage);
		}
		if (scenariosFailedMessage != null) {
			out.writeOutputMessage(LarvaLogLevel.TOTALS, scenariosFailedMessage);
		}
		if (scenariosAutosavedMessage != null) {
			out.writeOutputMessage(LarvaLogLevel.TOTALS, scenariosAutosavedMessage);
		}
		if (scenariosTotalMessage != null) {
			out.writeOutputMessage(LarvaLogLevel.TOTALS, scenariosTotalMessage);
		}
	}

	@Override
	public void startScenario(TestRunStatus testRunStatus, String scenarioName) {
		// No-op
	}

	@Override
	public void finishScenario(TestRunStatus testRunStatus, String scenarioName, int scenarioResult, String scenarioResultMessage) {
		LarvaLogLevel logLevel = scenarioResult == LarvaTool.RESULT_ERROR ? LarvaLogLevel.SCENARIO_FAILED : LarvaLogLevel.SCENARIO_PASSED_FAILED;
		out.writeOutputMessage(logLevel, scenarioResultMessage);
	}

	@Override
	public void startStep(TestRunStatus testRunStatus, String stepName) {
		// No-op
	}

	@Override
	public void finishStep(TestRunStatus testRunStatus, String stepName, int stepResult, String stepResultMessage) {
		out.writeOutputMessage(LarvaLogLevel.STEP_PASSED_FAILED, stepResultMessage);
	}

	@Override
	public void stepMessage(String stepName, String description, String stepMessage) {
		out.writeOutputMessage(LarvaLogLevel.PIPELINE_MESSAGES, "Step " + stepName + ": " + description + "\n" + stepMessage);
	}

	@Override
	public void stepMessageSuccess(String stepName, String description, String stepResultMessage, String stepResultMessagePreparedForDiff) {
		out.writeOutputMessage(LarvaLogLevel.PIPELINE_MESSAGES, "Step " + stepName + ": " + description + "\n" + stepResultMessage);
		out.writeOutputMessage(LarvaLogLevel.PIPELINE_MESSAGES_PREPARED_FOR_DIFF, stepResultMessagePreparedForDiff);
	}

	@Override
	public void stepMessageFailed(String stepName, String description, String stepSaveFileName, String stepExpectedResultMessage, String stepExpectedResultMessagePreparedForDiff, String stepActualResultMessage, String stepActualResultMessagePreparedForDiff) {
		String template = """
				Step: %s %s
				Actual%s:
				%s
				----------
				Expected%s:
				%s
				----------
				""";
		out.writeOutputMessage(LarvaLogLevel.WRONG_PIPELINE_MESSAGES, template.formatted(stepName, description, "", stepActualResultMessage, "", stepExpectedResultMessage));
		out.writeOutputMessage(LarvaLogLevel.WRONG_PIPELINE_MESSAGES_PREPARED_FOR_DIFF, template.formatted(stepName, description, " (prepared for diff)", stepActualResultMessagePreparedForDiff, " (prepared for diff)", stepExpectedResultMessagePreparedForDiff));
	}

	@Override
	public void messageError(String description, String messageError) {
		out.writeOutputMessage(LarvaLogLevel.WRONG_PIPELINE_MESSAGES, description + "\n" + messageError);
	}
}
