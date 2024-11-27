package org.frankframework.console.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, EnvironmentVariables.class})
public class EnvironmentVariablesTest extends FrankApiTestBase {

	@Test
	public void getEnvironmentVariables() throws Exception {
		testActionAndTopicHeaders("/environmentvariables", "ENVIRONMENT", null);
	}
}
