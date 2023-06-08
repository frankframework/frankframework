package nl.nn.adapterframework.testutil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.common.util.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.ClassLoaderUtils;
import nl.nn.adapterframework.util.LogUtil;

public class PropertyUtil {
	protected static Logger log = LogUtil.getLogger(PropertyUtil.class);

	public static Map<String,Properties> propertiesMap = new HashMap<>();

	public static String getProperty(String propertyFile, String property) {
		Properties properties = propertiesMap.get(propertyFile);
		if (properties == null) {
			properties = new Properties();
			try {
				properties.load(ClassLoaderUtils.getResourceURL(propertyFile).openStream());
			} catch (IOException e) {
				log.warn("Could not load property file ["+propertyFile+"]",e);
			}
			propertiesMap.put(propertyFile, properties);
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
