package org.frankframework.larva;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.larva.output.LarvaWriter;
import org.frankframework.util.AppConstants;

@Log4j2
public class LarvaConfig {
	static final int GLOBAL_TIMEOUT_MILLIS = AppConstants.getInstance().getInt("larva.timeout", 10_000);

	private @Getter @Setter int timeout = GLOBAL_TIMEOUT_MILLIS;
	private @Getter @Setter int waitBeforeCleanup = 100;
	private @Getter @Setter boolean multiThreaded = false;
	private @Getter @Setter LarvaLogLevel logLevel = LarvaLogLevel.WRONG_PIPELINE_MESSAGES;
	private @Getter @Setter boolean autoSaveDiffs = AppConstants.getInstance().getBoolean("larva.diffs.autosave", false);
	private @Getter @Setter boolean silent = false; // TODO: Do we still need this flag?

	private @Getter SortedMap<String, String> scenarioDirectories = new TreeMap<>();
	private @Getter @Setter String activeScenariosDirectory;
	private @Getter Map<File, String> scenarioFiles = Map.of();

	/*
	 * if allowReadlineSteps is set to true, actual results can be compared in line by using .readline steps.
	 * Those results cannot be saved to the inline expected value, however.
	 */
	protected @Getter @Setter boolean allowReadlineSteps = false;

	public LarvaConfig() {
	}

	public String initScenarioDirectories(LarvaWriter out) {
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
		if (StringUtils.isEmpty(activeScenariosDirectory)) {
			String currentScenariosRootDirectory;
			String scenariosRootDefault = appConstants.getProperty("scenariosroot.default");
			if (scenariosRoots.containsKey(scenariosRootDefault)) {
				currentScenariosRootDirectory = scenariosRoots.get(scenariosRootDefault);
			} else if (!scenariosRoots.isEmpty()) {
				currentScenariosRootDirectory = scenariosRoots.get(scenariosRoots.firstKey());
			} else {
				currentScenariosRootDirectory = null;
			}
			activeScenariosDirectory = currentScenariosRootDirectory;
		}
		scenarioDirectories = scenariosRoots;
		return activeScenariosDirectory;
	}

	public List<File> readScenarioFiles(LarvaWriter out) {
		this.scenarioFiles = readScenarioFiles(out, activeScenariosDirectory);
		return this.scenarioFiles.keySet().stream().toList();
	}

	public Map<File, String> readScenarioFiles(LarvaWriter out, String scenariosDirectory) {
		Map <File, String> scenarioFiles = new LinkedHashMap<>();
		out.debugMessage("List all files in directory '" + scenariosDirectory + "'");

		File directory = new File(scenariosDirectory);
		Path targetPath = directory.toPath().normalize();

		if (!directory.toPath().normalize().startsWith(targetPath)) {
			String message = "Scenarios directory is outside of the target directory";
			log.warn(message);
			out.errorMessage(message);

			return scenarioFiles;
		}

		File[] files = directory.listFiles();
		if (files == null) {
			out.debugMessage("Could not read files from directory '" + scenariosDirectory + "'");
			return scenarioFiles;
		}
		out.debugMessage("Sort files");
		Arrays.sort(files);
		out.debugMessage("Filter out property files containing a 'scenario.description' property");
		for (File file : files) {
			if (file.isFile() && file.getName().endsWith(".properties") && !file.getName().equalsIgnoreCase("common.properties")) {
				Properties properties = LarvaUtil.readProperties(out, file);
				Object description = properties.get("scenario.description");
				if (description == null) {
					continue;
				}
				String active = properties.getProperty("scenario.active", "true");
				String unstable = properties.getProperty("adapter.unstable", "false");
				if ("true".equalsIgnoreCase(active) && "false".equalsIgnoreCase(unstable)) {
					scenarioFiles.put(file, description.toString());
				}
			} else if (file.isDirectory() && (!file.getName().startsWith(".") && !"CVS".equals(file.getName()))) {
				scenarioFiles.putAll(readScenarioFiles(out, file.getAbsolutePath()));
			}
		}
		out.debugMessage(scenarioFiles.size() + " scenario files found");
		return scenarioFiles;
	}
}
