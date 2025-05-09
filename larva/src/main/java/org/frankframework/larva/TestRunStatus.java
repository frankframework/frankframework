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
package org.frankframework.larva;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.larva.output.LarvaWriter;
import org.frankframework.util.AppConstants;

/**
 * Keep track of the status of a test-run.
 * <p>
 * This class also tracks the scenario directories and all scenarios found, and the scenarios to run.
 * The actual loading of scenario data is delegated to the {@link ScenarioLoader}.
 * </p>>
 * <p>
 *     The TestRunStatus uses the {@link LarvaConfig} to find the configured {@link LarvaConfig#getActiveScenariosDirectory()}
 *     and the {@link LarvaWriter} for debug messages.
 * </p>
 */
@Log4j2
public class TestRunStatus {
	private final @Getter LarvaConfig larvaConfig;
	private final @Getter LarvaWriter out;

	private @Getter SortedMap<String, String> scenarioDirectories = new TreeMap<>();
	private @Getter Map<Scenario.ID, Scenario> allScenarios = Map.of();

	private @Getter List<Scenario> scenariosToRun = List.of();

	private final @Getter List<Scenario> failedScenarios = Collections.synchronizedList(new ArrayList<>());
	private final @Getter List<Scenario> passedScenarios = Collections.synchronizedList(new ArrayList<>());
	private final @Getter List<Scenario> autoSavedScenarios = Collections.synchronizedList(new ArrayList<>());

	public TestRunStatus(LarvaConfig larvaConfig, LarvaWriter out) {
		this.larvaConfig = larvaConfig;
		this.out = out;
	}

	/**
	 * Initialize the list of scenario-directories from the properties in {@link AppConstants}.
	 * <p>
	 *     The code looks at numbered properties {@code scenariosrootN.directory}, {@code scenariosrootN.description} and {@code scenariosrootN.m2e.pom.properties}
	 *     starting {@code scenariosroot1.directory}.
	 * </p>
	 * <p>
	 *     If {@code scenariosrootN.m2e.pom.properties} exists then it is used to determine to root path
	 *     for that scenario directory. Otherwise, the scenario directory specified is expected to be relative
	 *     to the web app root path.
	 * </p>
	 * <p>
	 *     The map with scenario roots has the description as key and full path to the directory as value.
	 * </p>
	 *
	 * @return Returns the path to the active / selected scenario root directory.
	 */
	public String initScenarioDirectories() {
		String realPath = LarvaUtil.getParentOfWebappRoot();
		if (realPath == null) {
			out.errorMessage("Could not read webapp real path");
			return null;
		}
		AppConstants appConstants = AppConstants.getInstance();

		SortedMap<String, String> scenariosRoots = new TreeMap<>(new CaseInsensitiveComparator());

		int j = 0;
		while (true) {
			++j;
			String directory = appConstants.getProperty("scenariosroot" + j + ".directory");
			String description = appConstants.getProperty("scenariosroot" + j + ".description");
			String m2eFileName = appConstants.getProperty("scenariosroot" + j + ".m2e.pom.properties");

			if (directory == null) {
				break;
			}
			if (description == null) {
				out.errorMessage("Could not find description for directory scenariosroot%d [%s]".formatted(j, directory));
				continue;
			}
			if (scenariosRoots.containsKey(description)) {
				out.errorMessage("A root directory named [%s] already exists".formatted(description));
				continue;
			}
			String parent = determineParentPath(realPath, m2eFileName);
			directory = LarvaUtil.getAbsolutePath(parent, directory, true);
			if (scenariosRoots.containsValue(directory)) {
				out.errorMessage("A root directory with path [%s] already exists".formatted(directory));
				continue;
			}
			if (new File(directory).exists()) {
				out.debugMessage("directory for [" + description + "] exists: " + directory);
				scenariosRoots.put(description, directory);
			} else {
				out.debugMessage("directory [" + directory + "] for [" + description + "] does not exist, parent [" + parent + "]");
				scenariosRoots.put("X " + description, directory);
			}
		}
		out.debugMessage("Read scenariosrootdirectory parameter");
		out.debugMessage("Get current scenarios root directory");
		if (StringUtils.isEmpty(larvaConfig.getActiveScenariosDirectory())) {
			String currentScenariosRootDirectory;
			String scenariosRootDefault = appConstants.getProperty("scenariosroot.default");
			if (StringUtils.isNotEmpty(scenariosRootDefault) && scenariosRoots.containsKey(scenariosRootDefault)) {
				currentScenariosRootDirectory = scenariosRoots.get(scenariosRootDefault);
			} else if (!scenariosRoots.isEmpty()) {
				currentScenariosRootDirectory = scenariosRoots.get(scenariosRoots.firstKey());
			} else {
				currentScenariosRootDirectory = null;
			}
			larvaConfig.setActiveScenariosDirectory(currentScenariosRootDirectory);
		}
		scenarioDirectories = scenariosRoots;
		return larvaConfig.getActiveScenariosDirectory();
	}

