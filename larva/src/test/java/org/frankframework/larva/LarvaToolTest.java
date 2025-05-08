package org.frankframework.larva;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.StringWriter;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.IbisContext;
import org.frankframework.larva.actions.LarvaApplicationContext;
import org.frankframework.lifecycle.FrankApplicationInitializer;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.util.AppConstants;

@Log4j2
class LarvaToolTest {

	private static TestConfiguration configuration;
	private static ApplicationContext applicationContext;

	private AppConstants appConstants;
	private File scenarioRoot;
	private IbisContext ibisContext;

	@BeforeAll
	public static void beforeAll() throws Exception {
		configuration = TransactionManagerType.DATASOURCE.create(true);
		applicationContext = configuration.getApplicationContext();

		try {
			configuration.refresh();
			configuration.start();
		} catch (Exception e) {
			log.error("Error starting configuration", e);
		}
	}

	@BeforeEach
	public void setUp() {
		ibisContext = configuration.getIbisManager().getIbisContext();
		appConstants = AppConstants.getInstance();

		scenarioRoot = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios");
		appConstants.setProperty("scenariosroot1.directory", scenarioRoot.getAbsolutePath());
		appConstants.setProperty("scenariosroot1.description", "Test Scenarios Root Directory");

		// Whacky Workaround for failing to create the 1st LarvaApplicationContext
		try (LarvaApplicationContext ignore = new LarvaApplicationContext(ibisContext, scenarioRoot.getAbsolutePath())) {
			// No-op
		} catch (Exception e) {
			// Ignore the error
			log.warn("Error setting up application context, ignoring", e);
		}
	}

	@AfterEach
	public void tearDown() {
		appConstants.remove("scenariosroot1.directory");
		appConstants.remove("scenariosroot1.description");

	}

	@Test
	void testRunScenariosFromServletRequest () {
		// Arrange

		// TODO: Set up more meaningful tests
		ServletContext servletContext = new MockServletContext();
		servletContext.setAttribute(FrankApplicationInitializer.CONTEXT_KEY, ibisContext);
		MockHttpServletRequest mockRequest = new MockHttpServletRequest(servletContext);
		mockRequest.addParameter(LarvaHtmlConfig.REQUEST_PARAM_EXECUTE, scenarioRoot.getAbsolutePath());

		StringWriter output = new StringWriter();

		// Act
		int result = LarvaTool.runScenarios(servletContext, mockRequest, output);

		// Assert
		assertEquals(0, result, output.toString());
	}

	@Test
	void testRunScenariosFromPlainConfig () {
		// Arrange
		IbisContext ibisContext = configuration.getIbisManager().getIbisContext();
		StringWriter output = new StringWriter();
		LarvaTool larvaTool = LarvaTool.createInstance(ibisContext, output);

		// Act
		int result = larvaTool.runScenarios(scenarioRoot.getAbsolutePath());

		// Assert
		assertEquals(0, result, output.toString());
		assertEquals(2, larvaTool.getTestRunStatus().getScenarioExecuteCount());
	}
}
