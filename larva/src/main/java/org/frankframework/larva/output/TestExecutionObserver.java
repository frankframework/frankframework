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

import org.frankframework.larva.Scenario;
import org.frankframework.larva.TestRunStatus;

/**
 * Interface to allow an observer to act on events during the execution of Larva tests.
 * Typically, this will be rendering some form of output for a particular output medium -- such as HTML, or plaintext, or different.
 * <p>
 *     The current LarvaTool allows for only a single TestExecutionObserver per LarvaTool instance, which is passed to it as a constructor parameter.
 * </p>
 */
public interface TestExecutionObserver {

	void startTestSuiteExecution(TestRunStatus testRunStatus);
	void endTestSuiteExecution(TestRunStatus testRunStatus);
	void executionOverview(TestRunStatus testRunStatus, long executionTime);

	void startScenario(TestRunStatus testRunStatus, Scenario scenario);
	void finishScenario(TestRunStatus testRunStatus, Scenario scenario, int scenarioResult, String scenarioResultMessage);

	void startStep(TestRunStatus testRunStatus, Scenario scenario, String stepName);
	void finishStep(TestRunStatus testRunStatus, Scenario scenario, String stepName, int stepResult, String stepResultMessage);
	void stepMessage(Scenario scenario, String stepName, String description, String stepMessage);
	void stepMessageSuccess(Scenario scenario, String stepName, String description, String stepResultMessage, String stepResultMessagePreparedForDiff);
	void stepMessageFailed(Scenario scenario, String stepName, String description, String stepSaveFileName, String stepExpectedResultMessage, String stepExpectedResultMessagePreparedForDiff, String stepActualResultMessage, String stepActualResultMessagePreparedForDiff);
	void messageError(String description, String messageError);
}
