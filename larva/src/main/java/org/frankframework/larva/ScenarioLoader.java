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
import org.frankframework.larva.output.LarvaWriter;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StringResolver;

/**
 * Load scenario data for a given scenario file.
 * <p>
 *     Scenario files can include other files. Included files are cached
 *     per instance of the ScenarioLoader.
 * </p>
 */
@Log4j2
public class ScenarioLoader {
	private static final String LEGACY_PACKAGE_NAME_LARVA = "org.frankframework.testtool.";
	private static final String CURRENT_PACKAGE_NAME_LARVA = "org.frankframework.larva.";
	public static final int SCENARIO_CACHE_SIZE = 20;

	private final Map<File, Properties> scenarioFileCache = new LRUMap<>(SCENARIO_CACHE_SIZE);

	private final LarvaWriter out;

	public ScenarioLoader(LarvaWriter out) {
		this.out = out;
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
			out.debugMessage("Reading scenario files took " + duration);
		}
	}

	private Map<Scenario.ID, Scenario> readScenarioFiles(String baseDirectory, String scenariosDirectory, AppConstants appConstants) {
		Map <Scenario.ID, Scenario> scenarioFiles = new LinkedHashMap<>();
		out.debugMessage("List all files in directory '" + scenariosDirectory + "'");

		File directory = new File(scenariosDirectory);
		File[] files = directory.listFiles();
		if (files == null) {
			out.debugMessage("Could not read files from directory '" + scenariosDirectory + "'");
			return scenarioFiles;
		}
		out.debugMessage("Sort files");
		Arrays.sort(files);
		out.debugMessage("Filter out property files containing a 'scenario.description' property");
		for (File scenarioFile : files) {
			if (scenarioFile.isFile() && scenarioFile.getName().endsWith(".properties") && !scenarioFile.getName().equalsIgnoreCase("common.properties")) {
				Properties properties = readScenarioProperties(scenarioFile, appConstants);
				String scenarioFilePath = scenarioFile.getAbsolutePath();
				if (properties == null || properties.getProperty("scenario.description") == null) {
					log.warn("Could not read properties file: [{}]", scenarioFilePath);
					continue;
				}
				String description = properties.getProperty("scenario.description");
				String active = properties.getProperty("scenario.active", "true");
				String unstable = properties.getProperty("adapter.unstable", "false");
				if ("true".equalsIgnoreCase(active) && "false".equalsIgnoreCase(unstable)) {
					String name = FilenameUtils.normalize(scenarioFilePath.substring(baseDirectory.length(), scenarioFilePath.length() - ".properties".length()), true);
					Scenario scenario = new Scenario(scenarioFile, name, description);
					scenarioFiles.put(scenario.getId(), scenario);
				}
			} else if (scenarioFile.isDirectory() && (!scenarioFile.getName().startsWith(".") && !"CVS".equals(scenarioFile.getName()))) {
				scenarioFiles.putAll(readScenarioFiles(baseDirectory, scenarioFile.getAbsolutePath(), appConstants));
			}
		}
		out.debugMessage(scenarioFiles.size() + " scenario files found");
		return scenarioFiles;
	}

	/**
	 * Read the properties of a scenario file.
	 * @param scenarioFile The scenario file to read
	 * @param appConstants {@link AppConstants} to be used for resolving propertes in scenarios
	 * @return The properties read from the scenario file.
	 */
	public @Nullable Properties readScenarioProperties(@Nonnull File scenarioFile, @Nonnull AppConstants appConstants) {
		return readScenarioProperties(scenarioFile, appConstants, true);
	}

	private @Nullable Properties readScenarioProperties(@Nonnull File scenarioFile, @Nullable AppConstants appConstants, boolean root) {
		if (scenarioFileCache.containsKey(scenarioFile)) {
			return scenarioFileCache.get(scenarioFile);
		}
		String directory = scenarioFile.getParentFile().getAbsolutePath();
		try {
			Properties properties = LarvaUtil.readProperties(out, scenarioFile);
			Properties includedProperties = new Properties();
			int i = 0;
			String includeFilename = properties.getProperty("include");
			if (includeFilename == null) {
				i++;
				includeFilename = properties.getProperty("include" + i);
			}
			while (includeFilename != null) {
				out.debugMessage("Load include file: " + includeFilename);
				File includeFile = new File(LarvaUtil.getAbsolutePath(directory, includeFilename));
				Properties includeProperties = readScenarioProperties(includeFile, appConstants, false);
				if (includeProperties != null) {
					includedProperties.putAll(includeProperties);
				}
				i++;
				includeFilename = properties.getProperty("include" + i);
			}
			properties.putAll(includedProperties);
			out.debugMessage(properties.size() + " properties found");
			if (root) {
				properties.putAll(appConstants);
				applyStringSubstitutions(properties, appConstants);
				addAbsolutePathProperties(directory, properties);
				return fixLegacyClassnames(properties);
			} else {
				// Only cache included files since they are most likely to be frequently read. Root files would just pollute the cache.
				scenarioFileCache.put(scenarioFile, properties);
				return properties;
			}
		} catch(Exception e) {
			out.errorMessage("Could not read properties file: " + e.getMessage(), e);
			return null;
		}
	}

	public static void applyStringSubstitutions(Properties properties, AppConstants appConstants) {
		for (Map.Entry<Object, Object> entry: properties.entrySet()) {
			properties.put(entry.getKey(), StringResolver.substVars((String)entry.getValue(), properties, appConstants));
		}
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

	private static @Nonnull Properties fixLegacyClassnames(@Nonnull Properties properties) {
		Map<Object, Object> collected = properties.entrySet().stream()
				.map(ScenarioLoader::rewriteClassName)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		Properties result = new Properties();
		result.putAll(collected);
		return result;
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
