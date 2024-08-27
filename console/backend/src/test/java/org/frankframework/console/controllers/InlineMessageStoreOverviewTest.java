package org.frankframework.console.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, InlineMessageStoreOverview.class})
public class InlineMessageStoreOverviewTest extends FrankApiTestBase {

	@Test
	public void getMessageBrowsers() throws Exception {
		testActionAndTopicHeaders("/inlinestores/overview", "INLINESTORAGE_SUMMARY", null);
	}
}
