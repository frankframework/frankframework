package org.frankframework.management.web.spring;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, ServerStatistics.class})
public class ServerStatisticsTest extends FrankApiTestBase {

	@Test
	public void testServerInformation() throws Exception {
		this.testBasicRequest("/server/info", "APPLICATION", "GET");
	}

	@Test
	public void testAllConfigurations() throws Exception {
		this.testBasicRequest("/server/configurations", "CONFIGURATION", "FIND");
	}

	@Test
	public void testServerWarnings() throws Exception {
		this.testBasicRequest("/server/warnings", "APPLICATION", "WARNINGS");
	}

	@Test
	public void testFrankHealth() throws Exception {
		this.testBasicRequest("/server/health", "HEALTH", null);
	}

}
