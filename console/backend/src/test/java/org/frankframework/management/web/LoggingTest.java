package org.frankframework.management.web;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, Logging.class})
class LoggingTest extends FrankApiTestBase {

	@Test
	public void getLoggingBasic() throws Exception {
		testBasicRequest("/logging", "LOGGING", "GET");
	}
}
