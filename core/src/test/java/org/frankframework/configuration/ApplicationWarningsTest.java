package org.frankframework.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

import org.frankframework.testutil.TestConfiguration;

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
}
