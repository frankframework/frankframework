package org.frankframework.larva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.IbisContext;
import org.frankframework.lifecycle.FrankApplicationInitializer;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.PropertyLoader;

@Log4j2
class LarvaToolTest {

	private static TestConfiguration configuration;
	private static ApplicationContext applicationContext;

	private static AppConstants appConstants;
	private static File scenarioRoot;

	private Properties properties;

	@BeforeAll
	public static void beforeAll() {
		configuration = new TestConfiguration();
		applicationContext = configuration.getApplicationContext();
		appConstants = AppConstants.getInstance();
		scenarioRoot = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios");
	}

	@BeforeEach
	public void setUp() {
		appConstants.setProperty("scenariosroot1.directory", scenarioRoot.getAbsolutePath());
		appConstants.setProperty("scenariosroot1.description", "Test Scenarios Root Directory");

		properties = new PropertyLoader("scenario-testdata.properties");
	}

	@AfterEach
	public void tearDown() {
		appConstants.remove("scenariosroot1.directory");
		appConstants.remove("scenariosroot1.description");
		appConstants.remove("testdata.dir");
	}

	@AfterAll
	public static void afterAll() {
		CloseUtils.closeSilently(configuration);
	}

	@Test
	public void testPrepareForCompareWithTestDataDirNotSet() {
		// Arrange
		LarvaTool larvaTool = LarvaTool.createInstance(configuration);
		Map<String, Map<String, Map<String, String>>> ignoresMap = LarvaTool.mapPropertiesToIgnores(properties);

		String input = """
				<test>C:\\TestData\\SomePath\\MyFile.txt</test>
				""";

		String expected = """
				<test>C:\\TestData\\SomePath\\MyFile.txt</test>
				""";

		// Act
		String actual = larvaTool.prepareResultForCompare(input, properties, ignoresMap);

		// Assert
		assertEquals(expected, actual);
	}

	@Test
	public void testPrepareForCompareWithTestDataDirUnixPathInput() {
		// Arrange
		LarvaTool larvaTool = LarvaTool.createInstance(configuration);
		appConstants.setProperty("testdata.dir", "/var/testdata");
		Map<String, Map<String, Map<String, String>>> ignoresMap = LarvaTool.mapPropertiesToIgnores(properties);

		String input = """
				<test>/var/testdata/SomePath/MyFile.txt</test>
				""";

		String expected = """
				<test>TESTDATA_DIR/SomePath/MyFile.txt</test>
				""";

		// Act
		String actual = larvaTool.prepareResultForCompare(input, properties, ignoresMap);

		// Assert
		assertEquals(expected, actual);
	}

	@Test
	public void testPrepareForCompareWithTestDataDirWindowsPathInput() {
		// Arrange
		LarvaTool larvaTool = LarvaTool.createInstance(configuration);
		appConstants.setProperty("testdata.dir", "C:\\TestData");
		Map<String, Map<String, Map<String, String>>> ignoresMap = LarvaTool.mapPropertiesToIgnores(properties);

		String input = """
				<test>C:\\TestData\\SomePath\\MyFile.txt</test>
				""";

		String expected = """
				<test>TESTDATA_DIR/SomePath/MyFile.txt</test>
				""";

		// Act
		String actual = larvaTool.prepareResultForCompare(input, properties, ignoresMap);

		// Assert
		assertEquals(expected, actual);
	}

	@Test
	void testRunScenariosFromServletRequest () {
		// Arrange
		IbisContext mockIbisContext = mock();
		when(mockIbisContext.getApplicationContext()).thenReturn((AbstractApplicationContext) applicationContext);
		ServletContext servletContext = new MockServletContext();
		servletContext.setAttribute(FrankApplicationInitializer.CONTEXT_KEY, mockIbisContext);
		MockHttpServletRequest mockRequest = new MockHttpServletRequest(servletContext);
		mockRequest.addParameter(LarvaHtmlConfig.REQUEST_PARAM_EXECUTE, scenarioRoot.getAbsolutePath());

		StringWriter output = new StringWriter();

		// Act
		TestRunStatus result = LarvaTool.runScenarios(servletContext, mockRequest, output);

		// Assert
		verifyTestRunResults(result, output);
	}

	@Test
	void testRunScenariosFromPlainConfig () {
		// Arrange
		StringWriter output = new StringWriter();

		// Act
		TestRunStatus result = LarvaTool.runScenarios(applicationContext, output, scenarioRoot.getAbsolutePath());

		// Assert
		verifyTestRunResults(result, output);
	}

	private static void verifyTestRunResults(TestRunStatus result, StringWriter output) {
		List<String> expectedFailed = List.of("scenariodir1/active-failing-scenario");
		List<String> expectedPassed = List.of("scenariodir1/active-scenario", "scenariodir1/scenario-using-params", "scenariodir1/subdir/scenario01");

		assertEquals(4, result.getScenarioExecuteCount(), () -> "Expected 3 scenarios to be executed, but was " + result.getScenarioExecuteCount() + "\n" + output.toString());
		assertIterableEquals(expectedFailed, result.getFailedScenarios().stream().map(Scenario::getName).toList(), () -> "Expected " + expectedFailed.size() + " scenarios to fail, but was " + result.getFailedScenarios().size() + "\n" + output.toString());
		assertIterableEquals(expectedPassed, result.getPassedScenarios().stream().map(Scenario::getName).toList(), () -> "Expected " + expectedPassed.size() + " scenarios to pass, but was " + result.getPassedScenarios().size() + "\n" + output.toString());
	}
}
