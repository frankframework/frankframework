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
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.io.FilenameUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ClassNameRewriter;
import org.frankframework.larva.actions.LarvaActionFactory;
import org.frankframework.util.AppConstants;
import org.frankframework.util.PropertyLoader;

/**
 * Load scenario data for a given scenario file.
 * <p>
 * Scenario files can include other files. Included files are cached
 * per instance of the ScenarioLoader.
 * </p>
 */
@Log4j2
public class ScenarioLoader {
	private static final String LEGACY_PACKAGE_NAME_LARVA = "org.frankframework.testtool.";
	private static final String CURRENT_PACKAGE_NAME_LARVA = "org.frankframework.larva.";
	public static final int SCENARIO_CACHE_SIZE = 5;

	private final Map<File, PropertyLoader> scenarioFileCache = new LRUMap<>(SCENARIO_CACHE_SIZE);

	private final LarvaTool larvaTool;

	public ScenarioLoader(LarvaTool larvaTool) {
		this.larvaTool = larvaTool;
	}


	public @Nonnull Map<Scenario.ID, Scenario> readScenarioFiles(@Nonnull String scenariosDirectory) {
		long start = System.currentTimeMillis();
		try {
			// Suffix with file-separator for getting the right length for stripping down scenario names.
			String dirNameToUse = scenariosDirectory.endsWith(File.separator) ? scenariosDirectory : scenariosDirectory + File.separator;

			return readScenarioFiles(dirNameToUse, dirNameToUse, AppConstants.getInstance());
		} finally {
			long end = System.currentTimeMillis();
			String duration = LarvaUtil.formatDuration(end - start);
			log.debug("Reading scenario files took {}", duration);
		}
	}

	private Map<Scenario.ID, Scenario> readScenarioFiles(String baseDirectory, String scenariosDirectory, AppConstants appConstants) {
		Map<Scenario.ID, Scenario> scenarioFiles = new LinkedHashMap<>();
		larvaTool.debugMessage("List all files in directory '" + scenariosDirectory + "'");

		File directory = new File(scenariosDirectory);
		File[] files = directory.listFiles();
		if (files == null) {
			larvaTool.debugMessage("Could not read files from directory '" + scenariosDirectory + "'");
			return scenarioFiles;
		}
		log.debug("Sort files");
		Arrays.sort(files);
		log.debug("Filter out property files containing a 'scenario.description' property");
		for (File scenarioFile : files) {
			if (scenarioFile.isFile() && scenarioFile.getName().endsWith(".properties") && !scenarioFile.getName().equalsIgnoreCase("common.properties")) {
				String scenarioFilePath = scenarioFile.getAbsolutePath();
				PropertyLoader properties;
				try {
					properties = readScenarioProperties(scenarioFile, appConstants);
				} catch (IOException e) {
					larvaTool.errorMessage("Could not read properties file [" + scenarioFilePath + "]: " + e.getMessage(), e);
					continue;
				}
				String description = properties.getProperty("scenario.description");
				if (description == null) {
					log.warn("Property file [" + scenarioFilePath + "] has no description");
				}
				boolean active = properties.getBoolean("scenario.active", true);
				boolean unstable = properties.getBoolean("adapter.unstable", false);
				if (active && !unstable && description != null) {
					String name = FilenameUtils.normalize(scenarioFilePath.substring(baseDirectory.length(), scenarioFilePath.length() - ".properties".length()), true);
					Scenario scenario = new Scenario(scenarioFile, name, description, properties);
					scenarioFiles.put(scenario.getId(), scenario);
				}
			} else if (scenarioFile.isDirectory() && (!scenarioFile.getName().startsWith(".") && !"CVS".equals(scenarioFile.getName()))) {
				scenarioFiles.putAll(readScenarioFiles(baseDirectory, scenarioFile.getAbsolutePath(), appConstants));
			}
		}
		larvaTool.debugMessage(scenarioFiles.size() + " scenario files found");
		return scenarioFiles;
	}

