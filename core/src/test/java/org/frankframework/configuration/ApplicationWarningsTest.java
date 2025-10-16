package org.frankframework.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.AppConstants;

@Log4j2
public class ApplicationWarningsTest {

	private TestConfiguration configuration = null;

	@BeforeEach
	public void setUp() {
		ApplicationWarnings.removeInstance(); //Remove old instance if present
		configuration = new TestConfiguration();
	}

	@AfterEach
	public void tearDown() {
		if(configuration != null) {
			configuration.close();
		}
	}

	@Test
	void testApplicationContextFromSpring() {
		ApplicationWarnings applWarnings = configuration.getBean("applicationWarnings", ApplicationWarnings.class);
		assertEquals(configuration, applWarnings.getApplicationContext());
	}

	@Test
	void testApplicationContextStaticInitialized() {
		ApplicationWarnings.add(log, "test message");

		ApplicationWarnings applWarnings = configuration.getBean("applicationWarnings", ApplicationWarnings.class);

		assertEquals(configuration, applWarnings.getApplicationContext());
		assertEquals("test message", applWarnings.getWarnings().get(0));
	}

	@Test
	void testApplicationContextFromRefreshedSpringContext() {
		ApplicationWarnings.add(log, "test message 1");

		configuration.getBean("applicationWarnings", ApplicationWarnings.class);
		configuration.refresh();

		ApplicationWarnings.add(log, "test message 2");

		ApplicationWarnings applWarnings = configuration.getBean("applicationWarnings", ApplicationWarnings.class);

		assertEquals(configuration, applWarnings.getApplicationContext());
		assertEquals(1, applWarnings.getWarnings().size(), "After a Context refresh it should not copy warnings over to the new instance");
		assertEquals("test message 2", applWarnings.getWarnings().get(0));
	}

	@Test
	void testApplicationWarningFromAppConstants() {
		// Arrange
		AppConstants appConstants = AppConstants.getInstance();
		appConstants.setProperty("test", "${= ApplicationWarnings.add(log, \"missing property\");}");

		// Act
		try (TestAppender testAppender = TestAppender.newBuilder().build()) {
			appConstants.getProperty("test");

			assertThat(testAppender.getLogLines(), hasItem(containsString("missing property")));
		}
		// Assert
		List<String> applWarnings = ApplicationWarnings.getWarningsList();

		assertEquals(1, applWarnings.size());
		assertEquals("missing property", applWarnings.get(0));
	}

	@Test
	void testConditionalApplicationWarningFromAppConstants() {
		// Arrange
		AppConstants appConstants = AppConstants.getInstance();
		appConstants.setProperty("test", "${= if (StringUtils.isBlank(no.such.prop)) { ApplicationWarnings.add(log, \"missing property\"); } }");

		// Act
		try (TestAppender testAppender = TestAppender.newBuilder().build()) {
			String result = appConstants.getProperty("test");

			assertThat(testAppender.getLogLines(), hasItem(containsString("missing property")));
			assertEquals("", result);
		}
		// Assert
		List<String> applWarnings = ApplicationWarnings.getWarningsList();

		assertEquals(1, applWarnings.size());
		assertEquals("missing property", applWarnings.get(0));
	}

	@Test
	void testConditionalWarningOrValueFromAppConstants() {
		// Arrange
		AppConstants appConstants = AppConstants.getInstance();
		appConstants.setProperty("remote.url", "${= if (StringUtils.isBlank(remote.host) || StringUtils.isBlank(remote.port)) { ApplicationWarnings.add(log, \"missing property remote.host or remote.port\"); } else { return \"http://%s:%s/api\".formatted(remote.host, remote.port); }}");
		appConstants.setProperty("remote.host", "example.com");
		appConstants.setProperty("remote.port", "8080");

		// Act
		String result = appConstants.getProperty("remote.url");

		// Assert
		assertEquals("http://example.com:8080/api", result);

		List<String> applWarnings = ApplicationWarnings.getWarningsList();
		assertEquals(0, applWarnings.size());
	}
}
