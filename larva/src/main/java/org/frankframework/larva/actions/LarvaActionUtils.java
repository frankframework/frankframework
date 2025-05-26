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

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.FileMessage;
import org.frankframework.util.DomBuilderException;
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
public class LarvaActionUtils {

	private LarvaActionUtils() {
		// don't construct util class
	}

	public static Properties getSubProperties(Properties properties, String keyBase) {
		if(!keyBase.endsWith("."))
			keyBase +=".";

		Properties filteredProperties = new Properties();
		for(Object objKey: properties.keySet()) {
			String key = (String) objKey;
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
	 * @param property   Property name to use as base name
	 * @return A map with parameters
	 */
	public static Map<String, IParameter> createParametersMapFromParamProperties(Properties properties, PipeLineSession session) {
		final String _name = ".name";
		final String _param = "param";
		final String _type = ".type";
		Map<String, IParameter> result = new HashMap<>();
		boolean processed = false;
		int i = 1;
		while (!processed) {
			String name = properties.getProperty(_param + i + _name);
			if (name != null) {
				String type = properties.getProperty(_param + i + _type);
				String propertyValue = properties.getProperty(_param + i + ".value");
				Object value = propertyValue;

				if (value == null) {
					String filename = properties.getProperty(_param + i + ".valuefile.absolutepath");
					if (filename != null) {
						value = new FileMessage(new File(filename));
					} else {
						throw new IllegalStateException("use either value or valuefile");
					}
				}
				if ("node".equals(type)) {
					try {
						value = XmlUtils.buildNode(MessageUtils.asString(value), true);
					} catch (DomBuilderException | IOException e) {
						throw new IllegalStateException("Could not build node for parameter '" + name + "' with value: " + value, e);
					}
				} else if ("domdoc".equals(type)) {
					try {
						value = XmlUtils.buildDomDocument(MessageUtils.asString(value), true);
					} catch (DomBuilderException | IOException e) {
						throw new IllegalStateException("Could not build node for parameter '" + name + "' with value: " + value, e);
					}
				} else if ("list".equals(type)) {
					value = StringUtil.split(propertyValue);
				} else if ("map".equals(type)) {
					List<String> parts = StringUtil.split(propertyValue);
					Map<String, String> map = new LinkedHashMap<>();
					for (String part : parts) {
						String[] splitted = part.split("\\s*(=\\s*)+", 2);
						if (splitted.length == 2) {
							map.put(splitted[0], splitted[1]);
						} else {
							map.put(splitted[0], "");
						}
					}
					value = map;
				}
				String pattern = properties.getProperty(_param + i + ".pattern");
				if (value == null && pattern == null) {
					throw new IllegalStateException("Property '" + _param + i + " doesn't have a value or pattern");
				} else {
					try {
						Parameter parameter = new Parameter();
						parameter.setName(name);

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
						throw new IllegalStateException("Parameter '" + name + "' could not be configured");
					}
				}
				i++;
			} else {
				processed = true;
			}
		}
		return result;
	}
}