	/**
	 * Read the properties of a scenario file.
	 *
	 * @param scenarioFile The scenario file to read
	 * @param appConstants {@link AppConstants} to be used for resolving propertes in scenarios
	 * @return The properties read from the scenario file.
	 */
	public @Nonnull PropertyLoader readScenarioProperties(@Nonnull File scenarioFile, @Nonnull AppConstants appConstants) throws IOException {
		PropertyLoader scenarioProperties;
		scenarioProperties = readScenarioProperties(scenarioFile, appConstants, true);
		String scenarioDirectory = scenarioFile.getParentFile().getAbsolutePath();
		addAbsolutePathProperties(scenarioDirectory, scenarioProperties);
		return scenarioProperties;
	}

	private @Nonnull PropertyLoader readScenarioProperties(@Nonnull File scenarioFile, @Nullable AppConstants appConstants, boolean root) throws IOException {
		// Only cache included files since they are most likely to be frequently read. Root files would just pollute the cache.
		if (!root && scenarioFileCache.containsKey(scenarioFile)) {
			return scenarioFileCache.get(scenarioFile);
		}
		String scenarioDirectory = scenarioFile.getParentFile().getAbsolutePath();
		PropertyLoader properties = new PropertyLoader(scenarioFile, appConstants);
		fixLegacyClassnames(properties);

		Properties includedProperties = getIncludedProperties(properties, scenarioDirectory);
		properties.putAll(includedProperties);
		log.debug("{} properties found", properties.size());
		if (!root) {
			// Only cache included files since they are most likely to be frequently read. Root files would just pollute the cache.
			scenarioFileCache.put(scenarioFile, properties);
		}
		return properties;
	}

	private @Nonnull Properties getIncludedProperties(Properties properties, String directory) throws IOException {
		Properties includedProperties = new Properties();
		int i = 0;
		String includeFilename = properties.getProperty("include");
		if (includeFilename == null) {
			i++;
			includeFilename = properties.getProperty("include" + i);
		}
		while (includeFilename != null) {
			log.debug("Load include file: [{}]", includeFilename);
			File includeFile = new File(LarvaUtil.getAbsolutePath(directory, includeFilename));
			Properties includeProperties = readScenarioProperties(includeFile, null, false);
			includedProperties.putAll(includeProperties);
			i++;
			includeFilename = properties.getProperty("include" + i);
		}
		return includedProperties;
	}

	private static void addAbsolutePathProperties(@Nonnull String propertiesDirectory, @Nonnull Properties properties) {
		for (Object o : properties.keySet()) {
			String property = (String) o;
			if ("configurations.directory".equalsIgnoreCase(property))
				continue;

			if (property.endsWith(".read") || property.endsWith(".write")
					|| property.endsWith(".directory")
					|| property.endsWith(".filename")
					|| property.endsWith(".valuefile")
					|| property.endsWith(".valuefileinputstream")) {
				String absolutePathProperty = property + ".absolutepath";
				String value = LarvaUtil.getAbsolutePath(propertiesDirectory, (String) properties.get(property));
				if (value != null) {
					properties.put(absolutePathProperty, value);
				}
			}
		}
	}

	private static void fixLegacyClassnames(@Nonnull Properties properties) {
		Map<Object, Object> collected = properties.entrySet().stream()
				.map(ScenarioLoader::rewriteClassName)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		properties.putAll(collected);
	}


	private static Map.Entry<Object, Object> rewriteClassName(Map.Entry<Object, Object> e) {
		Object propertyName = e.getKey();
		if (e.getValue() == null || !propertyName.toString().endsWith(LarvaActionFactory.CLASS_NAME_PROPERTY_SUFFIX)) {
			return e;
		}
		String newClassName = e.getValue()
				.toString()
				.replace(ClassNameRewriter.LEGACY_PACKAGE_NAME, ClassNameRewriter.ORG_FRANKFRAMEWORK_PACKAGE_NAME)
				.replace(LEGACY_PACKAGE_NAME_LARVA, CURRENT_PACKAGE_NAME_LARVA);
		return Map.entry(propertyName, newClassName);
	}
}
