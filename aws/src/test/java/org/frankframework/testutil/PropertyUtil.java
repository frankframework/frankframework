package org.frankframework.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.ClassLoaderUtils;

@Log4j2
@NullMarked
public class PropertyUtil {

	public static final Map<String,Properties> propertiesMap = new HashMap<>();

	/**
	 * Get property from the environment, or if not found in the environment, then from the given propertyFile.
	 * The propertyFile should be on the classpath. If the propertyFile cannot be loaded, it will be treated as if it was empty.
	 * The propertyFile is cached for efficient future reference until the server restarts, values cannot be reloaded if the property file changes.
	 * There will be no string interpolation or property expansion on values in the properties file.
	 * If the property cannot be found in the environment nor in the propertyFile, then a {@code NULL} value will be returned.
	 */
	public static @Nullable String getProperty(String propertyFile, String property) {
		Properties properties = propertiesMap.get(propertyFile);
		if (properties == null) {
			properties = new Properties();
			URL resourceURL = ClassLoaderUtils.getResourceURL(propertyFile);
			if  (resourceURL != null) {
				try (InputStream inStream = resourceURL.openStream()) {
					properties.load(inStream);
				} catch (IOException | NullPointerException e) {
					log.warn("Could not load property file [{}]", propertyFile, e);
				}
			} else {
				log.warn("Property file [{}] not found on the classpath", propertyFile);
			}
			propertiesMap.put(propertyFile, properties);
		}
		String envValue = System.getenv(property);
		if(StringUtils.isNotBlank(envValue)) {
			return envValue;
		}
		return properties.getProperty(property);
	}

	/**
	 * Get property from the environment, or if not found in the environment, then from the given propertyFile.
	 * The propertyFile should be on the classpath. If the propertyFile cannot be loaded, it will be treated as if it was empty.
	 * The propertyFile is cached for efficient future reference until the server restarts, values cannot be reloaded if the property file changes.
	 * There will be no string interpolation or property expansion on values in the properties file.
	 * If the property cannot be found in the environment nor in the propertyFile, then the defaultValue value will be returned.
	 */
	public static @Nullable String getProperty(String propertyFile, String property, @Nullable String defaultValue) {
		String result = getProperty(propertyFile, property);
		if (StringUtils.isBlank(result)) {
			return defaultValue;
		}
		return result;
	}

	/**
	 * Get a boolean property from the environment, or if not found in the environment, then from the given propertyFile.
	 * The propertyFile should be on the classpath. If the propertyFile cannot be loaded, it will be treated as if it was empty.
	 * The propertyFile is cached for efficient future reference until the server restarts, values cannot be reloaded if the property file changes.
	 * There will be no string interpolation or property expansion on values in the properties file.
	 * If the property cannot be found in the environment nor in the propertyFile, then the defaultValue value will be returned.
	 */	public static boolean getProperty(String propertyFile, String property, boolean defaultValue) {
		String result = getProperty(propertyFile, property);
		if (StringUtils.isBlank(result)) {
			return defaultValue;
		}
		return Boolean.parseBoolean(result.trim());
	}

	/**
	 * Get an integer property from the environment, or if not found in the environment, then from the given propertyFile.
	 * The propertyFile should be on the classpath. If the propertyFile cannot be loaded, it will be treated as if it was empty.
	 * The propertyFile is cached for efficient future reference until the server restarts, values cannot be reloaded if the property file changes.
	 * There will be no string interpolation or property expansion on values in the properties file.
	 * If the property cannot be found in the environment nor in the propertyFile, then the defaultValue value will be returned.
	 */	public static int getProperty(String propertyFile, String property, int defaultValue) {
		String result = getProperty(propertyFile, property);
		if (StringUtils.isBlank(result)) {
			return defaultValue;
		}
		return Integer.parseInt(result.trim());
	}
}
