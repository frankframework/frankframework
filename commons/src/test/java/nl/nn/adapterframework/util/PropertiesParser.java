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
package nl.nn.adapterframework.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class PropertiesParser {

	public static String parseFile(Reader reader) {

		Map<String, Object> accumulation = new LinkedHashMap<>();

		try (BufferedReader bufferedReader = new BufferedReader(reader)) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.startsWith("#") || line.isEmpty()) {
					continue;
				}
				String[] keys = line.split("\\.(?=[^=]*=)");
				addPropertyToMap(accumulation, keys);
			}
		} catch (IOException exception) {
			exception.printStackTrace();
		}

		Yaml yaml = new Yaml();
		return yaml.dump(accumulation);
	}

	static void addPropertyToMap(Map<String, Object> propertiesMap, String[] propertyParts) {
		String currentKey = propertyParts[0];
		String[] restOfProperty = Arrays.copyOfRange(propertyParts, 1, propertyParts.length);

		if (propertiesMap.containsKey(currentKey)) {
			Object currentValue = propertiesMap.get(currentKey);
			if (currentValue instanceof Map) {
				addPropertyToMap((Map<String, Object>) currentValue, restOfProperty);
			}
		} else {
			if (propertyParts.length == 1) {
				addValueToPropertiesMap(propertiesMap, currentKey);
			} else {
				addMapToPropertiesMap(propertiesMap, currentKey, restOfProperty);
			}
		}
	}

	private static void addValueToPropertiesMap(Map<String, Object> propertiesMap, String currentKey) {
		// Split the first occurrence of '='
		String[] keyValue = currentKey.split("=", 2);
		String key = keyValue[0].trim();
		if (keyValue.length == 2) {
			String[] values = keyValue[1].split(",");
			Object value = values.length > 1 ? values : values[0];
			if (!propertiesMap.containsKey(key)) {
				propertiesMap.put(key, value);
			}
		} else {
			propertiesMap.put(key, "");
		}
	}

	private static void addMapToPropertiesMap(Map<String, Object> propertiesMap, String currentKey, String[] restOfProperty) {
		Map<String, Object> newSubMap = new HashMap<>();
		propertiesMap.put(currentKey, newSubMap);
		addPropertyToMap(newSubMap, restOfProperty);
	}
}
