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

import org.frankframework.configuration.IbisContext;
import org.frankframework.lifecycle.FrankApplicationInitializer;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.AppConstants;

class LarvaToolTest {

	private static TestConfiguration configuration;
	private static ApplicationContext applicationContext;

	private AppConstants appConstants;
	private File scenarioRoot;

	@BeforeAll
	public static void beforeAll() throws Exception {
		configuration = new TestConfiguration();
		applicationContext = configuration.getApplicationContext();

	}

	@BeforeEach
	public void setUp() {
		appConstants = AppConstants.getInstance();

		scenarioRoot = LarvaTestHelpers.getFileFromResource("/scenario-test-data/scenarios");
		appConstants.setProperty("scenariosroot1.directory", scenarioRoot.getAbsolutePath());
		appConstants.setProperty("scenariosroot1.description", "Test Scenarios Root Directory");
	}

	@AfterEach
	public void tearDown() {
		appConstants.remove("scenariosroot1.directory");
		appConstants.remove("scenariosroot1.description");

	}

	@Test
	void testRunScenarios() {
		// Arrange

		// TODO: Set up more meaningful tests

		IbisContext ibisContext = applicationContext.getAutowireCapableBeanFactory().createBean(IbisContext.class);
		ServletContext servletContext = new MockServletContext();
		servletContext.setAttribute(FrankApplicationInitializer.CONTEXT_KEY, ibisContext);
		MockHttpServletRequest mockRequest = new MockHttpServletRequest(servletContext);
		mockRequest.addParameter(LarvaHtmlConfig.REQUEST_PARAM_EXECUTE, scenarioRoot.getAbsolutePath());

		StringWriter output = new StringWriter();

		// Act
		int result = LarvaTool.runScenarios(servletContext, mockRequest, output);

		// Assert
		assertEquals(2, result);
	}
}
