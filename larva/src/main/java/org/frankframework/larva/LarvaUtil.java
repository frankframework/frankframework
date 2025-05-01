package org.frankframework.larva;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ClassNameRewriter;
import org.frankframework.larva.actions.LarvaActionFactory;
import org.frankframework.larva.output.LarvaWriter;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringResolver;

@Log4j2
public class LarvaUtil {
	private static final String LEGACY_PACKAGE_NAME_LARVA = "org.frankframework.testtool.";
	private static final String CURRENT_PACKAGE_NAME_LARVA = "org.frankframework.larva.";

	private LarvaUtil() {
		// Private constructor to prevent creating instances
	}

	public static String getAbsolutePath(String parent, String child) {
		return getAbsolutePath(parent, child, false);
	}

	/**
	 * Returns the absolute pathname for the child pathname. The parent pathname
	 * is used as a prefix when the child pathname is an not absolute.
	 *
	 * @param parent  the parent pathname to use
	 * @param child   the child pathname to convert to a absolute pathname
	 */
	public static String getAbsolutePath(String parent, String child,
										 boolean addFileSeparator) {
		File result;
		File file = new File(child);
		if (file.isAbsolute()) {
			result = file;
		} else {
			result = new File(parent, child);
		}
		String absPath = FilenameUtils.normalize(result.getAbsolutePath());
		if (addFileSeparator) {
			return absPath + File.separator;
		} else {
			return absPath;
		}
	}

	public static @Nullable Properties readScenarioProperties(@Nonnull LarvaWriter out, @Nonnull File propertyFile, @Nonnull AppConstants appConstants) {
		return readScenarioProperties(out, propertyFile, appConstants, true);
	}

	public static @Nullable Properties readScenarioProperties(@Nonnull LarvaWriter out, @Nonnull File propertiesFile, @Nullable AppConstants appConstants, boolean root) {
		String directory = new File(propertiesFile.getAbsolutePath()).getParent();
		Properties properties = new Properties();
		try {
			try(FileInputStream fis = new FileInputStream(propertiesFile); Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(fis)) {
				properties.load(reader);
			}

			Properties includedProperties = new Properties();
			int i = 0;
			String includeFilename = properties.getProperty("include");
			if (includeFilename == null) {
				i++;
				includeFilename = properties.getProperty("include" + i);
			}
			while (includeFilename != null) {
				out.debugMessage("Load include file: " + includeFilename);
				File includeFile = new File(getAbsolutePath(directory, includeFilename));
				Properties includeProperties = readScenarioProperties(out, includeFile, appConstants, false);
				if (includeProperties != null) {
					includedProperties.putAll(includeProperties);
				}
				i++;
				includeFilename = properties.getProperty("include" + i);
			}
			properties.putAll(includedProperties);
			if (root) {
				properties.putAll(appConstants);
				for (Map.Entry<Object, Object> entry: properties.entrySet()) {
					properties.put(entry.getKey(), StringResolver.substVars((String)properties.get(entry.getValue()), properties));
				}
				addAbsolutePathProperties(directory, properties);
			}
			out.debugMessage(properties.size() + " properties found");
		} catch(Exception e) {
			properties = null;
			out.errorMessage("Could not read properties file: " + e.getMessage(), e);
		}
		return fixLegacyClassnames(properties);
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
				String value = getAbsolutePath(propertiesDirectory, (String) properties.get(property));
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
				.map(LarvaUtil::rewriteClassName)
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

	public static String formatDuration(long durationInMs) {
		Duration duration = Duration.ofMillis(durationInMs);
		if (duration.toMinutesPart() == 0 && duration.toSecondsPart() == 0) {
			// Only milliseconds (e.g. 123ms)
			return duration.toMillisPart() + "ms";
		} else if (duration.toMinutesPart() == 0) {
			// Seconds and milliseconds (e.g. 1s 123ms)
			return duration.toSecondsPart() + "s " + duration.toMillisPart() + "ms";
		} else {
			// Minutes, seconds and milliseconds (e.g. 1m 1s 123ms)
			return duration.toMinutesPart() + "m " + duration.toSecondsPart() + "s " + duration.toMillisPart() + "ms";
		}
	}

	public static @Nullable String getParentOfWebappRoot() {
		URL rootResource = LarvaUtil.class.getResource("/");
		if (rootResource == null) {
			return null;
		}
		String realPath = rootResource.getPath();
		return new File(realPath).getParent();
	}

	protected static @Nonnull Properties readProperties(LarvaWriter out, File propertyFile) {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(propertyFile));
		} catch (IOException e) {
			out.errorMessage("Cannot read property file: " + propertyFile.getAbsolutePath(), e);
		}
		return properties;
	}
}
