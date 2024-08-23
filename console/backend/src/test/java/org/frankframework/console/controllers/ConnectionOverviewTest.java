package org.frankframework.console.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, ConnectionOverview.class})
public class ConnectionOverviewTest extends FrankApiTestBase {

	@Test
	public void getConnections() throws Exception {
		testActionAndTopicHeaders("/connections", "CONNECTION_OVERVIEW", null);
	}
}
