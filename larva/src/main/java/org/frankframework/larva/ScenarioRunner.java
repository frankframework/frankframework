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
package org.frankframework.larva;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.springframework.context.ApplicationContext;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.TimeoutException;
import org.frankframework.larva.actions.LarvaActionFactory;
import org.frankframework.larva.actions.LarvaActionUtils;
import org.frankframework.larva.actions.LarvaApplicationContext;
import org.frankframework.larva.actions.LarvaScenarioAction;
import org.frankframework.larva.output.TestExecutionObserver;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StringResolver;
import org.frankframework.util.StringUtil;

@Log4j2
public class ScenarioRunner {

	private static final AtomicLong correlationIdSuffixCounter = new AtomicLong(1);
	private static final String TESTTOOL_CORRELATIONID = "Test Tool correlation id";
	public final List<String> parallelBlacklistDirs;

	private final LarvaTool larvaTool;
	private final ApplicationContext applicationContext;
	private final AppConstants appConstants;
	private final TestRunStatus testRunStatus;
	private final int waitBeforeCleanUp;
	private final LarvaLogLevel logLevel;
	private @Setter boolean multipleThreads;
	private final int threads;

	private final LarvaConfig larvaConfig;
	private final TestExecutionObserver testExecutionObserver;

	public ScenarioRunner(LarvaTool larvaTool, TestExecutionObserver testExecutionObserver, TestRunStatus testRunStatus) {
		this.larvaTool = larvaTool;
		this.applicationContext = larvaTool.getApplicationContext();
		this.larvaConfig = larvaTool.getLarvaConfig();
		this.testExecutionObserver = testExecutionObserver;
		this.testRunStatus = testRunStatus;

		this.logLevel = larvaConfig.getLogLevel();
		this.waitBeforeCleanUp = larvaConfig.getWaitBeforeCleanup();
		this.multipleThreads = larvaConfig.isMultiThreaded();
		this.appConstants = AppConstants.getInstance();

		String blackListDirs = appConstants.getProperty("larva.parallel.blacklistDirs", "");
		parallelBlacklistDirs = StringUtil.split(blackListDirs);
		log.info("Setting parallel blacklist dirs to: {}", parallelBlacklistDirs);

		threads = appConstants.getInt("larva.parallel.threads", 4);
	}

	public void runScenarios(List<Scenario> scenarios, String larvaScenarioRootDirectory) {
		Map<String, List<Scenario>> filesByFolder = groupScenariosByFolder(scenarios, larvaScenarioRootDirectory);
		log.debug("Found: {} folders", filesByFolder.size());

		List<Scenario> singleThreadedScenarios;
		if (multipleThreads) {
			singleThreadedScenarios = runScenariosMultithreaded(filesByFolder);
		} else {
			singleThreadedScenarios = scenarios;
		}

		runScenariosSingleThreaded(singleThreadedScenarios);
		log.info("Summary Larva run Scenario's: {} passed, {} failed. Total: {}", testRunStatus.getScenariosPassedCount(), testRunStatus.getScenariosFailedCount(), testRunStatus.getScenarioExecuteCount());
	}

	private List<Scenario> runScenariosMultithreaded(Map<String, List<Scenario>> filesByFolder) {
		List<Scenario> singleThreadedScenarios = new ArrayList<>(); // Collect scenarios that should be run single threaded

		// Run each scenario folder in a separate thread
		// Not using a try-with-resources because the default awaitTermination is set on 1 day
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
		try {
			filesByFolder.forEach((folder, scenarios) -> {
				log.debug("Starting FOLDER: {} - found: {} files", folder, scenarios.size());
				if (parallelBlacklistDirs.contains(folder)) {
					log.debug("Skipping folder because found in parallel blacklist: {}", folder);
					singleThreadedScenarios.addAll(scenarios);
					return;
				}
				executor.submit(() -> scenarios.forEach(scenario -> runOneFile(scenario, false)));
			});
			// Wait for all scenarios to finish
			executor.shutdown();
		} finally {
			try {
				executor.shutdown();
				executor.awaitTermination(15, TimeUnit.MINUTES); // Guessing a max timeout, otherwise we might hang forever
			} catch (InterruptedException e) {
				log.warn("Interrupted while waiting for scenario runner to finish", e);
				Thread.currentThread().interrupt();
			}
		}
		return singleThreadedScenarios;
	}

