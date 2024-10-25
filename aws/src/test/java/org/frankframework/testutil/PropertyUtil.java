package org.frankframework.testutil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.ClassLoaderUtils;

@Log4j2
public class PropertyUtil {

	public static Map<String,Properties> propertiesMap = new HashMap<>();

	public static String getProperty(String propertyFile, String property) {
		Properties properties = propertiesMap.get(propertyFile);
		if (properties == null) {
			properties = new Properties();
			try {
				properties.load(ClassLoaderUtils.getResourceURL(propertyFile).openStream());
			} catch (IOException e) {
				log.warn("Could not load property file [{}]", propertyFile, e);
			}
			propertiesMap.put(propertyFile, properties);
		}
		String envValue = System.getenv(property);
		if(StringUtils.isNotEmpty(envValue)) {
			return envValue;
		}
		return properties.getProperty(property);
	}

	public static String getProperty(String propertyFile, String property, String defaultValue) {
		String result = getProperty(propertyFile, property);
		if (StringUtils.isEmpty(result)) {
			return defaultValue;
		}
		return result;
	}

	public static boolean getProperty(String propertyFile, String property, boolean defaultValue) {
		String result = getProperty(propertyFile, property);
		if (StringUtils.isEmpty(result)) {
			return defaultValue;
		}
		return Boolean.parseBoolean(result);
	}

	public static int getProperty(String propertyFile, String property, int defaultValue) {
		String result = getProperty(propertyFile, property);
		if (StringUtils.isEmpty(result)) {
			return defaultValue;
		}
		return Integer.parseInt(result);
	}

}
