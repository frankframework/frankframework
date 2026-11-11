package org.frankframework.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.servlet.ServletException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.IbisManager;
import org.frankframework.configuration.classloaders.AbstractBytesClassLoader;
import org.frankframework.configuration.classloaders.DirectoryClassLoader;
import org.frankframework.testutil.JunitTestClassLoaderWrapper;
import org.frankframework.testutil.NullClassLoader;
import org.frankframework.testutil.TestConfiguration;

public class WebContentServletTest {

	private static final String TEST_ROOT_DIRECTORY = JunitTestClassLoaderWrapper.getTestClassesLocation() + "WebContentTestRoot";

	private MockHttpServletResponse getWebContentForConfiguration(Configuration config) throws IOException {
		WebContentServlet webContentServlet = new WebContentServlet();
		MockHttpServletResponse response = new MockHttpServletResponse();

		webContentServlet.getWebContentForConfiguration(response, config);

		return response;
	}

	/**
	 * Use a ClassLoader with a 'webcontent' directory.
	 */
	@Test
	void testListDirectory() throws IOException, ClassLoaderException {
		TestConfiguration config = createWebContentConfiguration(createDirectoryClassLoader());

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

	private static ClassLoader createBytesClassLoader() throws ClassLoaderException {
		AbstractBytesClassLoader classLoader = new AbstractBytesClassLoader(createDirectoryClassLoader()) {
			@Override
			protected Map<String, byte[]> loadResources() throws ClassLoaderException {
				Map<String, byte[]> resources = new HashMap<>();
				Path root = Path.of(TEST_ROOT_DIRECTORY);
				try (Stream<Path> stream = Files.walk(root)) {
					stream.forEach(e -> {
						Path entry = root.relativize(e);
						String entryName = FilenameUtils.normalize(entry.toString(), true);
						if (StringUtils.isBlank(entryName)) return; // Skip the root directory itself

						if (Files.isDirectory(e)) {
							entryName = entryName + "/";
						}
						URL resource = super.getResource(entryName);
						assertNotNull(resource, "unable to find resource ["+resource+"] directory classpath should be identical!");

						try (InputStream is = resource.openStream()) {
							resources.put(entryName, is.readAllBytes());
						} catch (Exception exception) {
							fail("unable to read file [" + entryName + "]", exception);
						}
					});
				} catch (Exception e) {
					throw new ClassLoaderException(e);
				}
				return resources;
			}

			@Override
			public String toString() {
				return "TestBytesClassLoader";
			}
		};
		classLoader.setIbisContext(null);
		classLoader.setConfigurationName("");
		classLoader.afterPropertiesSet();
		return classLoader;
	}

	private static ClassLoader createDirectoryClassLoader() throws ClassLoaderException {
		DirectoryClassLoader classLoader = new DirectoryClassLoader(new NullClassLoader());
		classLoader.setDirectory(TEST_ROOT_DIRECTORY);
		return classLoader;
	}

	private static Stream<Arguments> provideClassLoaders() throws ClassLoaderException {
		return Stream.of(
				Arguments.of(createBytesClassLoader()),
				Arguments.of(createDirectoryClassLoader())
		);
	}

	private TestConfiguration createWebContentConfiguration(ClassLoader classLoader) {
		TestConfiguration config = new TestConfiguration(TestConfiguration.TEST_SPRING_CONFIGURATION_FILE);
		config.setClassLoader(classLoader);
		return config;
	}

	@ParameterizedTest
	@MethodSource("provideClassLoaders")
	void testConfigurationWelcomeFile(ClassLoader classLoader) throws Exception {
		TestConfiguration config = createWebContentConfiguration(classLoader);

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME);

		// Assert
		assertEquals(200, response.getStatus());
		assertEquals("text/html", response.getContentType());
		String contentAsString = response.getContentAsString();
		assertEquals("<html><h1>HTML</h1></html>", contentAsString.trim());
	}

