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

import static org.frankframework.larva.LarvaTool.RESULT_AUTOSAVED;
import static org.frankframework.larva.LarvaTool.RESULT_ERROR;
import static org.frankframework.larva.LarvaTool.RESULT_OK;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.logging.log4j.CloseableThreadContext;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.IbisContext;
import org.frankframework.larva.actions.LarvaActionFactory;
import org.frankframework.larva.actions.LarvaApplicationContext;
import org.frankframework.larva.actions.LarvaScenarioAction;
import org.frankframework.larva.output.LarvaWriter;
import org.frankframework.larva.output.TestExecutionObserver;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StringUtil;

@Log4j2
public class ScenarioRunner {

	private static final AtomicLong correlationIdSuffixCounter = new AtomicLong(1);
	private static final String TESTTOOL_CORRELATIONID = "Test Tool correlation id";
	public final List<String> parallelBlacklistDirs;

	private final LarvaTool larvaTool;
	private final IbisContext ibisContext;
	private final AppConstants appConstants;
	private final TestRunStatus testRunStatus;
	private final int waitBeforeCleanUp;
	private final LarvaLogLevel logLevel;
	private @Setter boolean multipleThreads;
	private final int threads;

	private final LarvaWriter out;
	private final LarvaConfig larvaConfig;
	private final TestExecutionObserver testExecutionObserver;

	public ScenarioRunner(LarvaTool larvaTool) {
		this.larvaTool = larvaTool;
		this.ibisContext = larvaTool.getIbisContext();
		this.larvaConfig = larvaTool.getLarvaConfig();
		this.testExecutionObserver = larvaTool.getTestExecutionObserver();
		this.testRunStatus = larvaTool.getTestRunStatus();
		this.out = larvaTool.getWriter();

		this.logLevel = larvaConfig.getLogLevel();
		this.waitBeforeCleanUp = larvaConfig.getWaitBeforeCleanup();
		this.multipleThreads = larvaConfig.isMultiThreaded();
		this.appConstants = AppConstants.getInstance();

		String blackListDirs = appConstants.getProperty("larva.parallel.blacklistDirs", "");
		parallelBlacklistDirs = StringUtil.split(blackListDirs);
		log.info("Setting parallel blacklist dirs to: {}", parallelBlacklistDirs);

		threads = AppConstants.getInstance().getInt("larva.parallel.threads", 4);
	}

	public void runScenarios(List<File> scenarioConfigurationFiles, String larvaScenarioRootDirectory) {
		Map<String, List<File>> filesByFolder = groupFilesByFolder(scenarioConfigurationFiles, larvaScenarioRootDirectory);
		log.debug("Found: {} folders", filesByFolder.size());

		List<File> singleThreadedScenarios;
		if (multipleThreads) {
			singleThreadedScenarios = runScenariosMultithreaded(larvaScenarioRootDirectory, filesByFolder);
		} else {
			singleThreadedScenarios = scenarioConfigurationFiles;
		}

		runScenariosSingleThreaded(singleThreadedScenarios, larvaScenarioRootDirectory);
		log.info("Summary Larva run Scenario's: {} passed, {} failed. Total: {}", testRunStatus.getScenariosPassed(), testRunStatus.getScenariosFailed(), testRunStatus.getScenarioCount());
	}

