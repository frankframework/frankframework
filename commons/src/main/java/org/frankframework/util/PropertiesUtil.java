/*
   Copyright 2023-2026 WeAreFrank!

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
package org.frankframework.util;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;

import lombok.experimental.UtilityClass;

@NullMarked
@UtilityClass
public class PropertiesUtil {

	public static Properties propertiesOf(String... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException("keyValuePairs must have an even number of elements");
		}
		final Properties properties = new Properties();
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			properties.setProperty(keyValuePairs[i], keyValuePairs[i + 1]);
		}
		return properties;
	}

	/**
	 * Get a new {@link Properties} instance that contains a subset of the properties in the original instance, filtered
	 * by the keyBase prefix, and with that prefix removed.
	 *
	 * @param properties Original {@link Properties} to filter
	 * @param keyBase Base property name on which to filter. Should end with a {@code .} and if it doesn't, a {@code .} is appended.
	 * @return New {@link Properties} instance with only the filtered properties, the {@code keyBase} prefix removed.
	 */
	public static Properties getSubProperties(Properties properties, String keyBase) {
		final String propertyPrefix = keyBase + (!keyBase.endsWith(".") ? "." : "");

		Properties filteredProperties = new Properties();
		Map<String, String> tempMap = properties.stringPropertyNames().stream()
				.filter(key -> key.startsWith(propertyPrefix))
				.collect(Collectors.toMap(key -> key.substring(propertyPrefix.length()), properties::getProperty));
		filteredProperties.putAll(tempMap);
		return filteredProperties;
	}
}
