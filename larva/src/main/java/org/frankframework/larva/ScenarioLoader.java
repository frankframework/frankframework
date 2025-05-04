package org.frankframework.larva;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.collections4.map.LRUMap;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ClassNameRewriter;
import org.frankframework.larva.actions.LarvaActionFactory;
import org.frankframework.larva.output.LarvaWriter;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StringResolver;

@Log4j2
public class ScenarioLoader {
	private static final String LEGACY_PACKAGE_NAME_LARVA = "org.frankframework.testtool.";
	private static final String CURRENT_PACKAGE_NAME_LARVA = "org.frankframework.larva.";

	private final Map<File, Properties> scenarioFileCache = new LRUMap<>(20);

	private final LarvaWriter out;

	public ScenarioLoader(LarvaWriter out) {
		this.out = out;
	}


	public Map<File, String> readScenarioFiles(String scenariosDirectory) {
		return readScenarioFiles(scenariosDirectory, AppConstants.getInstance());
	}

	public Map<File, String> readScenarioFiles(String scenariosDirectory, AppConstants appConstants) {
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
				Properties properties = readScenarioProperties(file, appConstants);
				applyStringSubstitutions(properties, appConstants);
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
				scenarioFiles.putAll(readScenarioFiles(file.getAbsolutePath(), appConstants));
			}
		}
		out.debugMessage(scenarioFiles.size() + " scenario files found");
		return scenarioFiles;
	}

	public @Nullable Properties readScenarioProperties(@Nonnull File propertyFile, @Nonnull AppConstants appConstants) {
		return readScenarioProperties(propertyFile, appConstants, true);
	}

	public @Nullable Properties readScenarioProperties(@Nonnull File propertiesFile, @Nullable AppConstants appConstants, boolean root) {
		if (scenarioFileCache.containsKey(propertiesFile)) {
			return scenarioFileCache.get(propertiesFile);
		}
		String directory = new File(propertiesFile.getAbsolutePath()).getParent();
		try {
			Properties properties = LarvaUtil.readProperties(out, propertiesFile);
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
				scenarioFileCache.put(propertiesFile, properties);
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

	@Nullable
	private static Properties fixLegacyClassnames(@Nullable Properties properties) {
		if (properties == null) {
			return null;
		}
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