	private void runScenariosSingleThreaded(List<Scenario> singleThreadedScenarios) {
		if (singleThreadedScenarios.isEmpty()) {
			return;
		}
		larvaTool.infoMessage("Starting " + singleThreadedScenarios.size() + " Single threaded Scenarios");
		singleThreadedScenarios.forEach(scenario -> runOneFile(scenario, true));
	}

	// Sort property files by folder
	public static Map<String, List<Scenario>> groupScenariosByFolder(List<Scenario> scenarioFiles, String currentScenariosRootDirectory) {
		return scenarioFiles.stream()
				.collect(Collectors.groupingBy(scenarioFile -> getScenarioFolder(scenarioFile.getScenarioFile(), currentScenariosRootDirectory)));
	}

	private static String getScenarioFolder(File file, String currentScenariosDirectory) {
		if (currentScenariosDirectory.endsWith(File.separator)) {
			currentScenariosDirectory = currentScenariosDirectory.substring(0, currentScenariosDirectory.length() - 1);
		}
		if (currentScenariosDirectory.equals(file.getParent())) {
			return "";
		}

		File parentFolder = file.getParentFile();
		while (parentFolder != null && !currentScenariosDirectory.equals(parentFolder.getParent())) {
			parentFolder = parentFolder.getParentFile();
		}
		if (parentFolder == null) {
			return file.getParent();
		}
		return parentFolder.getName();
	}

