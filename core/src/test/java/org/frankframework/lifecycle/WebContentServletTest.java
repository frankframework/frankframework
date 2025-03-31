package org.frankframework.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.IOException;

import jakarta.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.IbisManager;
import org.frankframework.configuration.classloaders.DirectoryClassLoader;
import org.frankframework.testutil.JunitTestClassLoaderWrapper;
import org.frankframework.testutil.NullClassLoader;
import org.frankframework.testutil.TestConfiguration;

public class WebContentServletTest {

	private MockHttpServletResponse getWebContentForConfiguration(Configuration config) throws IOException {
		WebContentServlet webContentServlet = new WebContentServlet();
		MockHttpServletResponse response = new MockHttpServletResponse();

		webContentServlet.getWebContentForConfiguration(response, config);

		return response;
	}

	/**
	 * Use a DirectoryClassLoader with a 'webcontent' directory.
	 */
	@Test
	void testListDirectory() throws IOException, ClassLoaderException {
		TestConfiguration config = new TestConfiguration(TestConfiguration.TEST_CONFIGURATION_FILE);

		DirectoryClassLoader classLoader = new DirectoryClassLoader(new NullClassLoader());
		classLoader.setDirectory(JunitTestClassLoaderWrapper.getTestClassesLocation() + "WebContentTestRoot");
		config.setClassLoader(classLoader);

		// Act
		MockHttpServletResponse response = getWebContentForConfiguration(config);

		// Assert
		String contentAsString = response.getContentAsString();
		assertTrue(contentAsString.contains("TestConfiguration"));
	}

	/**
	 * Uses the default `JunitTestClassLoaderWrapper` ClassLoader.
	 */
	@Test
	void testListDirectoryNotPresent() throws IOException {
		TestConfiguration config = new TestConfiguration(TestConfiguration.TEST_CONFIGURATION_FILE);

		// Act
		MockHttpServletResponse response = getWebContentForConfiguration(config);

		// Assert
		String contentAsString = response.getContentAsString();
		assertFalse(contentAsString.contains("TestConfiguration"));
	}

	private MockHttpServletResponse doGet(Configuration config, String path) throws IOException, ServletException {
		WebContentServlet webContentServlet = spy(WebContentServlet.class);
		IbisManager ibisManager = new IbisManager();
		ibisManager.addConfiguration(config);
		doReturn(ibisManager).when(webContentServlet).getIbisManager();

		webContentServlet.init();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
		request.setPathInfo(path);
		MockHttpServletResponse response = new MockHttpServletResponse();

		webContentServlet.doGet(request, response);

		return response;
	}

	@Test
	void noPath() throws IOException, ServletException {
		TestConfiguration config = new TestConfiguration(TestConfiguration.TEST_CONFIGURATION_FILE);

		// Act
		MockHttpServletResponse response = doGet(config, null);

		// Assert
		assertEquals(302, response.getStatus());
		String contentAsString = response.getContentAsString();
		assertTrue(StringUtils.isBlank(contentAsString));
	}

	@Test
	void rootPath() throws IOException, ServletException {
		TestConfiguration config = new TestConfiguration(TestConfiguration.TEST_CONFIGURATION_FILE);

		// Act
		MockHttpServletResponse response = doGet(config, "/");

		// Assert
		assertEquals(404, response.getStatus());
		String contentAsString = response.getContentAsString();
		assertTrue(StringUtils.isBlank(contentAsString)); // By default to dtap stage is not LOC, returns a 404
	}

	@Test
	void testConfigurationWelcomeFile() throws IOException, ServletException, ClassLoaderException {
		TestConfiguration config = new TestConfiguration(TestConfiguration.TEST_CONFIGURATION_FILE);

		DirectoryClassLoader classLoader = new DirectoryClassLoader(new NullClassLoader());
		classLoader.setDirectory(JunitTestClassLoaderWrapper.getTestClassesLocation() + "WebContentTestRoot");
		config.setClassLoader(classLoader);

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME);

		// Assert
		assertEquals(200, response.getStatus());
		assertEquals("text/html", response.getContentType());
		String contentAsString = response.getContentAsString();
		assertEquals("<html><h1>HTML</h1></html>", contentAsString.trim());
	}

	@Test
	void testConfigurationTestFile() throws IOException, ServletException, ClassLoaderException {
		TestConfiguration config = new TestConfiguration(TestConfiguration.TEST_CONFIGURATION_FILE);

		DirectoryClassLoader classLoader = new DirectoryClassLoader(new NullClassLoader());
		classLoader.setDirectory(JunitTestClassLoaderWrapper.getTestClassesLocation() + "WebContentTestRoot");
		config.setClassLoader(classLoader);

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/test.txt");

		// Assert
		assertEquals(200, response.getStatus());
		assertEquals("text/plain", response.getContentType());
		String contentAsString = response.getContentAsString();
		assertEquals("git won't persist empty directories, so we need to add a file to the directory to make sure it gets persisted.", contentAsString.trim());
	}
}
