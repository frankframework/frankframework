package nl.nn.adapterframework.util.experimental;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

public class YamlParser extends Properties {

	/**
	 * Loads and parses a yaml file.
	 * Uses the {@link #recursiveYaml(String, Object)} method.
	 * @param reader    the reader
	 */
	public YamlParser(Reader reader) {
		Yaml yaml = new Yaml();
		Map<String, Object> obj = yaml.loadAs(reader, Map.class);
		obj.entrySet().forEach(entry -> {
			try {
				recursiveYaml(entry.getKey(), entry.getValue());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Recursively traverses the object; When string is found will put it in the properties.
	 * @param key    	Key of the property
	 * @param value 	Value of the property
	 */
	public void recursiveYaml(String key, Object value) throws IOException {

		// If the value is a map, will recursively call the method.
		// Key will be added to the 'keychain'.
		if (value instanceof Map){
			((Map<?, ?>) value).entrySet().forEach(entry -> {
				try {
					recursiveYaml(key + "." + entry.getKey(), entry.getValue());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
		// Due to how the parser works, Arraylist encapsulates the map.
		// Therefore, key doesn't need to be updated, only the value.
		else if (value instanceof ArrayList){
			String valueAppend = "";

			for (int i = 0; i < ((ArrayList) value).size(); i++) {

				Object innerValue = ((ArrayList) value).get(i);
				// Checks if the value contains either a space, colon or equals sign.
				if (innerValue.toString().contains(" ") || innerValue.toString().contains(":") || innerValue.toString().contains("=")){
					recursiveYaml(key, innerValue);
				}
				// else if value is not ""
				else if (StringUtils.isEmpty(innerValue.toString()) == false){
					if (valueAppend == "") {valueAppend += innerValue;}
					else valueAppend += ("," +innerValue );
				}
			}
			if (valueAppend != "") {
				recursiveYaml(key, valueAppend);}
		}

		// Else if the value is a string, will put it in the properties.
		else {
			value = value.toString();
			String[] split = ((String) value).split(":", 1);
			if (split.length == 1) {put(key, split[0]);}
			else{put(key + "." + split[0], split[1]);}
		}
	}
}