	private String determineParentPath(String realPath, String m2eFileName) {
		String parent = realPath;
		if (m2eFileName != null) {
			File m2eFile = new File(realPath, m2eFileName);
			if (m2eFile.exists()) {
				out.debugMessage("Read m2e pom.properties: " + m2eFileName);
				Properties m2eProperties = LarvaUtil.readProperties(out, m2eFile);
				parent = m2eProperties.getProperty("m2e.projectLocation");
				out.debugMessage("Use m2e parent: " + parent);
			}
		}
		return parent;
	}

	public void readScenarioFiles(ScenarioLoader scenarioLoader) {
		allScenarios = scenarioLoader.readScenarioFiles(larvaConfig.getActiveScenariosDirectory());
	}

	public List<Scenario> getScenariosToRun(@Nonnull String execute) {

		List<Scenario> scenarios;
		if (execute.endsWith(".properties")) {
			log.debug("Executing single scenario [{}]", execute);
			Scenario scenario = allScenarios.get(new Scenario.ID(execute));
			scenarios = List.of(scenario);
		} else if (execute.equals(larvaConfig.getActiveScenariosDirectory())) {
			log.debug("Executing all scenario files from root directory '{}'",larvaConfig.getActiveScenariosDirectory());
			scenarios = List.copyOf(allScenarios.values());
		} else {
			scenarios = allScenarios.values().stream()
					.filter(s -> s.getScenarioFile().getAbsolutePath().startsWith(execute))
					.toList();
		}
		this.scenariosToRun = scenarios;
		return scenariosToRun;
	}

	public void scenarioFailed(Scenario scenario) {
		failedScenarios.add(scenario);
	}

	public void scenarioPassed(Scenario scenario) {
		passedScenarios.add(scenario);
	}

	public void scenarioAutosaved(Scenario scenario) {
		autoSavedScenarios.add(scenario);
	}

	public int getScenariosFailedCount() {
		return failedScenarios.size();
	}

	public int getScenariosPassedCount() {
		return passedScenarios.size();
	}

	public int getScenariosAutosavedCount() {
		return autoSavedScenarios.size();
	}

	public int getScenarioExecuteCount() {
		return scenariosToRun.size();
	}

	public @Nullable String buildScenariosPassedMessage(long executionTime) {
		String formattedTime = LarvaUtil.formatDuration(executionTime);

		int scenariosTotal = getScenarioExecuteCount();
		int scenariosPassed = getScenariosPassedCount();

		if (scenariosPassed == scenariosTotal) {
			if (scenariosTotal == 1) {
				return "All scenarios passed (1 scenario executed in " + formattedTime + ")";
			} else {
				return "All scenarios passed (" + scenariosTotal + " scenarios executed in " + formattedTime + ")";
			}
		} else if (scenariosPassed == 1) {
			return "1 scenario passed";
		} else if (scenariosPassed > 1) {
			return scenariosPassed + " scenarios passed";
		} else {
			return null;
		}
	}

	public @Nullable String buildScenariosFailedMessage(long executionTime) {
		String formattedTime = LarvaUtil.formatDuration(executionTime);

		int scenariosTotal = getScenarioExecuteCount();
		int scenariosFailed = getScenariosFailedCount();

		if (scenariosFailed == scenariosTotal) {
			if (scenariosTotal == 1) {
				return "All scenarios failed (1 scenario executed in " + formattedTime + ")";
			} else {
				return "All scenarios failed (" + scenariosTotal + " scenarios executed in " + formattedTime + ")";
			}
		} else if (scenariosFailed == 1) {
			return "1 scenario failed";
		} else if (scenariosFailed > 1) {
			return scenariosFailed + " scenarios failed";
		} else {
			return null;
		}
	}

	public @Nullable String buildScenariosAutoSavedMessage(long executionTime) {
		String formattedTime = LarvaUtil.formatDuration(executionTime);

		int scenariosTotal = getScenarioExecuteCount();
		int scenariosAutoSaved = getScenariosAutosavedCount();

		if (scenariosAutoSaved == scenariosTotal) {
			if (scenariosTotal == 1) {
				return "All scenarios passed after autosave (1 scenario executed in " + formattedTime + ")";
			} else {
				return "All scenarios passed after autosave (" + scenariosTotal + " scenarios executed in " + formattedTime + ")";
			}
		} else if (scenariosAutoSaved == 1) {
			return "1 scenario passed after autosave";
		} else if (scenariosAutoSaved > 1) {
			return scenariosAutoSaved + " scenarios passed after autosave";
		} else {
			return null;
		}
	}

	public @Nullable String buildScenariosTotalMessage(long executionTime) {
		String formattedTime = LarvaUtil.formatDuration(executionTime);

		int scenariosTotal = getScenarioExecuteCount();
		int scenariosPassed = getScenariosPassedCount();
		int scenariosFailed = getScenariosFailedCount();
		int scenariosAutoSaved = getScenariosAutosavedCount();

		if (scenariosPassed == scenariosTotal ||
				scenariosFailed == scenariosTotal ||
				scenariosAutoSaved == scenariosTotal) {
			return null;
		}
		return scenariosTotal + " scenarios executed in " + formattedTime;
	}
}
