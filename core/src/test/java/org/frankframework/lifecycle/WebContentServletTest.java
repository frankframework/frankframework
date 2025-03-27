package org.frankframework.lifecycle;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import org.frankframework.configuration.classloaders.AbstractClassLoader;
import org.frankframework.testutil.TestConfiguration;

public class WebContentServletTest {

	@Test
	void testListDirectory() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		WebContentServlet webContentServlet = new WebContentServlet();

		TestConfiguration config = new TestConfiguration(TestConfiguration.TEST_CONFIGURATION_FILE);
		config.setClassLoader(new AbstractClassLoader() {
			@Override
			public URL getLocalResource(String name) {
				// By using getClassLoader() instead of getClass(), we can access the test classpath
				return this.getClass().getClassLoader().getResource(name);
			}
		});
		webContentServlet.getWebContentForConfiguration(response, config);

		String contentAsString = response.getContentAsString();

		Assertions.assertTrue(contentAsString.contains("TestConfiguration"));
	}

	@Test
	void testListDirectoryNotPresent() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		WebContentServlet webContentServlet = new WebContentServlet();

		TestConfiguration config = new TestConfiguration(TestConfiguration.TEST_CONFIGURATION_FILE);
		config.setClassLoader(new AbstractClassLoader() {
			@Override
			public URL getLocalResource(String name) {
				// By using getResource() on getClass(), the test classpath is restricted to 'lifecycle' package
				return this.getClass().getResource(name);
			}
		});

		webContentServlet.getWebContentForConfiguration(response, config);

		String contentAsString = response.getContentAsString();

		Assertions.assertFalse(contentAsString.contains("TestConfiguration"));
	}
}
