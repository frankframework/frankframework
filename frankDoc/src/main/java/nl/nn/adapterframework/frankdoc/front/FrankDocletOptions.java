/* 
Copyright 2021 WeAreFrank! 

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

package nl.nn.adapterframework.frankdoc.front;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.util.LogUtil;

class FrankDocletOptions {
	private static final Logger log = LogUtil.getLogger(FrankDocletOptions.class);

	private static enum Option {
		BASE_OUTPUT_DIR("outputDirectory"),
		STRICT_REL_PATH("strictPath"),
		COMPATIBILITY_REL_PATH("compatibilityPath"),
		JSON_REL_PATH("jsonPath");

		private @Getter String mavenName;

		private Option(String mavenName) {
			this.mavenName = mavenName;
		}
	}

	private static Map<String, Option> optionsByName;

	private @Getter String outputBaseDir;
	private @Getter String xsdStrictPath = "xml/xsd/FrankConfig-strict.xsd";
	private @Getter String xsdCompatibilityPath = "xml/xsd/FrankConfig-compatibility.xsd";
	private @Getter String jsonOutputPath = "js/frankdoc.json";

	static {
		optionsByName = Arrays.asList(Option.values()).stream().collect(Collectors.toMap(Option::getMavenName, Function.identity()));
	}

	static void validateOptions(String[][] options) throws InvalidDocletOptionsException {
		Set<Option> foundOptions = EnumSet.noneOf(Option.class);
		for(int i = 0; i < options.length; i++) {
			String[] commandLineOption = options[i];
			String optionMavenName = getOptionMavenName(commandLineOption);
			if(optionsByName.keySet().contains(optionMavenName)) {
				Option option = optionsByName.get(optionMavenName);
				if(foundOptions.contains(option)) {
					throw new InvalidDocletOptionsException(String.format("Duplicate option [%s]", "-" + option.getMavenName()));
				}
				if(commandLineOption.length != 2) {
					throw new InvalidDocletOptionsException(String.format("Only one value is allowed for option [%s], but got [%s]", Arrays.asList(commandLineOption).subList(1, commandLineOption.length).stream().collect(Collectors.joining(", "))));
				}
				foundOptions.add(option);
			}
		}
	}

	static int optionLength(String option) {
		if(!option.startsWith("-")) {
			return 0;
		}
		String key = option.substring(1);
		if(optionsByName.keySet().contains(key)) {
			return 2;
		}
		return 0;
	}

	private static String getOptionMavenName(String[] commandLineOption) throws InvalidDocletOptionsException {
		String optionString = commandLineOption[0];
		if(!optionString.substring(0, 1).equals("-")) {
			throw new InvalidDocletOptionsException(String.format("Option does not begin with \"-\": [%s]", optionString));
		}
		return optionString.substring(1);
	}

	static FrankDocletOptions getInstance(String[][] options) {
		log.info("Creating FrankDocletOptions object");
		FrankDocletOptions result = new FrankDocletOptions();
		result.fill(options);
		log.info("Done creating FrankDocletOptions object");
		return result;
	}

	private void fill(String[][] options) {
		for(int i = 0; i < options.length; ++i) {
			String[] opt = options[i];
			// Remove first "-" sign.
			String key = opt[0].substring(1);
			if(optionsByName.containsKey(key)) {
				Option option = optionsByName.get(key);
				String value = opt[1];
				log.info("Setting option [{}] to [{}]", option.getMavenName(), value);
				setOption(option, value);
			}
		}
	}

	private void setOption(Option opt, String value) {
		switch (opt) {
		case BASE_OUTPUT_DIR:
			outputBaseDir = value;
			break;
		case STRICT_REL_PATH:
			xsdStrictPath = value;
			break;
		case COMPATIBILITY_REL_PATH:
			xsdCompatibilityPath = value;
			break;
		case JSON_REL_PATH:
			jsonOutputPath = value;
			break;
		default:
			throw new IllegalArgumentException("Programming error. Switch over FrankDocletOptions.Option was supposed to cover all cases");
		}
	}
}
