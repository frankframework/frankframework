package org.frankframework.console.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, Logging.class})
class LoggingTest extends FrankApiTestBase {

	@Test
	public void getLoggingBasic() throws Exception {
		testActionAndTopicHeaders("/logging", "LOGGING", "GET");
	}
}
