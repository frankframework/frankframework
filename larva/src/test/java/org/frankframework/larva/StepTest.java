package org.frankframework.larva;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.Properties;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StepTest {

	@ParameterizedTest
	@CsvSource({
			"step1.action.read, 1, action, read, true, false, false",
			"step2.action.readline, 2, action, readline, true, false, true",
			"step2.action.readLine, 2, action, readLine, true, false, true",
			"step3.another.action.writeline, 3, another.action, writeline, false, true, true",
			"step3.another.action.writeLine, 3, another.action, writeLine, false, true, true",
			"step4.another.action.write, 4, another.action, write, false, true, false",
	})
	void testCreateStep(String rawLine, int idx, String actionTarget, String action, boolean isRead, boolean isWrite, boolean isInline) {
		// Arrange
		Properties scenarioProperties = new Properties();
		scenarioProperties.setProperty(rawLine, "in.txt");
		scenarioProperties.setProperty(rawLine + Scenario.ABSOLUTE_PATH_PROPERTY_SUFFIX, "/path/to/scenarios/in.txt");

		File scenarioFile = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios/scenariodir1/active-scenario.properties");
		Scenario scenario = new Scenario(scenarioFile, "test", "test", scenarioProperties);

		// Act
		Step step = Step.of(scenario, rawLine);

		// Assert
		String expectedDisplayName = "'test' - Step " + idx + ", action '" + actionTarget + "." + action + "' = '" + "in.txt" + "'";
		assertAll(
				() -> assertNotNull(step),
				() -> assertEquals(rawLine, step.getBaseKey()),
				() -> assertEquals("in.txt", step.getValue()),
				() -> assertEquals(expectedDisplayName, step.getDisplayName()),
				() -> assertEquals(idx, step.getIndex()),
				() -> assertEquals(actionTarget, step.getActionTarget()),
				() -> assertEquals(action, step.getAction()),
				() -> assertEquals(isRead, step.isRead()),
				() -> assertEquals(isWrite, step.isWrite()),
				() -> assertEquals(isInline, step.isInline())
		);

		if (step.isInline()) {
			assertNull(step.getStepDataFile());
		} else {
			assertEquals("/path/to/scenarios/in.txt", step.getStepDataFile());
		}
	}

	@ParameterizedTest
	@CsvSource({
			"step1..read",
			"step1.valid.action.readl",
			"step1.valid.action",
			"step1.valid.action.",
			"ste1.valid.action.writeline",
			"step.valid.action.writeline",
	})
	void testCreateStepInvalid(String rawLine) {
		// Arrange
		Properties scenarioProperties = new Properties();
		scenarioProperties.setProperty(rawLine, "in.txt");
		scenarioProperties.setProperty(rawLine + Scenario.ABSOLUTE_PATH_PROPERTY_SUFFIX, "/path/to/scenarios/in.txt");

		File scenarioFile = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios/scenariodir1/active-scenario.properties");

		Scenario scenario = new Scenario(scenarioFile, "test", "test", scenarioProperties);

		// Act
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Step.of(scenario, rawLine));

		// Assert
		assertEquals("Step '" + rawLine + "' does not have a step number, action target, or action", e.getMessage());
		assertFalse(Step.isValidStep(rawLine));
	}

	@ParameterizedTest
	@CsvSource({
			"ignore, true",
			"IGNORE, true",
			"DoIgnore, true",
			"IgnoreSomething, false",
			"TestThatIgnoresSomething, false",
			"IGNORED, false"
	})
	void testStepIgnore(String value, boolean shouldIgnoreFile) {
		// Arrange
		String rawLine = "step1.target.read";
		Properties scenarioProperties = new Properties();
		scenarioProperties.setProperty(rawLine, value);

		File scenarioFile = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios/scenariodir1/active-scenario.properties");
		Scenario scenario = new Scenario(scenarioFile, "test", "test", scenarioProperties);

		Step step = Step.of(scenario, rawLine);

		// Act
		assertEquals(shouldIgnoreFile, step.isIgnore(), "Step file should be ignored: [" + shouldIgnoreFile + "] but instead was [" + step.isIgnore() + "]");
	}
}
