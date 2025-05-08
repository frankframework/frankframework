package org.frankframework.larva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.StringWriter;

import jakarta.servlet.ServletContext;

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

@Log4j2
class LarvaToolTest {

	private static TestConfiguration configuration;
	private static ApplicationContext applicationContext;

	private static AppConstants appConstants;
	private static File scenarioRoot;

	@BeforeAll
	public static void beforeAll() throws Exception {
		configuration = new TestConfiguration();
		applicationContext = configuration.getApplicationContext();
		try {
			configuration.refresh();
			configuration.start();
		} catch (Exception e) {
			log.error("Error starting configuration", e);
		}

		appConstants = AppConstants.getInstance();
		scenarioRoot = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios");
	}

	@BeforeEach
	public void setUp() {
		appConstants.setProperty("scenariosroot1.directory", scenarioRoot.getAbsolutePath());
		appConstants.setProperty("scenariosroot1.description", "Test Scenarios Root Directory");
	}

	@AfterEach
	public void tearDown() {
		appConstants.remove("scenariosroot1.directory");
		appConstants.remove("scenariosroot1.description");
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
		LarvaTool larvaTool = LarvaTool.createInstance(applicationContext, output);

		// Act
		TestRunStatus result = larvaTool.runScenarios(scenarioRoot.getAbsolutePath());

		// Assert
		verifyTestRunResults(result, output);
	}

	private static void verifyTestRunResults(TestRunStatus result, StringWriter output) {
		assertEquals(1, result.getScenariosFailedCount(), output.toString());
		assertEquals(2, result.getScenariosPassedCount());
		assertEquals(3, result.getScenarioExecuteCount());
	}
}
