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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Data class to hold scenario data.
 */
@Log4j2
public class Scenario {

	private static final Pattern STEP_NR_RE = Pattern.compile("^step(\\d+)\\..+(\\.read|\\.readline|\\.write|\\.writeline)$");

	private final @Getter ID id;
	private final @Getter File scenarioFile;
	private final @Getter String name;
	private final @Getter String description;
	private final @Getter Properties properties;
	private Map<String, Map<String, Map<String, String>>> ignoreMapCache;

	public Scenario(File scenarioFile, String name, String description, Properties properties) {
		this.id = new ID(scenarioFile);
		this.scenarioFile = scenarioFile;
		this.name = name;
		this.description = description;
		this.properties = properties;
	}

	@Override
	public String toString() {
		return "Scenario[" +
				"name='" + name + '\'' +
				", description='" + description + '\'' +
				'[';
	}

	public String getLongName() {
		return scenarioFile.getAbsolutePath();
	}

	public List<String> getSteps(LarvaConfig larvaConfig) {
		// Filter and sort steps from all scenario property names
		List<String> steps = properties.stringPropertyNames().stream()
				.filter(Scenario::isValidStep)
				.filter(step -> larvaConfig.isAllowReadlineSteps() || !step.endsWith(".readline"))
				.sorted(new Scenario.StepSorter())
				.toList();

		// Validate that there are no duplicate step numbers
		//noinspection ResultOfMethodCallIgnored
		steps.stream()
				.mapToInt(Scenario::getStepNr)
				.reduce(-1, (lastStepNr, stepNr) -> {
					if (lastStepNr == stepNr) {
						throw new LarvaException(String.format("Scenario %s has more than one step numbered %d", name, stepNr));
					}
					return stepNr;
				});

		return steps;
	}

	public String getStepDataFile(String step) {
		return properties.getProperty(step + ".absolutepath");
	}

	public String getStepDisplayName(String step) {
		return getName() + " - " + step + " - " + properties.get(step);
	}

	public void clearScenarioCaches() {
		this.ignoreMapCache = null;
	}

	public Map<String, Map<String, Map<String, String>>> getIgnoreMap() {
		if (this.ignoreMapCache == null) {
			this.ignoreMapCache = LarvaTool.mapPropertiesToIgnores(this.properties);
		}
		return this.ignoreMapCache;
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

	private static class StepSorter implements Comparator<String> {
		@Override
		public int compare(String o1, String o2) {
			int step1Nr = getStepNr(o1);
			int step2Nr = getStepNr(o2);
			return Integer.compare(step1Nr, step2Nr);
		}
	}

	private static int getStepNr(String step) {
		Matcher stepNrMatch = STEP_NR_RE.matcher(step);
		if (!stepNrMatch.matches()) {
			throw new IllegalArgumentException("Step '" + step + "' does not have a step number");
		}
		return Integer.parseInt(stepNrMatch.group(1));
	}

	private static boolean isValidStep(String step) {
		Matcher stepMatch = STEP_NR_RE.matcher(step);
		return stepMatch.matches();
	}
}
