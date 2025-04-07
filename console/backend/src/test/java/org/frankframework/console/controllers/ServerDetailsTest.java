package org.frankframework.console.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, ServerDetails.class})
public class ServerDetailsTest extends FrankApiTestBase {

	@Test
	public void testServerInformation() throws Exception {
		this.testActionAndTopicHeaders("/server/info", "APPLICATION", "GET");
	}

	@Test
	public void testAllConfigurations() throws Exception {
		this.testActionAndTopicHeaders("/server/configurations", "CONFIGURATION", "FIND");
	}

	@Test
	public void testServerWarnings() throws Exception {
		this.testActionAndTopicHeaders("/server/warnings", "APPLICATION", "WARNINGS");
	}

	@Test
	public void testFrankHealth() throws Exception {
		this.testActionAndTopicHeaders("/server/health", "HEALTH", null);
	}
}
