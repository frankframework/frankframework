package org.frankframework.larva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import org.frankframework.larva.actions.LarvaScenarioAction;
import org.frankframework.larva.output.TestExecutionObserver;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.CloseUtils;

class ScenarioRunnerTest {

	private static TestConfiguration configuration;
	private static ApplicationContext applicationContext;

	@BeforeAll
	static void beforeAll() {
		configuration = new TestConfiguration();
		applicationContext = configuration.getApplicationContext();
	}

	@AfterAll
	static void afterAll() {
		CloseUtils.closeSilently(configuration);
	}

	private ScenarioRunner createScenarioRunner() {
		LarvaTool larvaTool = LarvaTool.createInstance(applicationContext);
		TestExecutionObserver observer = mock();
		TestRunStatus testRunStatus = new TestRunStatus(larvaTool.getLarvaConfig(), larvaTool);
		return new ScenarioRunner(larvaTool, observer, testRunStatus);
	}

	private Step createStep(String fileName, String actionClassName, Properties extraProperties) {
		String rawLine = "step1.reader.read";
		Properties scenarioProperties = new Properties();
		scenarioProperties.setProperty(rawLine, fileName);
		scenarioProperties.setProperty(rawLine + Scenario.ABSOLUTE_PATH_PROPERTY_SUFFIX, fileName);
		scenarioProperties.setProperty("reader" + Scenario.CLASS_NAME_PROPERTY_SUFFIX, actionClassName);
		extraProperties.forEach((key, value) -> scenarioProperties.setProperty(rawLine + "." + key, (String) value));

		File scenarioFile = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios/scenariodir1/active-scenario.properties");
		Scenario scenario = new Scenario(scenarioFile, "test", "test", scenarioProperties);
		return Step.of(scenario, rawLine);
	}

	// --- Existing single-shot behavior (no waitfor.* set) — establishes the regression baseline ---

	@Test
	void testSingleShotMatchReturnsOk() throws Exception {
		// Arrange
		ScenarioRunner scenarioRunner = createScenarioRunner();
		Step step = createStep("expected.txt", "org.frankframework.jdbc.FixedQuerySender", new Properties());
		LarvaScenarioAction action = mock();
		when(action.executeRead(any())).thenReturn(new Message("actual"));

		// Act
		int result = scenarioRunner.executeActionReadStep(step.getScenario(), step, action, "expected.txt", new Message("actual"));

		// Assert
		assertEquals(LarvaTool.RESULT_OK, result);
		verify(action, times(1)).executeRead(any());
	}

	@Test
	void testSingleShotMismatchReturnsErrorWithoutRetry() throws Exception {
		// Arrange
		ScenarioRunner scenarioRunner = createScenarioRunner();
		Step step = createStep("expected.txt", "org.frankframework.jdbc.FixedQuerySender", new Properties());
		LarvaScenarioAction action = mock();
		when(action.executeRead(any())).thenReturn(new Message("actual"));

		// Act
		int result = scenarioRunner.executeActionReadStep(step.getScenario(), step, action, "expected.txt", new Message("expected"));

		// Assert
		assertEquals(LarvaTool.RESULT_ERROR, result);
		verify(action, times(1)).executeRead(any());
	}

	@Test
	void testNullMessageWithEmptyFileNameReturnsOk() throws Exception {
		// Arrange
		ScenarioRunner scenarioRunner = createScenarioRunner();
		Step step = createStep("", "org.frankframework.jdbc.FixedQuerySender", new Properties());
		LarvaScenarioAction action = mock();
		when(action.executeRead(any())).thenReturn(null);

		// Act
		int result = scenarioRunner.executeActionReadStep(step.getScenario(), step, action, "", new Message("expected"));

		// Assert
		assertEquals(LarvaTool.RESULT_OK, result);
	}

	@Test
	void testIgnoreFileNameReturnsOkWithoutComparing() throws Exception {
		// Arrange
		ScenarioRunner scenarioRunner = createScenarioRunner();
		Step step = createStep("ignore", "org.frankframework.jdbc.FixedQuerySender", new Properties());
		LarvaScenarioAction action = mock();
		when(action.executeRead(any())).thenReturn(new Message("anything"));

		// Act
		int result = scenarioRunner.executeActionReadStep(step.getScenario(), step, action, "ignore", new Message("expected"));

		// Assert
		assertEquals(LarvaTool.RESULT_OK, result);
		verify(action, times(1)).executeRead(any());
	}