	@ParameterizedTest
	@MethodSource("provideClassLoaders")
	void testConfigurationTestFile(ClassLoader classLoader) throws Exception {
		TestConfiguration config = createWebContentConfiguration(classLoader);

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/test.txt");

		// Assert
		assertEquals(200, response.getStatus());
		assertEquals("text/plain", response.getContentType());
		String contentAsString = response.getContentAsString();
		assertEquals("git won't persist empty directories, so we need to add a file to the directory to make sure it gets persisted.", contentAsString.trim());
	}

	@ParameterizedTest
	@MethodSource("provideClassLoaders")
	void testConfigurationWelcomeFileWithTrailingSlash(ClassLoader classLoader) throws Exception {
		TestConfiguration config = createWebContentConfiguration(classLoader);

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/");

		// Assert
		assertEquals(200, response.getStatus());
		assertEquals("text/html", response.getContentType());
		assertEquals("<html><h1>HTML</h1></html>", response.getContentAsString().trim());
	}

	@ParameterizedTest
	@MethodSource("provideClassLoaders")
	void testSubFolderWelcomeFile(ClassLoader classLoader) throws Exception {
		TestConfiguration config = createWebContentConfiguration(classLoader);

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/sub/");

		// Assert
		assertEquals(200, response.getStatus());
		assertEquals("text/html", response.getContentType());
		assertEquals("<html><h1>SUB</h1></html>", response.getContentAsString().trim());
	}

	@ParameterizedTest
	@MethodSource("provideClassLoaders")
	void testSubFolderWithoutTrailingSlashRedirects(ClassLoader classLoader) throws Exception {
		TestConfiguration config = createWebContentConfiguration(classLoader);

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/sub");

		// Assert
		assertEquals(302, response.getStatus());
		assertEquals("./sub/", response.getRedirectedUrl());
		assertTrue(StringUtils.isBlank(response.getContentAsString()));
	}

	@ParameterizedTest
	@MethodSource("provideClassLoaders")
	void testSubFolderRedirectStaysRelativeForDoubleSlashRequestUri(ClassLoader classLoader) throws Exception {
		TestConfiguration config = createWebContentConfiguration(classLoader);
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

	@ParameterizedTest
	@MethodSource("provideClassLoaders")
	void testSubFolderRedirectKeepsQueryString(ClassLoader classLoader) throws Exception {
		TestConfiguration config = createWebContentConfiguration(classLoader);

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/sub", "a=b&c=d");

		// Assert
		assertEquals(302, response.getStatus());
		assertEquals("./sub/?a=b&c=d", response.getRedirectedUrl());
	}

	@ParameterizedTest
	@MethodSource("provideClassLoaders")
	void testSubFolderWithoutWelcomeFileIsNotListed(ClassLoader classLoader) throws Exception {
		TestConfiguration config = createWebContentConfiguration(classLoader);

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/emptysub/");

		// Assert
		assertEquals(404, response.getStatus());
		assertFalse(response.getContentAsString().contains("readme.txt"));
	}

	@ParameterizedTest
	@MethodSource("provideClassLoaders")
	void testSubFolderWithoutWelcomeFileOrTrailingSlashIsNotListed(ClassLoader classLoader) throws Exception {
		TestConfiguration config = createWebContentConfiguration(classLoader);

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/emptysub");

		// Assert
		assertEquals(404, response.getStatus());
		assertFalse(response.getContentAsString().contains("readme.txt"));
	}

	@ParameterizedTest
	@MethodSource("provideClassLoaders")
	void testFileInSubFolder(ClassLoader classLoader) throws Exception {
		TestConfiguration config = createWebContentConfiguration(classLoader);

		// Act
		MockHttpServletResponse response = doGet(config, "/" + TestConfiguration.TEST_CONFIGURATION_NAME + "/emptysub/readme.txt");

		// Assert
		assertEquals(200, response.getStatus());
		assertEquals("text/plain", response.getContentType());
	}
}
