package org.frankframework.larva;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.larva.output.LarvaWriter;
import org.frankframework.util.AppConstants;

@Log4j2
public class TestRunStatus {
	private final @Getter AtomicInteger scenariosFailed = new AtomicInteger();
	private final @Getter AtomicInteger scenariosPassed = new AtomicInteger();
	private final @Getter AtomicInteger scenariosAutosaved = new AtomicInteger();

	private final @Getter LarvaConfig larvaConfig;
	private final @Getter LarvaWriter out;

	private @Getter SortedMap<String, String> scenarioDirectories = new TreeMap<>();
	private @Getter Map<File, String> scenarioFiles = Map.of();

	private @Getter List<File> scenariosToRun = List.of();

	public TestRunStatus(LarvaConfig larvaConfig, LarvaWriter out) {
		this.larvaConfig = larvaConfig;
		this.out = out;
	}

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
			if (scenariosRoots.containsKey(directory)) {
				out.errorMessage("A root directory named [%s] already exists".formatted(directory));
				continue;
			}
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
			directory = LarvaUtil.getAbsolutePath(parent, directory, true);
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

	public List<File> getScenariosToRun(String execute) {

		List<File> scenarios;
		if (execute.endsWith(".properties")) {
			out.debugMessage("Read one scenario");
			scenarios = List.of(new File(execute));
		} else if (execute.equals(larvaConfig.getActiveScenariosDirectory())) {
			out.debugMessage("Executing all scenario files from root directory '" + larvaConfig.getActiveScenariosDirectory() + "'");
			scenarios = List.copyOf(scenarioFiles.keySet());
		} else {
			scenarios = scenarioFiles.keySet().stream()
					.filter(f -> f.getAbsolutePath().startsWith(execute))
					.toList();
		}
		this.scenariosToRun = scenarios;
		return scenariosToRun;
	}

	public int getScenarioCount() {
		return scenariosToRun.size();
	}

	public void readScenarioFiles(ScenarioLoader scenarioLoader) {
		scenarioFiles = scenarioLoader.readScenarioFiles(larvaConfig.getActiveScenariosDirectory());
	}

	public @Nullable String buildScenariosPassedMessage(long executionTime) {
		String formattedTime = LarvaUtil.formatDuration(executionTime);

		int scenariosTotal = getScenarioCount();
		int scenariosPassed = getScenariosPassed().get();

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

		int scenariosTotal = getScenarioCount();
		int scenariosFailed = getScenariosFailed().get();

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

		int scenariosTotal = getScenarioCount();
		int scenariosAutoSaved = getScenariosAutosaved().get();

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

		int scenariosTotal = getScenarioCount();
		int scenariosPassed = getScenariosPassed().get();
		int scenariosFailed = getScenariosFailed().get();
		int scenariosAutoSaved = getScenariosAutosaved().get();

		if (scenariosPassed == scenariosTotal ||
				scenariosFailed == scenariosTotal ||
				scenariosAutoSaved == scenariosTotal) {
			return null;
		}
		return scenariosTotal + " scenarios executed in " + formattedTime;
	}
}