	// --- New waitfor.* retry behavior ---

	@Test
	void testRetriesUntilFullContentMatch() throws Exception {
		// Arrange
		Properties waitFor = new Properties();
		waitFor.setProperty("waitfor.timeout", "2000");
		waitFor.setProperty("waitfor.interval", "10");
		ScenarioRunner scenarioRunner = createScenarioRunner();
		Step step = createStep("expected.txt", "org.frankframework.jdbc.FixedQuerySender", waitFor);
		LarvaScenarioAction action = mock();
		when(action.executeRead(any())).thenReturn(new Message("not yet"), new Message("not yet"), new Message("expected"));

		// Act
		int result = scenarioRunner.executeActionReadStep(step.getScenario(), step, action, "expected.txt", new Message("expected"));

		// Assert
		assertEquals(LarvaTool.RESULT_OK, result);
		verify(action, times(3)).executeRead(any());
	}

	@Test
	void testTimesOutWithoutMatchReturnsError() throws Exception {
		// Arrange
		Properties waitFor = new Properties();
		waitFor.setProperty("waitfor.timeout", "100");
		waitFor.setProperty("waitfor.interval", "20");
		ScenarioRunner scenarioRunner = createScenarioRunner();
		Step step = createStep("expected.txt", "org.frankframework.jdbc.FixedQuerySender", waitFor);
		LarvaScenarioAction action = mock();
		when(action.executeRead(any())).thenReturn(new Message("never matches"));

		// Act
		int result = scenarioRunner.executeActionReadStep(step.getScenario(), step, action, "expected.txt", new Message("expected"));

		// Assert
		assertEquals(LarvaTool.RESULT_ERROR, result);
		verify(action, atLeast(2)).executeRead(any());
	}

	@Test
	void testRetriesUntilXPathExpressionMatches() throws Exception {
		// Arrange
		// Timeout generous enough to absorb first-time TransformerPool/XSLT-engine classloading cost
		Properties waitFor = new Properties();
		waitFor.setProperty("waitfor.timeout", "10000");
		waitFor.setProperty("waitfor.interval", "10");
		waitFor.setProperty("waitfor.xPath", "count(/results/result[@type='E']) > 0");
		ScenarioRunner scenarioRunner = createScenarioRunner();
		Step step = createStep("expected.xml", "org.frankframework.jdbc.FixedQuerySender", waitFor);
		Message expected = new Message("<results><result type=\"E\"/></results>");
		LarvaScenarioAction action = mock();
		when(action.executeRead(any())).thenReturn(new Message("<results/>"), expected);

		// Act
		int result = scenarioRunner.executeActionReadStep(step.getScenario(), step, action, "expected.xml", expected);

		// Assert
		assertEquals(LarvaTool.RESULT_OK, result);
		verify(action, times(2)).executeRead(any());
	}

	@Test
	void testXPathMatchStopsPollingButFullCompareStillDecidesOutcome() throws Exception {
		// Arrange: the 2nd read satisfies the xPath condition (stops polling) but does not
		// byte-for-byte match the expected file, so the final compareResult() must still fail.
		// Timeout generous enough to absorb first-time TransformerPool/XSLT-engine classloading cost
		Properties waitFor = new Properties();
		waitFor.setProperty("waitfor.timeout", "10000");
		waitFor.setProperty("waitfor.interval", "10");
		waitFor.setProperty("waitfor.xPath", "count(/results/result[@type='E']) > 0");
		ScenarioRunner scenarioRunner = createScenarioRunner();
		Step step = createStep("expected.xml", "org.frankframework.jdbc.FixedQuerySender", waitFor);
		Message expected = new Message("<results><result type=\"E\"/></results>");
		Message xPathMatchesButDiffersFromExpected = new Message("<results><result type=\"E\"><extra/></result></results>");
		LarvaScenarioAction action = mock();
		when(action.executeRead(any())).thenReturn(new Message("<results/>"), xPathMatchesButDiffersFromExpected);

		// Act
		int result = scenarioRunner.executeActionReadStep(step.getScenario(), step, action, "expected.xml", expected);

		// Assert
		assertEquals(LarvaTool.RESULT_ERROR, result);
		verify(action, times(2)).executeRead(any());
	}
}
