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
