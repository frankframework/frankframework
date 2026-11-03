package org.frankframework.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.IbisManager;
import org.frankframework.configuration.classloaders.BytesURLStreamHandler;
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
		TestConfiguration config = createWebContentConfiguration();

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
		TestConfiguration config = new TestConfiguration(TestConfiguration.TEST_SPRING_CONFIGURATION_FILE);

		// Act
		MockHttpServletResponse response = getWebContentForConfiguration(config);

		// Assert
		String contentAsString = response.getContentAsString();
		assertFalse(contentAsString.contains("TestConfiguration"));
	}

	private MockHttpServletResponse doGet(Configuration config, String path) throws IOException, ServletException {
		return doGet(config, path, null);
	}

	private MockHttpServletResponse doGet(Configuration config, String path, String queryString) throws IOException, ServletException {
		WebContentServlet webContentServlet = new WebContentServlet() {
			@Override
			IbisManager getIbisManager() {
				IbisManager ibisManager = new IbisManager();
				ibisManager.addConfiguration(config);
				return ibisManager;
			}
		};

		webContentServlet.init();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
		request.setPathInfo(path);
		if (path != null) {
			request.setRequestURI("/webcontent" + path);
		}
		request.setQueryString(queryString);
		MockHttpServletResponse response = new MockHttpServletResponse();

		webContentServlet.doGet(request, response);

		return response;
	}

	@Test
	void noPath() throws IOException, ServletException {
		TestConfiguration config = new TestConfiguration(TestConfiguration.TEST_SPRING_CONFIGURATION_FILE);

		// Act
		MockHttpServletResponse response = doGet(config, null);

		// Assert
		assertEquals(302, response.getStatus());
		String contentAsString = response.getContentAsString();
		assertTrue(StringUtils.isBlank(contentAsString));
	}

	@Test
	void rootPath() throws IOException, ServletException {
		TestConfiguration config = new TestConfiguration(TestConfiguration.TEST_SPRING_CONFIGURATION_FILE);

		// Act
		MockHttpServletResponse response = doGet(config, "/");

		// Assert
		assertEquals(404, response.getStatus());
		String contentAsString = response.getContentAsString();
		assertTrue(StringUtils.isBlank(contentAsString)); // By default to dtap stage is not LOC, returns a 404
	}

	private TestConfiguration createWebContentConfiguration() throws ClassLoaderException {
		TestConfiguration config = new TestConfiguration(TestConfiguration.TEST_SPRING_CONFIGURATION_FILE);

		DirectoryClassLoader classLoader = new DirectoryClassLoader(new NullClassLoader()) {
			@Override
			public URL getLocalResource(String name) {
				URL url = super.getLocalResource(name);
				if (url == null) {
					return null;
				}

				try {
					// If the resource is a directory, and the name does not end with a slash, return null to indicate that it should not be served.
					if (Files.isDirectory(Path.of(url.toURI())) && !name.endsWith("/")) {
						return null;
					}

					// Wrap the URL so it never uses the file system directly.
					// This ensures that serving files will work with all ClassLoaders, including the BytesClassLoader, which does not have a file system representation.
					try (InputStream is = url.openStream()) {
						URLStreamHandler urlStreamHandler = new BytesURLStreamHandler(is.readAllBytes());
						return new URL(null, CLASSPATH_RESOURCE_SCHEME + name, urlStreamHandler);
					}
				} catch (Exception e) {
					return fail("Could not read url for resource [" + name + "]", e);
				}
			}
		};

		classLoader.setDirectory(JunitTestClassLoaderWrapper.getTestClassesLocation() + "WebContentTestRoot");
		config.setClassLoader(classLoader);
		return config;
	}

	@Test
	void testConfigurationWelcomeFile() throws IOException, ServletException, ClassLoaderException {
		TestConfiguration config = createWebContentConfiguration();

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
		TestConfiguration config = createWebContentConfiguration();

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/test.txt");

		// Assert
		assertEquals(200, response.getStatus());
		assertEquals("text/plain", response.getContentType());
		String contentAsString = response.getContentAsString();
		assertEquals("git won't persist empty directories, so we need to add a file to the directory to make sure it gets persisted.", contentAsString.trim());
	}

	@Test
	void testConfigurationWelcomeFileWithTrailingSlash() throws IOException, ServletException, ClassLoaderException {
		TestConfiguration config = createWebContentConfiguration();

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/");

		// Assert
		assertEquals(200, response.getStatus());
		assertEquals("text/html", response.getContentType());
		assertEquals("<html><h1>HTML</h1></html>", response.getContentAsString().trim());
	}

	@Test
	void testSubFolderWelcomeFile() throws IOException, ServletException, ClassLoaderException {
		TestConfiguration config = createWebContentConfiguration();

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/sub/");

		// Assert
		assertEquals(200, response.getStatus());
		assertEquals("text/html", response.getContentType());
		assertEquals("<html><h1>SUB</h1></html>", response.getContentAsString().trim());
	}

	@Test
	void testSubFolderWithoutTrailingSlashRedirects() throws IOException, ServletException, ClassLoaderException {
		TestConfiguration config = createWebContentConfiguration();

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/sub");

		// Assert
		assertEquals(302, response.getStatus());
		assertEquals("./sub/", response.getRedirectedUrl());
		assertTrue(StringUtils.isBlank(response.getContentAsString()));
	}

	@Test
	void testSubFolderRedirectStaysRelativeForDoubleSlashRequestUri() throws IOException, ServletException, ClassLoaderException {
		TestConfiguration config = createWebContentConfiguration();
		WebContentServlet webContentServlet = spy(WebContentServlet.class);
		IbisManager ibisManager = new IbisManager();
		ibisManager.addConfiguration(config);
		doReturn(ibisManager).when(webContentServlet).getIbisManager();
		webContentServlet.init();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
		request.setPathInfo("/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/sub");
		request.setRequestURI("//webcontent/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/sub");
		MockHttpServletResponse response = new MockHttpServletResponse();

		// Act
		webContentServlet.doGet(request, response);

		// Assert
		assertEquals(302, response.getStatus());
		assertEquals("./sub/", response.getRedirectedUrl());
	}

	@Test
	void testSubFolderRedirectKeepsQueryString() throws IOException, ServletException, ClassLoaderException {
		TestConfiguration config = createWebContentConfiguration();

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/sub", "a=b&c=d");

		// Assert
		assertEquals(302, response.getStatus());
		assertEquals("./sub/?a=b&c=d", response.getRedirectedUrl());
	}

	@Test
	void testSubFolderWithoutWelcomeFileIsNotListed() throws IOException, ServletException, ClassLoaderException {
		TestConfiguration config = createWebContentConfiguration();

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/emptysub/");

		// Assert
		assertEquals(404, response.getStatus());
		assertFalse(response.getContentAsString().contains("readme.txt"));
	}

	@Test
	void testSubFolderWithoutWelcomeFileOrTrailingSlashIsNotListed() throws IOException, ServletException, ClassLoaderException {
		TestConfiguration config = createWebContentConfiguration();

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/emptysub");

		// Assert
		assertEquals(404, response.getStatus());
		assertFalse(response.getContentAsString().contains("readme.txt"));
	}

	@Test
	void testFileInSubFolder() throws IOException, ServletException, ClassLoaderException {
		TestConfiguration config = createWebContentConfiguration();

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/emptysub/readme.txt");

		// Assert
		assertEquals(200, response.getStatus());
		assertEquals("text/plain", response.getContentType());
	}
}
