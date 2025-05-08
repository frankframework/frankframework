/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.larva;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Data class to hold scenario data.
 */
@Log4j2
public class Scenario {
	private final @Getter ID id;
	private final @Getter File scenarioFile;
	private final @Getter String name;
	private final @Getter String description;
	private final @Getter Properties properties;

	public Scenario(File scenarioFile, String name, String description, Properties properties) {
		this.id = new ID(scenarioFile);
		this.scenarioFile = scenarioFile;
		this.name = name;
		this.description = description;
		this.properties = properties;
	}

	public String getLongName() {
		return scenarioFile.getAbsolutePath();
	}

	public List<String> getSteps(LarvaConfig larvaConfig) {
		// TODO: This code can really do with some improvements and better test coverage. Now that it is part of Scenario class that will be easier.
		List<String> steps = new ArrayList<>();
		int i = 1;
		boolean lastStepFound = false;
		while (!lastStepFound) {
			boolean stepFound = false;
			Enumeration<?> enumeration = properties.propertyNames();
			while (enumeration.hasMoreElements()) {
				String key = (String) enumeration.nextElement();
				if (key.startsWith("step" + i + ".") && (key.endsWith(".read") || key.endsWith(".write") || (larvaConfig.isAllowReadlineSteps() && key.endsWith(".readline")) || key.endsWith(".writeline"))) {
					if (!stepFound) {
						steps.add(key);
						stepFound = true;
						log.debug("Added step '{}'", key);
					} else {
						throw new LarvaException("More than one step" + i + " properties found, already found '" + steps.get(steps.size() - 1) + "' before finding '" + key + "'");
					}
				}
			}
			if (!stepFound) {
				lastStepFound = true;
			}
			i++;
		}
		return steps;
	}

	public static class ID {
		private final String scenarioId;

		public ID(String scenarioFilePath) {
			this(new File(scenarioFilePath));
		}

		public ID(File scenarioFile) {
			this.scenarioId = scenarioFile.getAbsolutePath().replaceAll("([\\\\/:])", "-");
		}

		@Override
		public String toString() {
			return scenarioId;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ID id1 = (ID) o;
			return Objects.equals(scenarioId, id1.scenarioId);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(scenarioId);
		}
	}
}
