package org.frankframework.larva;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nonnull;

import lombok.Getter;

import org.frankframework.larva.actions.LarvaActionUtils;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StringResolver;

public class Step implements Comparable<Step> {

	private static final Pattern STEP_PARSE_RE = Pattern.compile("^step(\\d+)\\.(.+)(\\.read|\\.readline|\\.write|\\.writeline)$");

	private final @Getter Scenario scenario;
	private final @Getter String rawLine;
	private final @Getter int idx;
	private final @Getter String actionTarget;
	private final @Getter String action;
	private final @Getter String value;

	public Step(Scenario scenario, String rawLine, int idx, String actionTarget, String action, String value) {
		this.scenario = scenario;
		this.rawLine = rawLine;
		this.idx = idx;
		this.actionTarget = actionTarget;
		this.action = action;
		this.value = value;
	}

	public static Step of(Scenario scenario, String stepLine) {
		Matcher stepMatch = STEP_PARSE_RE.matcher(stepLine);
		if (!stepMatch.matches()) {
			throw new IllegalArgumentException("Step '" + stepLine + "' does not have a step number or action");
		}
		String value = scenario.getProperties().getProperty(stepLine);
		return new Step(scenario, stepLine, Integer.parseInt(stepMatch.group(1)), stepMatch.group(2), stepMatch.group(3), value);
	}

	public String getStepDataFile() {
		if (isInline() || isIgnore()) {
			return null;
		}
		return scenario.getProperties().getProperty(rawLine + ".absolutepath");
	}

	public boolean isInline() {
		return action.endsWith("line");
	}

	public boolean isIgnore() {
		return value.toLowerCase().contains("ignore");
	}

	public boolean isRead() {
		return  action.toLowerCase().contains("read");
	}

	public boolean isWrite() {
		return  action.toLowerCase().contains("write");
	}

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
			throw new LarvaException("Failed to resolve properties in input file [" + value + "] for step " + idx);
		}
		return new Message(StringResolver.substVars(fileData, appConstants), fileMessage.copyContext());
	}

	public Properties getStepParameters() {
		return LarvaActionUtils.getSubProperties(scenario.getProperties(), rawLine);
	}

	@Override
	public int compareTo(@Nonnull Step o) {
		return Integer.compare(idx, o.idx);
	}

	@Override
	public String toString() {
		return "Step " + idx +
				", action '" + actionTarget + '.' + action +
				"' = '" + value + '\''
				;
	}
}
