package org.frankframework.management.web;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, AdapterStatistics.class})
public class AdapterStatisticsTest extends FrankApiTestBase {

	@Test
	public void getAdapterStatisticsReturnsCorrectResponse() throws Exception {
		testBasicRequest("/configurations/{configuration}/adapters/{adapter}/statistics", "ADAPTER", "STATUS", "TestConfiguration", "TestAdapter");
	}
}
