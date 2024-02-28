/*
   Copyright 2014-2019 Nationale-Nederlanden, 2024 WeAreFrank!

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

import static org.frankframework.testtool.LarvaTool.LOG_LEVEL_ORDER;
import static org.frankframework.testtool.LarvaTool.RESULT_AUTOSAVED;
import static org.frankframework.testtool.LarvaTool.RESULT_ERROR;
import static org.frankframework.testtool.LarvaTool.RESULT_OK;

import java.io.File;
import java.io.IOException;
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
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Getter
@Log4j2
public class ScenarioRunner {

	private int scenariosFailed;
	private int scenariosPassed;
	private int scenariosAutosaved;
	@Setter private LarvaTool larvaTool;

	private static final String STEP_SYNCHRONIZER = "Step synchronizer";
	private static final AtomicLong correlationIdSuffixCounter = new AtomicLong(1);
	private static final String TESTTOOL_CORRELATIONID = "Test Tool correlation id";

	public void runScenario(IbisContext ibisContext, TestConfig config, List<File> scenarioFiles, String currentScenariosRootDirectory, AppConstants appConstants, boolean evenStep, int waitBeforeCleanUp, String logLevel) {
		for (File file : scenarioFiles) {
				runOneFile(ibisContext, config, file, currentScenariosRootDirectory, appConstants, evenStep, waitBeforeCleanUp, logLevel, scenarioFiles.size());
		}
	}

	private void runOneFile(IbisContext ibisContext, TestConfig config, File file, String currentScenariosRootDirectory, AppConstants appConstants, boolean evenStep, int waitBeforeCleanUp, String logLevel, int scenarioFileSize) {
		// increment suffix for each scenario
		String correlationId = TESTTOOL_CORRELATIONID + "(" + correlationIdSuffixCounter.getAndIncrement() + ")";
		int scenarioPassed = RESULT_ERROR;

		String scenarioDirectory = file.getParentFile().getAbsolutePath() + File.separator;
		String longName = file.getAbsolutePath();
		String shortName = longName.substring(currentScenariosRootDirectory.length() - 1, longName.length() - ".properties".length());

		if (!config.isSilent() && (LOG_LEVEL_ORDER.indexOf("[" + config.getLogLevel() + "]") < LOG_LEVEL_ORDER.indexOf("[scenario passed/failed]"))) {
			larvaTool.writeHtml("<br/><br/>", false);
			larvaTool.writeHtml("<div class='scenario'>", false);
		}
		larvaTool.debugMessage("Read property file " + file.getName());
		Properties properties = larvaTool.readProperties(appConstants, file);
		List<String> steps;

		if (properties != null) {
			larvaTool.debugMessage("Read steps from property file");
			steps = larvaTool.getSteps(properties);
			larvaTool.debugMessage("Open queues");
			Map<String, Queue> queues = new QueueCreator(config, larvaTool).openQueues(scenarioDirectory, properties, ibisContext, correlationId);
			if (queues != null) {
				larvaTool.debugMessage("Execute steps");
				boolean allStepsPassed = true;
				boolean autoSaved = false;
				Iterator<String> iterator = steps.iterator();
				while (allStepsPassed && iterator.hasNext()) {
					if (evenStep) {
						larvaTool.writeHtml("<div class='even'>", false);
						evenStep = false;
					} else {
						larvaTool.writeHtml("<div class='odd'>", false);
						evenStep = true;
					}
					String step = iterator.next();
					String stepDisplayName = shortName + " - " + step + " - " + properties.get(step);
					larvaTool.debugMessage("Execute step '" + stepDisplayName + "'");
					int stepPassed = larvaTool.executeStep(step, properties, stepDisplayName, queues, correlationId);
					if (stepPassed == RESULT_OK) {
						larvaTool.stepPassedMessage("Step '" + stepDisplayName + "' passed");
					} else if (stepPassed == RESULT_AUTOSAVED) {
						larvaTool.stepAutosavedMessage("Step '" + stepDisplayName + "' passed after autosave");
						autoSaved = true;
					} else {
						larvaTool.stepFailedMessage("Step '" + stepDisplayName + "' failed");
						allStepsPassed = false;
					}
					larvaTool.writeHtml("</div>", false);
					config.flushWriters();
				}
				if (allStepsPassed) {
					if (autoSaved) {
						scenarioPassed = RESULT_AUTOSAVED;
					} else {
						scenarioPassed = RESULT_OK;
					}
				}
				larvaTool.debugMessage("Wait " + waitBeforeCleanUp + " ms before clean up");
				try {
					Thread.sleep(waitBeforeCleanUp);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				larvaTool.debugMessage("Close queues");
				boolean remainingMessagesFound = larvaTool.closeQueues(queues, properties, correlationId);
				if (remainingMessagesFound) {
					larvaTool.stepFailedMessage("Found one or more messages on queues or in database after scenario executed");
					scenarioPassed = RESULT_ERROR;
				}
			}
		}

		if (scenarioPassed == RESULT_OK) {
			scenariosPassed++;
			larvaTool.scenarioPassedMessage("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' passed (" + scenariosFailed + "/" + scenariosPassed + "/" + scenarioFileSize + ")");
			if (config.isSilent() && LOG_LEVEL_ORDER.indexOf("[" + logLevel + "]") <= LOG_LEVEL_ORDER.indexOf("[scenario passed/failed]")) {
				try {
					config.getOut().write("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' passed");
				} catch (IOException e) {
				}
			}
		} else if (scenarioPassed == RESULT_AUTOSAVED) {
			scenariosAutosaved++;
			larvaTool.scenarioAutosavedMessage("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' passed after autosave");
			if (config.isSilent()) {
				try {
					config.getOut().write("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' passed after autosave");
				} catch (IOException e) {
				}
			}
		} else {
			scenariosFailed++;
			larvaTool.scenarioFailedMessage("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' failed (" + scenariosFailed + "/" + scenariosPassed + "/" + scenarioFileSize + ")");
			if (config.isSilent()) {
				try {
					config.getOut().write("Scenario '" + shortName + " - " + properties.getProperty("scenario.description") + "' failed");
				} catch (IOException e) {
				}
			}
		}
		larvaTool.writeHtml("</div>", false);
	}

}