	/**
	 * @param scenario full path to the `.properties` configuration file
	 * @param flushLogsForEveryScenarioStep if true, the log will be flushed after every scenario step
	 */
	public int runOneFile(Scenario scenario, boolean flushLogsForEveryScenarioStep) {
		long scenarioStart = System.currentTimeMillis();
		int scenarioResult = LarvaTool.RESULT_ERROR;

		File scenarioConfigurationFile = scenario.getScenarioFile();
		String scenarioDirectory = scenarioConfigurationFile.getParentFile().getAbsolutePath() + File.separator;
		log.info("Running scenario [{}]", scenario.getName());
		testExecutionObserver.startScenario(testRunStatus, scenario);
		try (CloseableThreadContext.Instance ignored = CloseableThreadContext.put("scenario", scenario.getName());
			// This is far from optimal, but without refactoring the whole LarvaTool, this is the quick and dirty way to do it
			LarvaApplicationContext applicationContext = new LarvaApplicationContext(this.applicationContext, scenarioDirectory)
		) {
			Properties properties = scenario.getProperties();
			log.debug("Open actions");

			LarvaActionFactory actionFactory = new LarvaActionFactory(larvaTool, testExecutionObserver);

			// increment suffix for each scenario
			String correlationId = TESTTOOL_CORRELATIONID + "(" + correlationIdSuffixCounter.getAndIncrement() + ")";
			Map<String, LarvaScenarioAction> larvaActions = actionFactory.createLarvaActions(scenario, applicationContext, correlationId);
			if (larvaActions == null || larvaActions.isEmpty()) {
				testRunStatus.scenarioFailed(scenario);
				testExecutionObserver.finishScenario(testRunStatus, scenario, LarvaTool.RESULT_ERROR, "Could not create LarvaActions");
				return LarvaTool.RESULT_ERROR;
			}
			applicationContext.configure();
			applicationContext.start();

			// Start the scenario
			// TODO: The buffering is now not threadsafe yet.
			log.debug("Get steps from property file");
			List<String> stepList = getSteps(scenario);
			if (stepList.isEmpty()) {
				testRunStatus.scenarioFailed(scenario);
				testExecutionObserver.finishScenario(testRunStatus, scenario, LarvaTool.RESULT_ERROR, "No steps found");
				return LarvaTool.RESULT_ERROR;
			}
			log.debug("Execute steps");
			boolean allStepsPassed = true;
			boolean autoSaved = false;
			Iterator<String> steps = stepList.iterator();
			while (allStepsPassed && steps.hasNext()) {
				String step = steps.next();
				testExecutionObserver.startStep(testRunStatus, scenario, step);

				long stepStart = System.currentTimeMillis();
				int stepResult = executeStep(scenario, step, larvaActions, correlationId);
				long stepEnd = System.currentTimeMillis();

				testExecutionObserver.finishStep(testRunStatus, scenario, step, stepResult, buildStepFinishedMessage(scenario, step, stepResult, (stepEnd - stepStart)));
				if (stepResult == LarvaTool.RESULT_ERROR) {
					allStepsPassed = false;
				} else if (stepResult == LarvaTool.RESULT_AUTOSAVED) {
					autoSaved = true;
				}
				if (flushLogsForEveryScenarioStep) {
					larvaTool.flushOutput();
				}
			}
			if (allStepsPassed) {
				if (autoSaved) {
					scenarioResult = LarvaTool.RESULT_AUTOSAVED;
				} else {
					scenarioResult = LarvaTool.RESULT_OK;
				}
			}
			log.debug("Wait [{}]ms before clean up", waitBeforeCleanUp);
			try {
				Thread.sleep(waitBeforeCleanUp);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			log.debug("Close actions");
			boolean remainingMessagesFound = actionFactory.closeLarvaActions(larvaActions);
			if (remainingMessagesFound) {
				if (logLevel.shouldLog(LarvaLogLevel.STEP_PASSED_FAILED)) {
					testExecutionObserver.finishStep(testRunStatus, scenario, scenario.getName() + " - Remaining messages found", LarvaTool.RESULT_ERROR, "Found one or more messages on actions or in database after scenario executed");
				}
				scenarioResult = LarvaTool.RESULT_ERROR;
			}

			if (scenarioResult == LarvaTool.RESULT_OK) {
				testRunStatus.scenarioPassed(scenario);
			} else if (scenarioResult == LarvaTool.RESULT_AUTOSAVED) {
				testRunStatus.scenarioAutosaved(scenario);
			} else {
				testRunStatus.scenarioFailed(scenario);
			}
			long scenarioDurationMs = System.currentTimeMillis() - scenarioStart;
			testExecutionObserver.finishScenario(testRunStatus, scenario, scenarioResult, buildScenarioFinishedMessage(scenario, scenarioResult, scenarioDurationMs));
			log.info("Finished scenario [{}], result: {}", scenario.getName(), scenarioResult == LarvaTool.RESULT_OK ? "OK" : "FAILED");
			return scenarioResult;
		} catch (Exception e) {
			log.warn("Error occurred while creating Larva Scenario Actions for scenario [{}]", scenario.getName(), e);
			testRunStatus.scenarioFailed(scenario);
			testExecutionObserver.finishScenario(testRunStatus, scenario, LarvaTool.RESULT_ERROR, "Error occurred while executing Larva Scenario: " + e.getMessage());
			larvaTool.errorMessage(e.getClass().getSimpleName() + ": "+e.getMessage(), e);
			return LarvaTool.RESULT_ERROR;
		} finally {
			// Clear caches to keep memory consumption under control
			scenario.clearScenarioCaches();
		}
	}

	// Ideally, this should be moved to its own class.
	private int executeStep(Scenario scenario, String step, Map<String, LarvaScenarioAction> actions, String correlationId) {
		Properties properties = scenario.getProperties();
		String fileName = properties.getProperty(step);
		int i = step.indexOf('.');
		String actionName;
		Message fileContent;

		// Read the scenario file for this step
		try {
			fileContent = readScenarioStepData(scenario, step);
		} catch (Exception e) {
			larvaTool.errorMessage("Error reading data for step " + step + ":" + e.getMessage(), e);
			return LarvaTool.RESULT_ERROR;
		}

		actionName = step.substring(i + 1, step.lastIndexOf("."));
		Object actionFactoryClassname = properties.get(actionName + LarvaActionFactory.CLASS_NAME_PROPERTY_SUFFIX);
		LarvaScenarioAction scenarioAction = actions.get(actionName);
		if (scenarioAction == null) {
			larvaTool.errorMessage("Property '" + actionName + LarvaActionFactory.CLASS_NAME_PROPERTY_SUFFIX + "' not found or not valid");
			return LarvaTool.RESULT_ERROR;
		}

		if (step.endsWith(".read") || step.endsWith(".readline")) {
			if ("org.frankframework.larva.XsltProviderListener".equals(actionFactoryClassname)) {
				Properties scenarioStepProperties = LarvaActionUtils.getSubProperties(properties, step);
				Map<String, Object> xsltParameters = larvaTool.createParametersMapFromParamProperties(scenarioStepProperties);
				return executeActionWriteStep(scenario, step, scenarioAction, actionName, fileContent, correlationId, xsltParameters); // XsltProviderListener has .read and .write reversed
			} else {
				return executeActionReadStep(scenario, step, scenarioAction, actionName, fileName, fileContent);
			}
		} else {
			if ("org.frankframework.larva.XsltProviderListener".equals(actionFactoryClassname)) {
				return executeActionReadStep(scenario, step, scenarioAction, actionName, fileName, fileContent);  // XsltProviderListener has .read and .write reversed
			} else {
				return executeActionWriteStep(scenario, step, scenarioAction, actionName, fileContent, correlationId, null);
			}
		}
	}

	@Nonnull
	private Message readScenarioStepData(Scenario scenario, String step) throws IOException {
		Properties properties = scenario.getProperties();
		String fileName = properties.getProperty(step);
		String stepDataFileAbsolutePath = scenario.getStepDataFile(step);
		Message fileContent;
		if (StringUtils.isBlank(fileName)) {
			throw new LarvaException("No file specified for step '" + step + "'");
		}
		if (step.endsWith("readline") || step.endsWith("writeline")) {
			fileContent = new Message(fileName);
		} else {
			if (fileName.endsWith("ignore")) {
				log.debug("creating dummy expected file for filename [{}]", fileName);
				fileContent = new Message("ignore");
			} else {
				larvaTool.debugMessage("Read file " + fileName);
				fileContent = LarvaUtil.readFile(stepDataFileAbsolutePath);
			}
		}
		String resolveProperties = properties.getProperty("scenario.resolveProperties", "true");
		if ("false".equalsIgnoreCase(resolveProperties) || "!true".equalsIgnoreCase(resolveProperties)) {
			return fileContent;
		}
		String fileData = larvaTool.messageToString(fileContent);
		if (fileData == null) {
			throw new LarvaException("Failed to resolve properties in input file [" + fileName + "] for step '" + step + "'");
		}
		return new Message(StringResolver.substVars(fileData, appConstants), fileContent.copyContext());
	}

	private int executeActionWriteStep(Scenario scenario, String step, LarvaScenarioAction scenarioAction, String actionName, Message fileContent, String correlationId, Map<String, Object> xsltParameters) {
		try {
			scenarioAction.executeWrite(fileContent, correlationId, xsltParameters);
			testExecutionObserver.stepMessage(scenario, step, "Successfully wrote message to '" + actionName + "':", larvaTool.messageToString(fileContent));
			log.debug("Successfully wrote message to '{}'", actionName);
			return LarvaTool.RESULT_OK;
		} catch(TimeoutException e) {
			larvaTool.errorMessage("Timeout sending message to '" + actionName + "': " + e.getMessage(), e);
		} catch(Exception e) {
			larvaTool.errorMessage("Could not send message to '" + actionName + "' ("+e.getClass().getSimpleName()+"): " + e.getMessage(), e);
		}
		return LarvaTool.RESULT_ERROR;
	}

	private int executeActionReadStep(Scenario scenario, String step, LarvaScenarioAction scenarioAction, String actionName, String fileName, Message expected) {
		try {
			Message message = scenarioAction.executeRead(scenario.getProperties()); // cannot close this message because of FrankSender (JSON scenario02)
			if (message == null) {
				if (StringUtils.isEmpty(fileName)) {
					return LarvaTool.RESULT_OK;
				} else {
					larvaTool.errorMessage("Could not read from ["+actionName+"] (null returned)");
				}
			} else {
				if (StringUtils.isEmpty(fileName)) {
					testExecutionObserver.stepMessage(scenario, step, "Unexpected message read from '" + actionName + "':", larvaTool.messageToString(message));
				} else if (fileName.endsWith("ignore")) {
					larvaTool.debugMessage("ignoring compare for filename '"+fileName+"'");
					return LarvaTool.RESULT_OK;
				} else {
					return larvaTool.compareResult(testExecutionObserver, scenario, step, fileName, expected, message);
				}
			}
		} catch (Exception e) {
			larvaTool.errorMessage("Could not read from ["+actionName+"] ("+e.getClass().getSimpleName()+"): " + e.getMessage(), e);
		}

		return LarvaTool.RESULT_ERROR;
	}

	private List<String> getSteps(Scenario scenario) {
		List<String> steps = scenario.getSteps(larvaConfig);
		larvaTool.debugMessage(steps.size() + " steps found");
		return steps;
	}

	private String buildStepFinishedMessage(Scenario scenario, String step, int stepResult, long stepDurationMs) {
		String stepName = scenario.getStepDisplayName(step);
		StringBuilder stepResultMessage = new StringBuilder("Step '");
		stepResultMessage.append(stepName).append("' ");
		if (stepResult == LarvaTool.RESULT_OK) {
			stepResultMessage.append("passed");
		} else if (stepResult == LarvaTool.RESULT_AUTOSAVED) {
			stepResultMessage.append("passed after autosave");
		} else {
			stepResultMessage.append("failed");
		}
		if (larvaConfig.getLogLevel().shouldLog(LarvaLogLevel.DEBUG)) {
			stepResultMessage.append(" in ").append(stepDurationMs).append(" ms");
		}
		return stepResultMessage.toString();
	}

	private String buildScenarioFinishedMessage(Scenario scenario, int scenarioResult, long scenarioDurationMs) {
		StringBuilder scenarioResultMessage = new StringBuilder("Scenario '");
		scenarioResultMessage.append(scenario.getName()).append(" - ").append(scenario.getDescription()).append("' ");
		if (scenarioResult == LarvaTool.RESULT_OK) {
			scenarioResultMessage.append("passed");
		} else if (scenarioResult == LarvaTool.RESULT_AUTOSAVED) {
			scenarioResultMessage.append("passed after autosave");
		} else {
			scenarioResultMessage.append("failed");
		}
		String scenarioDurationFormatted = LarvaUtil.formatDuration(scenarioDurationMs);
		scenarioResultMessage.append(" in ").append(scenarioDurationFormatted);
		scenarioResultMessage.append(" (").append(testRunStatus.getScenariosFailedCount()).append('/').append(testRunStatus.getScenariosAutosavedCount() + testRunStatus.getScenariosPassedCount()).append('/').append(testRunStatus.getScenarioExecuteCount()).append(')');
		return scenarioResultMessage.toString();
	}
}