	private List<File> runScenariosMultithreaded(String currentScenariosRootDirectory, Map<String, List<File>> filesByFolder) {
		List<File> singleThreadedScenarios = new ArrayList<>(); // Collect scenarios that should be run single threaded

		// Run each scenario folder in a separate thread
		// Not using a try-with-resources because the default awaitTermination is set on 1 day
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
		try {
			filesByFolder.forEach((folder, files) -> {
				log.debug("Starting FOLDER: {} - found: {} files", folder, files.size());
				if (parallelBlacklistDirs.contains(folder)) {
					log.debug("Skipping folder because found in parallel blacklist: {}", folder);
					singleThreadedScenarios.addAll(files);
					return;
				}
				executor.submit(() -> files.forEach(file -> runOneFile(file, currentScenariosRootDirectory, false)));
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

	private void runScenariosSingleThreaded(List<File> singleThreadedScenarios, String currentScenariosDirectory) {
		if (singleThreadedScenarios.isEmpty()) {
			return;
		}
		out.infoMessage("Starting " + singleThreadedScenarios.size() + " Single threaded Scenarios");
		singleThreadedScenarios.forEach(file -> runOneFile(file, currentScenariosDirectory, true));
	}

	// Sort property files by folder
	public static Map<String, List<File>> groupFilesByFolder(List<File> scenarioFiles, String currentScenariosRootDirectory) {
		return scenarioFiles.stream()
				.collect(Collectors.groupingBy(scenarioFile -> getScenarioFolder(scenarioFile, currentScenariosRootDirectory)));
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
	 * @param scenarioConfigurationFile full path to the `.properties` configuration file
	 * @param larvaScenariosRootDirectory the root directory of all larva scenarios
	 * @param flushLogsForEveryScenarioStep if true, the log will be flushed after every scenario step
	 */
	public int runOneFile(File scenarioConfigurationFile, String larvaScenariosRootDirectory, boolean flushLogsForEveryScenarioStep) {
		int scenarioPassed = RESULT_ERROR;

		String scenarioDirectory = scenarioConfigurationFile.getParentFile().getAbsolutePath() + File.separator;
		String longName = scenarioConfigurationFile.getAbsolutePath();
		String scenarioName = longName.substring(larvaScenariosRootDirectory.length() - 1, longName.length() - ".properties".length());
		log.info("Running scenario [{}]", scenarioName);
		testExecutionObserver.startScenario(testRunStatus, scenarioName);
		try (CloseableThreadContext.Instance ignored = CloseableThreadContext.put("scenario", scenarioName);
			 // This is far from optimal, but without refactoring the whole LarvaTool, this is the quick and dirty way to do it
			 LarvaApplicationContext applicationContext = new LarvaApplicationContext(ibisContext, scenarioDirectory)
		) {
			larvaTool.debugMessage("Read property file " + scenarioConfigurationFile.getName());
			Properties properties = larvaTool.getScenarioLoader().readScenarioProperties(scenarioConfigurationFile, appConstants);
			String scenarioDescription = properties.getProperty("scenario.description");

			larvaTool.debugMessage("Open actions");

			LarvaActionFactory actionFactory = new LarvaActionFactory(larvaTool);

			// increment suffix for each scenario
			String correlationId = TESTTOOL_CORRELATIONID + "(" + correlationIdSuffixCounter.getAndIncrement() + ")";
			Map<String, LarvaScenarioAction> larvaActions = actionFactory.createLarvaActions(properties, applicationContext, correlationId);
			if (larvaActions == null || larvaActions.isEmpty()) {
				testRunStatus.getScenariosFailed().incrementAndGet();
				testExecutionObserver.finishScenario(testRunStatus, scenarioName, RESULT_ERROR, "Could not create LarvaActions");
				return RESULT_ERROR;
			}
			applicationContext.configure();
			applicationContext.start();

			// Start the scenario
			// TODO: The buffering is now not threadsafe yet.
			larvaTool.debugMessage("Read steps from property file");
			List<String> stepList = getSteps(properties);
			if (stepList.isEmpty()) {
				testRunStatus.getScenariosFailed().incrementAndGet();
				testExecutionObserver.finishScenario(testRunStatus, scenarioName, RESULT_ERROR, "No steps found");
				return RESULT_ERROR;
			}
			larvaTool.debugMessage("Execute steps");
			boolean allStepsPassed = true;
			boolean autoSaved = false;
			Iterator<String> steps = stepList.iterator();
			while (allStepsPassed && steps.hasNext()) {
				String step = steps.next();
				String stepDisplayName = scenarioName + " - " + step + " - " + properties.get(step);
				testExecutionObserver.startStep(testRunStatus, stepDisplayName);
				long start = System.currentTimeMillis();
				int stepPassed = larvaTool.executeStep(step, properties, stepDisplayName, larvaActions, correlationId);
				long end = System.currentTimeMillis();
				testExecutionObserver.finishStep(testRunStatus, stepDisplayName, stepPassed, buildStepFinishedMessage(stepDisplayName, stepPassed, (end - start)));
				if (stepPassed == RESULT_ERROR) {
					allStepsPassed = false;
				} else if (stepPassed == RESULT_AUTOSAVED) {
					autoSaved = true;
				}
				if (flushLogsForEveryScenarioStep) {
					out.flush();
				}
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
			larvaTool.debugMessage("Close actions");
			boolean remainingMessagesFound = actionFactory.closeLarvaActions(larvaActions);
			if (remainingMessagesFound) {
				if (logLevel.shouldLog(LarvaLogLevel.STEP_PASSED_FAILED)) {
					testExecutionObserver.finishStep(testRunStatus, scenarioName + " - Remaining messages found", RESULT_ERROR, "Found one or more messages on actions or in database after scenario executed");
				}
				scenarioPassed = RESULT_ERROR;
			}

			if (scenarioPassed == RESULT_OK) {
				testRunStatus.getScenariosPassed().incrementAndGet();
			} else if (scenarioPassed == RESULT_AUTOSAVED) {
				testRunStatus.getScenariosAutosaved().incrementAndGet();
			} else {
				testRunStatus.getScenariosFailed().incrementAndGet();
			}
			testExecutionObserver.finishScenario(testRunStatus, scenarioName, scenarioPassed, buildScenarioFinishedMessage(scenarioName, scenarioDescription, scenarioPassed, 0));
			return scenarioPassed;
		} catch (Exception e) {
			log.warn("Error occurred while creating Larva Scenario Actions", e);
			testRunStatus.getScenariosFailed().incrementAndGet();
			testExecutionObserver.finishScenario(testRunStatus, scenarioName, RESULT_ERROR, "Error occurred while executing Larva Scenario: " + e.getMessage());
			larvaTool.errorMessage(e.getClass().getSimpleName() + ": "+e.getMessage(), e);
			return RESULT_ERROR;
		}
	}

	private List<String> getSteps(Properties properties) {
		List<String> steps = new ArrayList<>();
		int i = 1;
		boolean lastStepFound = false;
		while (!lastStepFound) {
			boolean stepFound = false;
			Enumeration<?> enumeration = properties.propertyNames();
			while (enumeration.hasMoreElements()) {
				String key = (String) enumeration.nextElement();
				if (key.startsWith("step" + i + ".") && (key.endsWith(".read") || key.endsWith(".write") || (larvaConfig.isAllowReadlineSteps() && key.endsWith(".readline")) || key.endsWith(".writeline"))) {
					if (!stepFound) {
						steps.add(key);
						stepFound = true;
						larvaTool.debugMessage("Added step '" + key + "'");
					} else {
						larvaTool.errorMessage("More than one step" + i + " properties found, already found '" + steps.get(steps.size() - 1) + "' before finding '" + key + "'");
					}
				}
			}
			if (!stepFound) {
				lastStepFound = true;
			}
			i++;
		}
		larvaTool.debugMessage(steps.size() + " steps found");
		return steps;
	}

	private String buildStepFinishedMessage(String stepName, int stepResult, long stepDurationMs) {
		StringBuilder stepResultMessage = new StringBuilder("Step '");
		stepResultMessage.append(stepName).append("' ");
		if (stepResult == LarvaTool.RESULT_OK) {
			stepResultMessage.append("passed.");
		} else if (stepResult == LarvaTool.RESULT_AUTOSAVED) {
			stepResultMessage.append("passed after autosave.");
		} else {
			stepResultMessage.append("failed.");
		}
		if (larvaConfig.getLogLevel().shouldLog(LarvaLogLevel.DEBUG)) {
			stepResultMessage.append(" Duration: ").append(stepDurationMs).append(" ms");
		}
		return stepResultMessage.toString();
	}

	private String buildScenarioFinishedMessage(String scenarioName, String scenarioDescription, int scenarioResult, long scenarioDurationMs) {
		StringBuilder scenarioResultMessage = new StringBuilder("Scenario '");
		scenarioResultMessage.append(scenarioName).append(" - ").append(scenarioDescription).append("' ");
		if (scenarioResult == LarvaTool.RESULT_OK) {
			scenarioResultMessage.append("passed");
		} else if (scenarioResult == LarvaTool.RESULT_AUTOSAVED) {
			scenarioResultMessage.append("passed after autosave");
		} else {
			scenarioResultMessage.append("failed");
		}
		if (larvaConfig.getLogLevel().shouldLog(LarvaLogLevel.DEBUG)) {
			scenarioResultMessage.append(". Duration: ").append(scenarioDurationMs).append(" ms");
		}
		scenarioResultMessage.append(" (").append(testRunStatus.getScenariosFailed()).append('/').append(testRunStatus.getScenariosAutosaved().get() + testRunStatus.getScenariosPassed().get()).append('/').append(testRunStatus.getScenarioCount()).append(')');
		return scenarioResultMessage.toString();
	}
}
