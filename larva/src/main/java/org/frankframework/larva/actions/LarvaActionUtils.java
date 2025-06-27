/*
   Copyright 2022-2025 WeAreFrank!

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
package org.frankframework.larva.actions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.annotation.Nullable;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterType;
import org.frankframework.stream.FileMessage;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlUtils;

/**
 * Reflection helper to create Larva Actions
 *
 * When a class is created it will attempt to set the name and disable HTTP SSL capabilities by default
 * When setting the bean properties it loops through the available setter methods and looks for a matching property.
 *
 * @author Niels Meijer
 */
@Log4j2
public class LarvaActionUtils {

	public static final String NAME_KEY = ".name";
	public static final String PARAM_KEY = "param";
	public static final String TYPE_KEY = ".type";
	public static final String PATTERN_KEY = ".pattern";
	public static final String VALUE_KEY = ".value";
	public static final String VALUEFILE_ABSOLUTEPATH_KEY = ".valuefile.absolutepath";

	private LarvaActionUtils() {
		// don't construct util class
	}

	public static Properties getSubProperties(Properties properties, String keyBase) {
		if(!keyBase.endsWith("."))
			keyBase +=".";

		Properties filteredProperties = new Properties();
		for(String key: properties.stringPropertyNames()) {
			if(key.startsWith(keyBase)) {
				filteredProperties.put(key.substring(keyBase.length()), properties.get(key));
			}
		}

		return filteredProperties;
	}

	/**
	 * Create a Map for a specific property based on other properties that are
	 * the same except for a .param1.name, .param1.value or .param1.valuefile
	 * suffix.  The property with the .name suffix specifies the key for the
	 * Map, the property with the value suffix specifies the value for the Map.
	 * A property with a the .valuefile suffix can be used as an alternative
	 * for a property with a .value suffix to specify the file to read the
	 * value for the Map from. More than one param can be specified by using
	 * param2, param3 etc.
	 *
	 * @param properties Properties object from which to create the map
	 * @param session   PipeLineSession from which to resolve parameter values
	 * @return A map with parameters
	 */
	public static Map<String, IParameter> createParametersMapFromParamProperties(Properties properties, PipeLineSession session) {
		Map<String, IParameter> result = new HashMap<>();
		int i = 1;
		while (true) {
			String name = properties.getProperty(PARAM_KEY + i + NAME_KEY);
			if (name == null) {
				break;
			}
			String type = properties.getProperty(PARAM_KEY + i + TYPE_KEY);
			Object value = getParamValue(properties, i, type, name);
			String pattern = properties.getProperty(PARAM_KEY + i + PATTERN_KEY);
			if (value == null && pattern == null) {
				throw new IllegalArgumentException("Property '" + PARAM_KEY + i + " doesn't have a value or pattern");
			} else {
				try {
					Parameter parameter = new Parameter();
					parameter.setName(name);
					parameter.setType(EnumUtils.parse(ParameterType.class, type));
					if (value != null) {
						if (value instanceof String string) {
							parameter.setValue(string);
							parameter.setPattern(pattern);
						} else {
							parameter.setSessionKey(name);
							session.put(name, value);
						}
					}

					parameter.configure();
					result.put(name, parameter);
				} catch (ConfigurationException e) {
					throw new IllegalArgumentException("Parameter '" + name + "' could not be configured");
				}
			}
			i++;
		}
		return result;
	}

	/**
	 * Create a Map for a specific property based on other properties that are
	 * the same except for a .param1.name, .param1.value or .param1.valuefile
	 * suffix.  The property with the .name suffix specifies the key for the
	 * Map, the property with the value suffix specifies the value for the Map.
	 * A property with a the .valuefile suffix can be used as an alternative
	 * for a property with a .value suffix to specify the file to read the
	 * value for the Map from. More than one param can be specified by using
	 * param2, param3 etc.
	 *
	 * @param properties Properties from which to extract the parameters
	 * @return A map with parameters
	 */
	// Merge this with LarvaActionUtils#createParametersMapFromParamProperties(Properties, PipeLineSession)
	public static Map<String, Object> createParametersMapFromParamProperties(Properties properties) {
		Map<String, Object> result = new HashMap<>();
		int i = 1;
		while (true) {
			String name = properties.getProperty(PARAM_KEY + i + NAME_KEY);
			if (name == null) {
				break;
			}
			String type = properties.getProperty(PARAM_KEY + i + TYPE_KEY);
			Object value = getParamValue(properties, i, type, name);
			if (value == null) {
				throw new IllegalArgumentException("Property '" + PARAM_KEY + i + ".value' or '" + PARAM_KEY + i + ".valuefile' not found while property '" + PARAM_KEY + i + ".name' exist");
			} else {
				result.put(name, value);
				log.debug("Add param with name [{}] and value [{}] for property '" + "'", name, value);
			}
			i++;
		}
		return result;
	}

	@Nullable
	private static Object getParamValue(Properties properties, int i, String type, String name) {
		String propertyValue = properties.getProperty(PARAM_KEY + i + VALUE_KEY);
		Object value = propertyValue;

		if (value == null) {
			String filename = properties.getProperty(PARAM_KEY + i + VALUEFILE_ABSOLUTEPATH_KEY);
			if (filename != null) {
				value = new FileMessage(new File(filename));
			} else {
				String inputStreamFilename = properties.getProperty(PARAM_KEY + i + ".valuefileinputstream.absolutepath");
				if (inputStreamFilename != null) {
					throw new IllegalArgumentException("'valuefileinputstream' is no longer supported, use 'valuefile' instead");
				}
			}
		}
		if ("node".equalsIgnoreCase(type)) {
			try {
				value = XmlUtils.buildNode(MessageUtils.asString(value), true);
			} catch (DomBuilderException | IOException e) {
				throw new IllegalArgumentException("Could not build node for parameter '" + name + "' with value: " + value, e);
			}
		} else if ("domdoc".equalsIgnoreCase(type)) {
			try {
				value = XmlUtils.buildDomDocument(MessageUtils.asString(value), true);
			} catch (DomBuilderException | IOException e) {
				throw new IllegalArgumentException("Could not build node for parameter '" + name + "' with value: " + value, e);
			}
		} else if ("list".equalsIgnoreCase(type)) {
			value = StringUtil.split(propertyValue);
		} else if ("map".equalsIgnoreCase(type)) {
			List<String> parts = StringUtil.split(propertyValue);
			Map<String, String> map = new LinkedHashMap<>();

			for (String part : parts) {
				String[] splitted = part.split("=", 2);
				if (splitted.length==2) {
					map.put(splitted[0].strip(), splitted[1].strip());
				} else {
					map.put(splitted[0].strip(), "");
				}
			}
			value = map;
		}
		return value;
	}
}
