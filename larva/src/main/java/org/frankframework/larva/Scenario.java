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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Data class to hold scenario data.
 */
@Log4j2
public class Scenario {

	public static final String CLASS_NAME_PROPERTY_SUFFIX = ".className";
	public static final String ABSOLUTE_PATH_PROPERTY_SUFFIX = ".absolutepath";

	private final @Getter ID id;
	private final @Getter File scenarioFile;
	private final @Getter String name;
	private final @Getter String description;
	private final @Getter Properties properties;
	private final @Getter SortedSet<LarvaMessage> messages = new TreeSet<>(Comparator.comparing(LarvaMessage::getMessage));
	private final @Getter boolean resolvePropertiesInScenarioFiles;
	private Map<String, Map<String, Map<String, String>>> ignoreMapCache;

	public Scenario(File scenarioFile, String name, String description, Properties properties) {
		this.id = new ID(scenarioFile);
		this.scenarioFile = scenarioFile;
		this.name = name;
		this.description = description;
		this.properties = properties;
		this.resolvePropertiesInScenarioFiles = parseScenarioProperty("scenario.resolveProperties", true);
	}

	public Scenario(File scenarioFile, String name, String description, Properties properties, Collection<LarvaMessage> messages) {
		this(scenarioFile, name, description, properties);
		addMessages(messages);
	}

	public boolean parseScenarioProperty(String name, boolean dfault) {
		String value = properties.getProperty(name);
		if (value == null) {
			return dfault;
		}
		return "true".equalsIgnoreCase(value) || "!false".equalsIgnoreCase(value);
	}

	public void addWarning(@Nonnull String warning) {
		messages.add(new LarvaMessage(LarvaLogLevel.WARNING, warning));
	}

	public void addError(@Nonnull String error) {
		messages.add(new LarvaMessage(LarvaLogLevel.ERROR, error));
	}

	public void addError(@Nonnull String error, @Nonnull Exception e) {
		messages.add(new LarvaMessage(LarvaLogLevel.ERROR, error, e));
	}

	public void addMessages(@Nonnull Collection<LarvaMessage> messages) {
		this.messages.addAll(messages);
	}

	@Override
	public String toString() {
		return "Scenario[" +
				"name='" + name + '\'' +
				", description='" + description + '\'' +
				']';
	}

	public String getLongName() {
		return scenarioFile.getAbsolutePath();
	}

	public Set<String> getScenarioActionNames() {
		return properties.stringPropertyNames()
				.stream()
				.filter(key -> key.endsWith(CLASS_NAME_PROPERTY_SUFFIX))
				.map(key -> key.substring(0, key.lastIndexOf(".")))
				.collect(Collectors.toSet());
	}

	public String getScenarioActionClassName(String scenarioActionName) {
		String className = properties.getProperty(scenarioActionName + CLASS_NAME_PROPERTY_SUFFIX);
		// NB: Instead of rewriting legacy packages names when loading scenarios, we could also do that here instead.
		if ("org.frankframework.jms.JmsListener".equals(className)) {
			return "org.frankframework.jms.PullingJmsListener";
		}
		return className;
	}

	public List<Step> getSteps(LarvaConfig larvaConfig) {
		// Filter and sort steps from all scenario property names
		List<Step> steps = properties.stringPropertyNames().stream()
				.filter(Step::isValidStep)
				.filter(step -> larvaConfig.isAllowReadlineSteps() || !step.endsWith(".readline"))
				.map(step -> Step.of(this, step))
				.sorted()
				.toList();

		// Validate that there are no duplicate step numbers
		//noinspection ResultOfMethodCallIgnored
		steps.stream()
				.mapToInt(Step::getIdx)
				.reduce(-1, (lastStepNr, stepNr) -> {
					if (lastStepNr == stepNr) {
						throw new LarvaException(String.format("Scenario %s has more than one step numbered %d", name, stepNr));
					}
					return stepNr;
				});

		return steps;
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

}
