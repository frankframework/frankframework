/*
   Copyright 2023 WeAreFrank!

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

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

public class YamlParser {

	private final Properties properties;

	/**
	 * Loads and parses a yaml file.
	 * Uses the {@link #handleRawValue(String, Object)} method to recursively loop over all values.
	 */
	public YamlParser() {
		this.properties = new Properties();
	}

	public Properties load(Reader reader) {
		Yaml yaml = new Yaml();

		Map<String, Object> obj = yaml.loadAs(reader, Map.class);
		obj.entrySet().forEach(entry -> handleRawValue(entry.getKey(), entry.getValue()));

		return properties;
	}

	/**
	 * Recursively traverses the object; When string is found will put it in the properties.
	 *
	 * @param key   Key of the property
	 * @param value Value of the property
	 */
	@SuppressWarnings("unchecked")
	private void handleRawValue(String key, Object value) {
		// If the value is a map, will recursively call the method.
		// Key will be added to the 'chain'.
		if (value instanceof Map) {
			handleMapValue(key, (Map<String, Object>) value);
		}

		// Due to how the parser works, ArrayList may encapsulate a map.
		// Key doesn't need to be updated
		else if (value instanceof ArrayList list) {
			handleListValue(key, list);
		}

		// Threat as a single value and store it in the properties.
		else {
			if (value != null) {
				String valueToString = value.toString(); // String / Integer
				properties.setProperty(key, valueToString);
			}
		}
	}

	/**
	 * This translates an object-array to multiple properties using the name value as key.
	 * <p>
	 * Example:
	 * <pre>
	 *     array:
	 *       - name: Sergi
	 *         value: 100
	 *       - name: Niels
	 *         value: 50
	 *     </pre>
	 * Will result in the properties: <br/>
	 *     <ul>
	 *       <li>array.Sergi.value = 100</li>
	 *       <li>array.Niels.value = 50</li>
	 *     </ul>
	 * </p>
	 */
	private void handleMapValue(final String key, Map<String, Object> map) {
		String name = (String) map.remove("name");
		String updatedKey = StringUtils.isNotEmpty(name) ? (key + "." + name) : key;

		map.entrySet().forEach(entry -> handleRawValue(updatedKey + "." + entry.getKey(), entry.getValue()));
	}

	/**
	 * Not all entries have to be of the same type.
	 * <p>
	 * Collect non-map entries and add those as a 'list' property
	 * Delegate the map to {@link #handleMapValue(String, Map)}.
	 */
	@SuppressWarnings("unchecked")
	private void handleListValue(String key, ArrayList<Object> list) {
		List<String> listProperty = new ArrayList<>();
		for (Object object : list) {
			if (object instanceof Map) {
				handleMapValue(key, (Map<String, Object>) object);
			} else {
				listProperty.add(object.toString()); // String or Integer
			}
		}
		if (!listProperty.isEmpty()) {
			properties.setProperty(key, listProperty.stream().collect(Collectors.joining(",")));
		}
	}
}
