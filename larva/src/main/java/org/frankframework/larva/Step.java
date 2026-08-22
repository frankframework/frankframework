/*
   Copyright 2025-2026 WeAreFrank!

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

import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import lombok.Getter;

import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.larva.actions.LarvaActionUtils;
import org.frankframework.senders.DelaySender;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StringResolver;

/**
 * Step of a scenario loaded from property files.
 */
public class Step implements Comparable<Step> {

	private static final Pattern STEP_PARSE_RE = Pattern.compile("(?i)^step(?<idx>\\d+)\\.(?<target>.+)\\.(?<action>read|readline|write|writeline)$");

	private static final String WAITFOR_TIMEOUT_PROPERTY = "waitfor.timeout";
	private static final String WAITFOR_INTERVAL_PROPERTY = "waitfor.interval";
	private static final String WAITFOR_XPATH_PROPERTY = "waitfor.xPath";
	private static final long DEFAULT_WAITFOR_INTERVAL_MILLIS = 100L;

	// Only action classes whose read can be safely re-executed on every poll (query-style reads)
	// support waitfor.*. Other actions (e.g. listeners, or senders driven via SenderThread) either
	// throw on a second read or destructively consume a new message from a queue.
	private static final Set<String> WAITFOR_SUPPORTED_ACTION_CLASS_NAMES = Set.of(FixedQuerySender.class.getName(), DelaySender.class.getName());

	private final @Getter Scenario scenario;
	private final @Getter String baseKey;
	private final @Getter int index;
	private final @Getter String actionTarget;
	private final @Getter String action;
	private final @Getter String value;

	private Step(Scenario scenario, String baseKey, int index, String actionTarget, String action, String value) {
		this.scenario = scenario;
		this.baseKey = baseKey;
		this.index = index;
		this.actionTarget = actionTarget;
		this.action = action;
		this.value = value;
	}

	public static Step of(Scenario scenario, String stepLine) {
		Matcher stepMatch = STEP_PARSE_RE.matcher(stepLine);
		if (!stepMatch.matches()) {
			throw new IllegalArgumentException("Step '" + stepLine + "' does not have a step number, action target, or action");
		}
		String value = scenario.getProperties().getProperty(stepLine);
		Step step = new Step(scenario, stepLine, Integer.parseInt(stepMatch.group("idx")), stepMatch.group("target"), stepMatch.group("action"), value);
		step.validateWaitFor();
		return step;
	}

	public static boolean isValidStep(String stepLine) {
		return STEP_PARSE_RE.matcher(stepLine).matches();
	}

	public String getStepDataFile() {
		if (isInline() || isIgnore()) {
			return null;
		}
		return scenario.getProperties().getProperty(baseKey + Scenario.ABSOLUTE_PATH_PROPERTY_SUFFIX);
	}

	public String getDisplayName() {
		return "'" + scenario.getName() + "' - " + this;
	}

	public boolean isInline() {
		return action.toLowerCase().endsWith("line");
	}

	public boolean isIgnore() {
		return value.toLowerCase().endsWith("ignore");
	}

	public boolean isRead() {
		return  action.toLowerCase().contains("read");
	}

	public boolean isWrite() {
		return  action.toLowerCase().contains("write");
	}

	@NonNull
	public Message getStepMessage(AppConstants appConstants) throws IOException {
		if (isInline() || isIgnore()) {
			return new Message(value);
		}
		Message fileMessage = LarvaUtil.readFile(getStepDataFile());
		if (!scenario.isResolvePropertiesInScenarioFiles()) {
			return fileMessage;
		}
		String fileData = fileMessage.asString();
		if (fileData == null) {
			throw new LarvaException("Failed to resolve properties in input file [" + value + "] for step " + index);
		}
		return new Message(StringResolver.substVars(fileData, appConstants), fileMessage.copyContext());
	}

	public Properties getStepParameters() {
		return LarvaActionUtils.getSubProperties(scenario.getProperties(), baseKey);
	}

	public long getWaitForTimeoutMillis() {
		return parseWaitForMillis(WAITFOR_TIMEOUT_PROPERTY, 0L);
	}

	public long getWaitForIntervalMillis() {
		return parseWaitForMillis(WAITFOR_INTERVAL_PROPERTY, DEFAULT_WAITFOR_INTERVAL_MILLIS);
	}

	public @Nullable String getWaitForXPath() {
		return getStepParameters().getProperty(WAITFOR_XPATH_PROPERTY);
	}

	private long parseWaitForMillis(String propertyName, long defaultValue) {
		String value = getStepParameters().getProperty(propertyName);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Step '" + baseKey + "' property '" + propertyName + "' must be a number, but was '" + value + "'");
		}
	}

	private void validateWaitFor() {
		if (getWaitForTimeoutMillis() <= 0) {
			return;
		}
		String actionClassName = scenario.getScenarioActionClassName(actionTarget);
		if (actionClassName == null || !WAITFOR_SUPPORTED_ACTION_CLASS_NAMES.contains(actionClassName)) {
			throw new IllegalArgumentException("Step '" + baseKey + "' sets '" + WAITFOR_TIMEOUT_PROPERTY + "', but action '" + actionTarget
					+ "' (class '" + actionClassName + "') does not support waitfor; supported action classes: " + WAITFOR_SUPPORTED_ACTION_CLASS_NAMES);
		}
	}

	@Override
	public int compareTo(@NonNull Step o) {
		return Integer.compare(index, o.index);
	}

	@Override
	public String toString() {
		return "Step " + index +
				", action '" + actionTarget +
				'.' + action +
				"' = '" + value + '\''
				;
	}
}
