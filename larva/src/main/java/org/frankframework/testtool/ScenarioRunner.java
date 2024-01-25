/*
   Copyright 2014-2019 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.testtool;

import static org.frankframework.testtool.TestTool.LOG_LEVEL_ORDER;
import static org.frankframework.testtool.TestTool.RESULT_AUTOSAVED;
import static org.frankframework.testtool.TestTool.RESULT_ERROR;
import static org.frankframework.testtool.TestTool.RESULT_OK;
import static org.frankframework.testtool.TestTool.closeQueues;
import static org.frankframework.testtool.TestTool.debugMessage;
import static org.frankframework.testtool.TestTool.executeStep;
import static org.frankframework.testtool.TestTool.getSteps;
import static org.frankframework.testtool.TestTool.readProperties;
import static org.frankframework.testtool.TestTool.scenarioAutosavedMessage;
import static org.frankframework.testtool.TestTool.scenarioFailedMessage;
import static org.frankframework.testtool.TestTool.scenarioPassedMessage;
import static org.frankframework.testtool.TestTool.stepAutosavedMessage;
import static org.frankframework.testtool.TestTool.stepFailedMessage;
import static org.frankframework.testtool.TestTool.stepPassedMessage;
import static org.frankframework.testtool.TestTool.writeHtml;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.frankframework.configuration.IbisContext;
import org.frankframework.testtool.queues.Queue;
import org.frankframework.testtool.queues.QueueCreator;
import org.frankframework.util.AppConstants;

import lombok.Getter;

@Getter
public class ScenarioRunner {

	private int scenariosFailed;
	private int scenariosPassed;
	private int scenariosAutosaved;

	private static final String STEP_SYNCHRONIZER = "Step synchronizer";
	private static final AtomicLong correlationIdSuffixCounter = new AtomicLong(1);
	protected static final String TESTTOOL_CORRELATIONID = "Test Tool correlation id";

	public void runScenario(IbisContext ibisContext, int timeout, Writer out, boolean silent, List<File> scenarioFiles, String currentScenariosRootDirectory, Map<String, Object> writers, AppConstants appConstants, boolean evenStep, int waitBeforeCleanUp, String logLevel) {
		for (File file : scenarioFiles) {
			// increment suffix for each scenario
			String correlationId = TESTTOOL_CORRELATIONID + "(" + correlationIdSuffixCounter.getAndIncrement() + ")";
			int scenarioPassed = RESULT_ERROR;

			String scenarioDirectory = file.getParentFile().getAbsolutePath() + File.separator;
			String longName = file.getAbsolutePath();
			String shortName = longName.substring(currentScenariosRootDirectory.length() - 1, longName.length() - ".properties".length());

			if (writers != null && (LOG_LEVEL_ORDER.indexOf("[" + writers.get("loglevel") + "]") < LOG_LEVEL_ORDER.indexOf("[scenario passed/failed]"))) {
					writeHtml("<br/>", writers, false);
					writeHtml("<br/>", writers, false);
					writeHtml("<div class='scenario'>", writers, false);

			}
			debugMessage("Read property file " + file.getName(), writers);
			Properties properties = readProperties(appConstants, file, writers);
			List<String> steps;

			if (properties != null) {
				debugMessage("Read steps from property file", writers);
				steps = getSteps(properties, writers);
//				synchronized (STEP_SYNCHRONIZER) {
					debugMessage("Open queues", writers);
					Map<String, Queue> queues = QueueCreator.openQueues(scenarioDirectory, properties, ibisContext, writers, timeout, correlationId);
					if (queues != null) {
						debugMessage("Execute steps", writers);
						boolean allStepsPassed = true;
						boolean autoSaved = false;
						Iterator<String> iterator = steps.iterator();
						while (allStepsPassed && iterator.hasNext()) {
							if (evenStep) {
								writeHtml("<div class='even'>", writers, false);
								evenStep = false;
							} else {
								writeHtml("<div class='odd'>", writers, false);
								evenStep = true;
							}
							String step = iterator.next();
							String stepDisplayName = shortName + " - " + step + " - " + properties.get(step);
							debugMessage("Execute step '" + stepDisplayName + "'", writers);
							int stepPassed = executeStep(step, properties, stepDisplayName, queues, writers, timeout, correlationId);
							if (stepPassed == RESULT_OK) {
								stepPassedMessage("Step '" + stepDisplayName + "' passed", writers);
							} else if (stepPassed == RESULT_AUTOSAVED) {
								stepAutosavedMessage("Step '" + stepDisplayName + "' passed after autosave", writers);
								autoSaved = true;
							} else {
								stepFailedMessage("Step '" + stepDisplayName + "' failed", writers);
								allStepsPassed = false;
							}
							writeHtml("</div>", writers, false);
						}
						if (allStepsPassed) {
							if (autoSaved) {
								scenarioPassed = RESULT_AUTOSAVED;
							} else {
								scenarioPassed = RESULT_OK;
							}
						}
						debugMessage("Wait " + waitBeforeCleanUp + " ms before clean up", writers);
						try {
							Thread.sleep(waitBeforeCleanUp);
						} catch (InterruptedException e) {
						}
						debugMessage("Close queues", writers);
						boolean remainingMessagesFound = closeQueues(queues, properties, writers, correlationId);
						if (remainingMessagesFound) {
							stepFailedMessage("Found one or more messages on queues or in database after scenario executed", writers);
							scenarioPassed = RESULT_ERROR;
						}
					}
//				}
			}

			if (scenarioPassed == RESULT_OK) {
				scenariosPassed++;
				scenarioPassedMessage("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' passed (" + scenariosFailed + "/" + scenariosPassed + "/" + scenarioFiles.size() + ")", writers);
				if (silent && LOG_LEVEL_ORDER.indexOf("[" + logLevel + "]") <= LOG_LEVEL_ORDER.indexOf("[scenario passed/failed]")) {
					try {
						out.write("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' passed");
					} catch (IOException e) {
					}
				}
			} else if (scenarioPassed == RESULT_AUTOSAVED) {
				scenariosAutosaved++;
				scenarioAutosavedMessage("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' passed after autosave", writers);
				if (silent) {
					try {
						out.write("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' passed after autosave");
					} catch (IOException e) {
					}
				}
			} else {
				scenariosFailed++;
				scenarioFailedMessage("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' failed (" + scenariosFailed + "/" + scenariosPassed + "/" + scenarioFiles.size() + ")", writers);
				if (silent) {
					try {
						out.write("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' failed");
					} catch (IOException e) {
					}
				}
			}
			writeHtml("</div>", writers, false);
		}
	}

}
