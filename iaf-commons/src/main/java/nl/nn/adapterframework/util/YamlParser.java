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

import lombok.Lombok;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class YamlParser extends Properties {

	/**
	 * Loads and parses a yaml file.
	 * Uses the {@link #recursiveYaml(String, Object)} method.
	 *
	 * @param reader the reader
	 */
	public YamlParser(Reader reader) {
		Yaml yaml = new Yaml();
		Map<String, Object> obj = yaml.loadAs(reader, Map.class);
		obj.entrySet().forEach(entry -> {
			try {
				recursiveYaml(entry.getKey(), entry.getValue());
			} catch (IOException e) {
				throw Lombok.sneakyThrow(e);
			}
		});
	}

	/**
	 * Recursively traverses the object; When string is found will put it in the properties.
	 *
	 * @param key   Key of the property
	 * @param value Value of the property
	 */
	public void recursiveYaml(String key, Object value) throws IOException {
		// If the value is a map, will recursively call the method.
		// Key will be added to the 'keychain'.
		if (value instanceof Map) {
			mapEntry(key, value);
		}

		// Due to how the parser works, Arraylist encapsulates the map.
		// Therefore, key doesn't need to be updated, only the value.
		else if (value instanceof ArrayList) {
			arrayListEntry(key, value);
		}

		// Else if the value is a string, will put it in the properties.
		else {
			String valueToString = value.toString();
			String[] split = (valueToString).split(":", 1);
			if (split.length == 1) {
				put(key, split[0]);
			} else {
				put(key + "." + split[0], split[1]);
			}
		}
	}

	public void mapEntry(String key, Object value) {
		((Map<?, ?>) value).entrySet().forEach(entry -> {
			try {
				recursiveYaml(key + "." + entry.getKey(), entry.getValue());
			} catch (IOException e) {
				throw Lombok.sneakyThrow(e);
			}
		});
	}

	public void namePropertyCheck(String key, Object value){
		boolean isName = false;
		Iterator<? extends Map.Entry<?, ?>> iterator = ((Map<?, ?>) value).entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<?, ?> entry = iterator.next();
			try {
				if (entry.getKey().toString().equals("name")) {
					isName = true;
					iterator.remove();
					mapEntry(key + "." + entry.getValue(), value);
				}
			} catch (Exception e) {
				throw Lombok.sneakyThrow(e);
			}
		}
		if (!isName){
			mapEntry(key, value);
		}
	}

	public void arrayListEntry(String key, Object value) throws IOException {
		String valueAppend = "";

		for (int i = 0; i < ((ArrayList) value).size(); i++) {
			Object innerValue = ((ArrayList) value).get(i);

			if (innerValue instanceof Map) {
				namePropertyCheck(key, innerValue);
			}
			else if (innerValue.toString().contains(" ") || innerValue.toString().contains(":") || innerValue.toString().contains("=")) {
				recursiveYaml(key, innerValue);
			}
			else if (!StringUtils.isEmpty(innerValue.toString())) {
				if (valueAppend.equals("")) {
					valueAppend += innerValue;
				} else valueAppend += ("," + innerValue);
			}
		}
		if (!valueAppend.equals("")) {
			recursiveYaml(key, valueAppend);
		}
	}
}
